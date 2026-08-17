# Ticket Classifier

A small Spring Boot REST API that classifies support tickets into a category
and priority, and drafts a suggested reply. Classification is done by Azure
OpenAI, with a built-in keyword-based fallback if the AI call fails for any
reason.

## Tech stack

- Java 21
- Spring Boot 4.1 (Spring Web)
- Maven
- Docker (multi-stage build)
- Deployed on Azure Container Apps, image hosted in Azure Container Registry

## How it works

- `POST /classify` accepts a ticket's text and returns a category, priority,
  and suggested reply.
- `GET /health` returns a simple liveness check (used by Kubernetes /
  Container Apps probes).
- Classification logic sits behind a `ClassificationService` interface, so
  the controller never has to change when the logic behind it does:
  - `AiClassificationService` (the active bean) calls Azure OpenAI first.
  - If that call fails for any reason (missing config, network error, bad
    response), it automatically falls back to `RuleBasedClassificationService`,
    a simple keyword matcher.
  - Which path handled a given request is logged.

## Project structure

```
src/main/java/com/bipin/ticketclassifier/
├── TicketClassifierApplication.java     # Spring Boot entry point
├── controller/
│   ├── ClassifyController.java          # POST /classify
│   └── HealthController.java            # GET /health
├── dto/
│   ├── ClassifyRequest.java             # { "text": "..." }
│   ├── ClassifyResponse.java            # { "category", "priority", "suggestedReply" }
│   └── HealthResponse.java              # { "status": "UP" }
└── service/
    ├── ClassificationService.java           # interface
    ├── AiClassificationService.java         # active bean: AI first, falls back to rules
    ├── AzureOpenAiService.java              # calls Azure OpenAI chat completions
    └── RuleBasedClassificationService.java  # keyword-based fallback
```

## Configuration

Three environment variables configure the Azure OpenAI call. The app
**still starts and works without them** - it just always uses the keyword
fallback, which is logged on every request.

| Variable | Example |
|---|---|
| `AZURE_OPENAI_ENDPOINT` | `https://<resource-name>.openai.azure.com/openai/v1` |
| `AZURE_OPENAI_KEY` | from Azure Portal → your resource → Keys and Endpoint |
| `AZURE_OPENAI_DEPLOYMENT` | the deployment name shown in Azure AI Foundry, e.g. `gpt-4.1-mini` |

Never commit real values for these - `application.properties` only ever
reads them from the environment (`${AZURE_OPENAI_ENDPOINT:}` etc.), it
never hardcodes a secret.

## Run locally

```bash
export AZURE_OPENAI_ENDPOINT="https://<resource-name>.openai.azure.com/openai/v1"
export AZURE_OPENAI_KEY="<your-key>"
export AZURE_OPENAI_DEPLOYMENT="<your-deployment-name>"

./mvnw spring-boot:run
```

Test it:

```bash
curl -X POST http://localhost:8080/classify \
  -H "Content-Type: application/json" \
  -d '{"text":"my payment failed twice, please refund me"}'
```

Run tests:

```bash
./mvnw test
```

## Run with Docker

Build:

```bash
docker build -t ticket-classifier .
```

Run:

```bash
docker run -d --name ticket-classifier \
  -p 8080:8080 \
  -e AZURE_OPENAI_ENDPOINT="$AZURE_OPENAI_ENDPOINT" \
  -e AZURE_OPENAI_KEY="$AZURE_OPENAI_KEY" \
  -e AZURE_OPENAI_DEPLOYMENT="$AZURE_OPENAI_DEPLOYMENT" \
  ticket-classifier
```

> **Apple Silicon note:** a plain `docker build` on an M-series Mac produces
> an `arm64` image, which won't run on most Azure compute (typically
> `amd64`). Build for the target platform explicitly before deploying:
> `docker buildx build --platform linux/amd64 -t ticket-classifier .`

## Deploy to Azure

Deployed using:
- **Azure Container Registry (ACR)** to host the image
- **Azure Container Apps** to run it - public HTTPS ingress on port 8080,
  scale-to-zero when idle, and the OpenAI key stored as an encrypted
  Container Apps secret rather than a plain env var
- The container app pulls the image using its own system-assigned managed
  identity, so the registry never needs admin credentials enabled

To ship a new image:

```bash
# Build for Azure's architecture and push
docker buildx build --platform linux/amd64 \
  -t <acr-name>.azurecr.io/ticket-classifier:v2 --push .

# Point the running app at the new image
az containerapp update \
  --name ticket-classifier \
  --resource-group ticket-classifier-rg \
  --image <acr-name>.azurecr.io/ticket-classifier:v2
```

## API reference

### `POST /classify`

Request:
```json
{ "text": "my payment failed twice" }
```

Response:
```json
{
  "category": "Billing",
  "priority": "High",
  "suggestedReply": "..."
}
```

`category` is one of `Billing` / `Technical` / `General`.
`priority` is one of `High` / `Medium` / `Low`.

### `GET /health`

```json
{ "status": "UP" }
```
