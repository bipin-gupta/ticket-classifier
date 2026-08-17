package com.bipin.ticketclassifier.service;

import com.bipin.ticketclassifier.dto.ClassifyResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

// This is the bean ClassifyController actually gets injected (it's the
// only @Service that implements ClassificationService), so no controller
// change was needed to wire the AI path in.
//
// It tries Azure OpenAI first. If that call throws for any reason (bad
// config, network error, bad JSON back from the model, etc.) it falls back
// to the plain keyword rules so /classify still returns a normal answer
// instead of an error.
@Service
public class AiClassificationService implements ClassificationService {

    private static final Logger log = LoggerFactory.getLogger(AiClassificationService.class);

    private final AzureOpenAiService azureOpenAiService;
    private final RuleBasedClassificationService fallbackService = new RuleBasedClassificationService();

    public AiClassificationService(AzureOpenAiService azureOpenAiService) {
        this.azureOpenAiService = azureOpenAiService;
    }

    @Override
    public ClassifyResponse classify(String text) {
        try {
            ClassifyResponse response = azureOpenAiService.classify(text);
            log.info("Classified ticket using Azure OpenAI");
            return response;
        } catch (Exception e) {
            log.warn("Azure OpenAI call failed ({}), falling back to keyword rules", e.getMessage());
            return fallbackService.classify(text);
        }
    }
}
