# REST Client Implementation

## Overview
This document describes the Quarkus REST Client implementations for external APIs (Spoonacular and LLM) with comprehensive timeout and error handling.

## Architecture

### REST Clients
1. **[`SpoonacularRestClient`](../src/main/java/dev/stat/chat/client/SpoonacularRestClient.java)**: Fetches daily nutrition data
2. **[`LlmRestClient`](../src/main/java/dev/stat/chat/client/LlmRestClient.java)**: Sends prompts to LLM API (OpenAI/Gemini compatible)

### Service Implementations
1. **[`DefaultNutritionApiClient`](../src/main/java/dev/stat/chat/service/DefaultNutritionApiClient.java)**: Implements [`NutritionApiClient`](../src/main/java/dev/stat/chat/service/NutritionApiClient.java)
2. **[`DefaultLlmClient`](../src/main/java/dev/stat/chat/service/DefaultLlmClient.java)**: Implements [`LlmClient`](../src/main/java/dev/stat/chat/service/LlmClient.java)

## Spoonacular API Integration

### Endpoint
```
GET /mealplanner/{username}/day/{date}?apiKey={key}
```

### Request Example
```http
GET /mealplanner/username/day/2026-04-14?apiKey=abc123
Host: api.spoonacular.com
```

### Response Example
```json
{
  "nutritionSummary": {
    "nutrients": [
      {"name": "Calories", "amount": 2100},
      {"name": "Protein", "amount": 150},
      {"name": "Carbohydrates", "amount": 220},
      {"name": "Fat", "amount": 70}
    ]
  }
}
```

### Mapping to Domain Model
The response is mapped to [`NutritionData`](../src/main/java/dev/stat/chat/domain/NutritionData.java):
```java
record NutritionData(
    int calories,      // from "Calories"
    int proteinGrams,  // from "Protein"
    int carbsGrams,    // from "Carbohydrates"
    int fatGrams       // from "Fat"
)
```

## LLM API Integration

### Endpoint
```
POST /v1/chat/completions
```

### Request Example
```http
POST /v1/chat/completions
Host: api.openai.com
Authorization: Bearer sk-...
Content-Type: application/json

{
  "model": "gpt-4",
  "messages": [
    {
      "role": "user",
      "content": "User context: Body Battery 45, Sleep Score 85..."
    }
  ],
  "temperature": 0.7,
  "maxTokens": 500
}
```

### Response Example
```json
{
  "id": "chatcmpl-123",
  "object": "chat.completion",
  "created": 1677652288,
  "model": "gpt-4",
  "choices": [{
    "index": 0,
    "message": {
      "role": "assistant",
      "content": "Da du in 2 Stunden Intervalle fährst..."
    },
    "finish_reason": "stop"
  }]
}
```

## Error Handling

### All-or-Nothing Principle
According to [`specs/api_chat.md`](../../specs/api_chat.md), if ANY external service fails, the entire request must fail with HTTP 503.

### Error Scenarios

#### 1. Timeout (> 5 seconds)
```java
// Configuration
quarkus.rest-client.spoonacular.read-timeout=5000
quarkus.rest-client.llm.read-timeout=5000
```

**Behavior**: Throws `ExternalServiceException` with message:
- `"Spoonacular API did not respond within timeout"`
- `"LLM API did not respond within timeout"`

#### 2. Rate Limiting (HTTP 429)
**Behavior**: Throws `ExternalServiceException` with message:
- `"Spoonacular API rate limit exceeded (HTTP 429)"`
- `"LLM API rate limit exceeded (HTTP 429)"`

#### 3. Server Error (HTTP 5xx)
**Behavior**: Throws `ExternalServiceException` with message:
- `"Spoonacular API server error (HTTP 500)"`
- `"LLM API server error (HTTP 503)"`

#### 4. Authentication Error (HTTP 401)
**Behavior**: Throws `ExternalServiceException` with message:
- `"LLM API authentication failed (HTTP 401)"`

#### 5. Connection Failure
**Behavior**: Throws `ExternalServiceException` with message:
- `"Spoonacular API connection failed: ..."`
- `"LLM API connection failed: ..."`

### Exception Propagation
All `ExternalServiceException` instances are caught by [`ExternalServiceExceptionMapper`](../src/main/java/dev/stat/chat/ExternalServiceExceptionMapper.java) and converted to HTTP 503 responses.

## Configuration

