# Google AI Studio (Gemini) Integration

## Overview
The Stat backend has been configured to use Google AI Studio's Gemini API instead of OpenAI. This document explains the changes and how to configure your API key.

## Changes Made

### 1. Configuration (`application.properties`)
Updated the LLM configuration to use Gemini endpoints:
```properties
# LLM Configuration (Google AI Studio / Gemini)
stat.llm.base-url=https://generativelanguage.googleapis.com
stat.llm.api-key=changeme
stat.llm.model=gemini-1.5-flash
stat.llm.temperature=0.7
stat.llm.max-tokens=500
```

### 2. REST Client (`LlmRestClient.java`)
Adapted the REST client to match Gemini's API structure:
- **Endpoint**: `/v1beta/models/{model}:generateContent`
- **Authentication**: API key passed as query parameter (`?key=YOUR_API_KEY`)
- **Request Format**: Uses Gemini's `contents` and `parts` structure
- **Response Format**: Uses Gemini's `candidates` structure

### 3. Service Implementation (`DefaultLlmClient.java`)
Updated to work with Gemini's request/response format:
- Builds requests with `Content` and `Part` objects
- Extracts responses from `candidates[0].content.parts[0].text`
- Updated error messages to reference "Gemini API"

### 4. Tests (`DefaultLlmClientTest.java`)
All unit tests updated to mock Gemini API responses:
- ✅ All 6 tests passing
- Tests cover: successful responses, timeouts, rate limits, server errors, invalid JSON

## How to Configure Your API Key

### Step 1: Get Your API Key
1. Go to [Google AI Studio](https://aistudio.google.com/)
2. Sign in with your Google account
3. Click "Get API Key" in the left sidebar
4. Create a new API key or use an existing one

### Step 2: Update Configuration
Open `backend/src/main/resources/application.properties` and replace `changeme` with your actual API key:

```properties
stat.llm.api-key=YOUR_ACTUAL_API_KEY_HERE
```

### Step 3: Choose Your Model (Optional)
You can change the model if needed. Available models include:
- `gemini-1.5-flash` (default, fast and efficient)
- `gemini-1.5-pro` (more capable, slower)
- `gemini-1.0-pro` (older version)

```properties
stat.llm.model=gemini-1.5-flash
```

## API Differences: OpenAI vs Gemini

| Aspect | OpenAI | Gemini |
|--------|--------|--------|
| **Base URL** | `https://api.openai.com` | `https://generativelanguage.googleapis.com` |
| **Endpoint** | `/v1/chat/completions` | `/v1beta/models/{model}:generateContent` |
| **Auth Method** | Header: `Authorization: Bearer {key}` | Query param: `?key={key}` |
| **Request Structure** | `messages: [{role, content}]` | `contents: [{parts: [{text}]}]` |
| **Response Structure** | `choices[0].message.content` | `candidates[0].content.parts[0].text` |

## Testing the Integration

### 1. Test the Gemini API Endpoint Directly

Before running the application, verify your API key works with the test script:

**Windows (PowerShell):**
```powershell
.\test-gemini-api.ps1 YOUR_API_KEY
```

**Linux/Mac:**
```bash
chmod +x test-gemini-api.sh
./test-gemini-api.sh YOUR_API_KEY
```

This will test the actual Gemini API endpoint and confirm:
- ✅ The endpoint URL is correct
- ✅ Your API key is valid
- ✅ The request/response format works

### 2. Run Unit Tests

Run the unit tests to verify the integration code:
```bash
cd backend
./mvnw test -Dtest=DefaultLlmClientTest
```

### 3. Test the Full Application

Start the application:
```bash
./mvnw quarkus:dev
```

Then send a chat request to verify the Gemini integration:
```bash
curl -X POST http://localhost:8081/api/chat \
  -H "Content-Type: application/json" \
  -d '{"currentMessage": "What should I eat today?"}'
```

## Rate Limits & Pricing

Google AI Studio offers:
- **Free tier**: 15 requests per minute, 1,500 requests per day
- **Paid tier**: Higher limits available

Monitor your usage at: https://aistudio.google.com/app/apikey

## Troubleshooting

### Error: "Gemini API authentication failed (HTTP 403)"
- Check that your API key is correct in `application.properties`
- Verify the API key is enabled in Google AI Studio

### Error: "Gemini API rate limit exceeded (HTTP 429)"
- You've exceeded the free tier limits
- Wait a minute or upgrade to a paid plan

### Error: "Gemini API did not respond within timeout"
- The default timeout is 5000ms (5 seconds)
- Increase it in `application.properties`: `stat.external.timeout-ms=10000`

## Security Note

⚠️ **Never commit your API key to version control!**

Consider using environment variables:
```properties
stat.llm.api-key=${GEMINI_API_KEY:changeme}
```

Then set the environment variable:
```bash
export GEMINI_API_KEY=your_actual_key
```
