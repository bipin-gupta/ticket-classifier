package com.bipin.ticketclassifier.service;

import com.bipin.ticketclassifier.dto.ClassifyResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

// Calls Azure OpenAI's chat completions REST API and turns the answer into
// our ClassifyResponse DTO. Any failure (bad config, network error, bad
// JSON from the model, ...) is thrown as an exception - AiClassificationService
// catches it and falls back to the keyword rules.
@Service
public class AzureOpenAiService {

    private static final String SYSTEM_PROMPT = """
            You are a support ticket classifier. Read the customer's ticket
            text and respond with ONLY a single JSON object, no other text,
            in exactly this shape:
            {"category": "...", "priority": "...", "suggestedReply": "..."}
            "category" must be exactly one of: Billing, Technical, General.
            "priority" must be exactly one of: High, Medium, Low.
            "suggestedReply" is a short, polite reply to send the customer.
            """;

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper;

    private final String endpoint;
    private final String apiKey;
    private final String deployment;

    public AzureOpenAiService(
            ObjectMapper objectMapper,
            @Value("${azure.openai.endpoint}") String endpoint,
            @Value("${azure.openai.key}") String apiKey,
            @Value("${azure.openai.deployment}") String deployment) {
        this.objectMapper = objectMapper;
        this.endpoint = endpoint;
        this.apiKey = apiKey;
        this.deployment = deployment;
    }

    public ClassifyResponse classify(String text) {
        if (endpoint.isBlank() || apiKey.isBlank() || deployment.isBlank()) {
            throw new IllegalStateException(
                    "Azure OpenAI is not configured - set AZURE_OPENAI_ENDPOINT, "
                            + "AZURE_OPENAI_KEY and AZURE_OPENAI_DEPLOYMENT");
        }

        // AZURE_OPENAI_ENDPOINT must be the full v1 base URL shown in Azure
        // AI Foundry, e.g. https://<resource>.openai.azure.com/openai/v1
        // (this newer API takes the deployment name in the body as "model"
        // instead of in the URL path).
        String url = endpoint + "/chat/completions";

        Map<String, Object> requestBody = Map.of(
                "model", deployment,
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", text)),
                "temperature", 0.2,
                "max_tokens", 300,
                // Forces the model to return valid JSON. Only works on
                // deployments that support JSON mode (e.g. gpt-4o,
                // gpt-4o-mini, gpt-35-turbo 1106+). Remove this line if
                // your deployment doesn't support it.
                "response_format", Map.of("type", "json_object"));

        String requestJson = objectMapper.writeValueAsString(requestBody);

        String responseJson = restClient.post()
                .uri(url)
                .header("api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestJson)
                .retrieve()
                .body(String.class);

        AzureChatResponse azureResponse = objectMapper.readValue(responseJson, AzureChatResponse.class);
        String content = azureResponse.choices().get(0).message().content();

        return objectMapper.readValue(cleanJson(content), ClassifyResponse.class);
    }

    // Some models still wrap JSON in ```json ... ``` fences even when told
    // not to - strip those if present before parsing.
    private String cleanJson(String content) {
        String cleaned = content.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```[a-zA-Z]*", "").trim();
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
            }
        }
        return cleaned;
    }

    // Minimal shape of Azure OpenAI's chat completions response - we only
    // read the first choice's message content. ignoreUnknown=true means we
    // don't need to list every field Azure actually returns (id, usage, ...).
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AzureChatResponse(List<Choice> choices) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Choice(Message message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Message(String content) {
    }
}
