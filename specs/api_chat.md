# Specification: Chat API

**Endpoint:** `POST /api/chat`

**Purpose:** Core intelligence endpoint. Combines user message with recent chat history, external health data, and nutritional data to generate an LLM prompt.

## Architectural Rules (Enforced)

### 1. Strict Dependency (All-or-Nothing)

- The endpoint MUST successfully retrieve data from InfluxDB (Garmin data) AND the Nutrition API (Spoonacular).
- If ANY external service fails or times out, the endpoint MUST abort and return an HTTP 503 Service Unavailable with a clear error message (e.g., "Nutrition API currently unavailable").
- No partial prompts are allowed.

### 2. Data Aggregation Window

- **Sleep:** Extract the single Sleep Score from the *previous* night.
- **Training Load:** Sum up the training duration/intensity from the *last 48 hours*.
- **Nutrition:** Total macros (Protein, Carbs, Fats) and Calories consumed *today*.
- **Live Metric:** Most recent 'Body Battery' value.

### 3. State Management (Client-Side State)

- The Quarkus backend is 100% stateless.
- The frontend is responsible for providing the conversation history in the request payload.

## API Contract

### Request (Frontend → Quarkus)

```json
{
  "currentMessage": "Was soll ich jetzt essen?",
  "chatHistory": [
    {
      "role": "user",
      "content": "Hallo Coach, wie ist meine Form?"
    },
    {
      "role": "assistant",
      "content": "Deine Body Battery ist auf 45, aber dein Schlaf war mit 85 Punkten exzellent."
    }
  ]
}
```

### Response (Quarkus → Frontend)

#### Success (200 OK)

```json
{
  "reply": "Da du in 2 Stunden Intervalle fährst und heute erst 120g Carbs hattest, empfehle ich dir jetzt sofort 2 Bananen oder eine Schüssel Haferflocken.",
  "metricsUsed": {
    "bodyBattery": 45,
    "sleepScore": 85,
    "trainingLoad48h": "120 mins"
  }
}
```

#### Error (503 Service Unavailable)

```json
{
  "error": "EXTERNAL_API_FAILURE",
  "message": "Spoonacular API did not respond within 5000ms."
}
```