### application.properties
```properties
# Spoonacular API
stat.spoonacular.base-url=https://api.spoonacular.com
stat.spoonacular.api-key=changeme
stat.spoonacular.username=username

# LLM API
stat.llm.base-url=https://api.openai.com
stat.llm.api-key=changeme
stat.llm.model=gpt-4
stat.llm.temperature=0.7
stat.llm.max-tokens=500

# Timeouts
stat.external.timeout-ms=5000

# REST Client Configuration
quarkus.rest-client.spoonacular.url=${stat.spoonacular.base-url}
quarkus.rest-client.spoonacular.read-timeout=${stat.external.timeout-ms}
quarkus.rest-client.spoonacular.connect-timeout=2000

quarkus.rest-client.llm.url=${stat.llm.base-url}
quarkus.rest-client.llm.read-timeout=${stat.external.timeout-ms}
quarkus.rest-client.llm.connect-timeout=2000
```

### Environment Variables (Production)
```bash
STAT_SPOONACULAR_API_KEY=<secret-key>
STAT_LLM_API_KEY=<secret-key>
STAT_LLM_BASE_URL=https://api.openai.com  # or Gemini
STAT_LLM_MODEL=gpt-4
```

## Testing

### Unit Tests with WireMock
Both clients are tested using WireMock to simulate external APIs:

- **[`DefaultNutritionApiClientTest`](../src/test/java/dev/stat/chat/service/DefaultNutritionApiClientTest.java)**
  - ✅ Successful data retrieval
  - ✅ Timeout handling (6s delay)
  - ✅ Rate limiting (HTTP 429)
  - ✅ Server errors (HTTP 500)
  - ✅ Invalid JSON handling

- **[`DefaultLlmClientTest`](../src/test/java/dev/stat/chat/service/DefaultLlmClientTest.java)**
  - ✅ Successful chat completion
  - ✅ Timeout handling (6s delay)
  - ✅ Rate limiting (HTTP 429)
  - ✅ Server errors (HTTP 500)
  - ✅ Invalid JSON handling
  - ✅ Prompt inclusion verification

### Running Tests
```bash
# Run all tests
./mvnw test

# Run specific test
./mvnw test -Dtest=DefaultNutritionApiClientTest
./mvnw test -Dtest=DefaultLlmClientTest
```

## Quarkus REST Client Features

### @RegisterRestClient
Marks interfaces as REST clients that Quarkus will implement at build time.

```java
@RegisterRestClient(configKey = "spoonacular")
@Path("/mealplanner")
public interface SpoonacularRestClient {
    // Methods...
}
```

### @RestClient Injection
Injects the generated REST client implementation:

```java
@RestClient
SpoonacularRestClient spoonacularClient;
```

### Automatic JSON Serialization
Uses Jackson for automatic JSON mapping to/from Java records.

### Timeout Configuration
- **Connect Timeout**: 2 seconds (time to establish connection)
- **Read Timeout**: 5 seconds (time to receive response)

## Performance Considerations

### Timeouts
- **5 seconds** is the maximum wait time per external service
- Total maximum latency for `/api/chat` endpoint:
  - InfluxDB: ~500ms (local)
  - Spoonacular: up to 5s
  - LLM: up to 5s
  - **Total**: ~10.5 seconds worst case

### Optimization Opportunities
1. **Parallel Execution**: Fetch InfluxDB, Spoonacular, and prepare prompt concurrently
2. **Caching**: Cache nutrition data for the current day
3. **Circuit Breaker**: Implement Quarkus Fault Tolerance for failing services

## Related Files

- Specification: [`specs/api_chat.md`](../../specs/api_chat.md)
- REST Clients:
  - [`SpoonacularRestClient.java`](../src/main/java/dev/stat/chat/client/SpoonacularRestClient.java)
  - [`LlmRestClient.java`](../src/main/java/dev/stat/chat/client/LlmRestClient.java)
- Service Implementations:
  - [`DefaultNutritionApiClient.java`](../src/main/java/dev/stat/chat/service/DefaultNutritionApiClient.java)
  - [`DefaultLlmClient.java`](../src/main/java/dev/stat/chat/service/DefaultLlmClient.java)
- Tests:
  - [`DefaultNutritionApiClientTest.java`](../src/test/java/dev/stat/chat/service/DefaultNutritionApiClientTest.java)
  - [`DefaultLlmClientTest.java`](../src/test/java/dev/stat/chat/service/DefaultLlmClientTest.java)
- Exception Handling: [`ExternalServiceExceptionMapper.java`](../src/main/java/dev/stat/chat/ExternalServiceExceptionMapper.java)
