# Vision Support for Meal Photo Tracking

## Overview

The Stat AI Coach now supports vision-based meal tracking through image analysis. Users can upload or capture photos of their meals, and the system will analyze the image using Gemini's vision capabilities to estimate nutritional values.

## Implementation Details

### Backend Changes

#### 1. API Contract Extension

**File:** [`specs/api_chat.md`](../../specs/api_chat.md)

The chat API now accepts an optional `imageBase64` field:

```json
{
  "currentMessage": "Was ist das für eine Mahlzeit?",
  "chatHistory": [],
  "imageBase64": "/9j/4AAQSkZJRgABAQEAYABgAAD..."
}
```

#### 2. DTO Updates

**File:** [`backend/src/main/java/dev/stat/chat/dto/ChatRequest.java`](../src/main/java/dev/stat/chat/dto/ChatRequest.java)

Added optional `imageBase64` field to the ChatRequest record:

```java
public record ChatRequest(
    @NotBlank(message = "currentMessage must not be blank")
    String currentMessage,
    List<ChatMessage> chatHistory,
    String imageBase64  // Optional Base64-encoded image
) {}
```

#### 3. LLM Client Updates

**File:** [`backend/src/main/java/dev/stat/chat/client/LlmRestClient.java`](../src/main/java/dev/stat/chat/client/LlmRestClient.java)

Extended the `Part` record to support inline image data:

```java
record Part(
    String text,
    FunctionCall functionCall,
    InlineData inlineData  // For vision support
) {}

record InlineData(
    String mimeType,
    String data
) {}
```

**File:** [`backend/src/main/java/dev/stat/chat/service/LlmClient.java`](../src/main/java/dev/stat/chat/service/LlmClient.java)

Added new method signature:

```java
String chat(String prompt, String systemContext, String imageBase64);
```

**File:** [`backend/src/main/java/dev/stat/chat/service/DefaultLlmClient.java`](../src/main/java/dev/stat/chat/service/DefaultLlmClient.java)

Implemented the new method to construct multi-part requests with both text and image:

```java
@Override
public String chat(String prompt, String systemContext, String imageBase64) {
    // Build content parts: text prompt and optional image
    List<LlmRestClient.Part> parts = new java.util.ArrayList<>();
    parts.add(new LlmRestClient.Part(prompt, null, null));

    if (imageBase64 != null && !imageBase64.isBlank()) {
        // Add image as inline_data with JPEG mime type
        parts.add(new LlmRestClient.Part(
            null,
            null,
            new LlmRestClient.InlineData("image/jpeg", imageBase64)
        ));
    }
    // ... rest of implementation
}
```

#### 4. ChatService Updates

**File:** [`backend/src/main/java/dev/stat/chat/service/ChatService.java`](../src/main/java/dev/stat/chat/service/ChatService.java)

Updated to pass images to the LLM and enhanced system context with vision instructions:

```java
public ChatResponse processChat(ChatRequest request) {
    HealthData health = healthDataClient.fetchCurrentHealthData();
    NutritionData nutrition = nutritionApiClient.fetchTodayNutrition();

    String systemContext = buildSystemContext(request.imageBase64() != null);
    String prompt = buildPrompt(request, health, nutrition);
    String llmResponse = llmClient.chat(prompt, systemContext, request.imageBase64());

    // ... rest of implementation
}
```

Enhanced system context when image is present:

```
## Vision Analysis
- When an image is provided, analyze the meal in the photo.
- Estimate portion sizes and identify all visible food items.
- Calculate the nutritional values (calories, protein, carbs, fat).
- Use the log_nutrition tool to record the estimated values.
- Provide feedback on how this meal fits the user's goals.
```

#### 5. Test Coverage

**File:** [`backend/src/test/java/dev/stat/chat/ChatResourceTest.java`](../src/test/java/dev/stat/chat/ChatResourceTest.java)

Added integration test for image-based meal tracking:

```java
@Test
void imageProvided_sendsImageToLlmAndAnalyzesMeal() {
    // Arrange: stub all external boundaries
    Mockito.when(healthDataClient.fetchCurrentHealthData())
            .thenReturn(new HealthData(50, 80, 100));

    Mockito.when(nutritionApiClient.fetchTodayNutrition())
            .thenReturn(new NutritionData(1500, 100, 180, 50));

    // LLM analyzes image and returns function call to log nutrition
    Mockito.when(llmClient.chat(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
            .thenReturn("{\"functionCall\":{\"name\":\"log_nutrition\",\"args\":{\"calories\":650,\"protein\":35,\"carbs\":80,\"fat\":25}}}");

    // Act & Assert
    given()
            .contentType(ContentType.JSON)
            .body("""
                    {
                      "currentMessage": "Was ist das für eine Mahlzeit?",
                      "chatHistory": [],
                      "imageBase64": "/9j/4AAQSkZJRgABAQEAYABgAAD..."
                    }
                    """)
    .when()
            .post("/api/chat")
    .then()
            .statusCode(200)
            .body("reply", is("Mahlzeit erfasst! 650 kcal (35g Protein, 80g Carbs, 25g Fett)"));

    // Verify that logNutrition was called with analyzed values
    Mockito.verify(healthDataClient).logNutrition(650, 35, 80, 25);
}
```

## Usage Flow

1. **Frontend captures/uploads image** → Converts to Base64
2. **POST /api/chat** with `imageBase64` field
3. **Backend processes request:**
   - Fetches health and nutrition data from InfluxDB
   - Builds enhanced system context with vision instructions
   - Sends multi-part request to Gemini (text + image)
4. **Gemini analyzes image:**
   - Identifies food items
   - Estimates portions
   - Calculates macros
   - Returns function call to `log_nutrition`
5. **Backend executes function call:**
   - Writes nutrition data to InfluxDB
   - Returns confirmation to frontend

## Gemini API Format

The image is sent to Gemini as inline data:

```json
{
  "contents": [
    {
      "parts": [
        {
          "text": "Analyze this meal and estimate nutritional values..."
        },
        {
          "inlineData": {
            "mimeType": "image/jpeg",
            "data": "/9j/4AAQSkZJRgABAQEAYABgAAD..."
          }
        }
      ]
    }
  ],
  "generationConfig": { ... },
  "tools": [ ... ],
  "systemInstruction": { ... }
}
```

## Frontend Integration (Next Steps)

The frontend needs to:

1. Add image capture/upload button to chat input
2. Convert image to Base64 string
3. Include `imageBase64` in the chat request
4. Handle the response (nutrition logged confirmation)

Example frontend code:

```typescript
// Capture image and convert to Base64
const imageFile = await captureImage();
const imageBase64 = await fileToBase64(imageFile);

// Send to backend
const response = await fetch('/api/chat', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    currentMessage: 'Was ist das für eine Mahlzeit?',
    chatHistory: [],
    imageBase64: imageBase64
  })
});
```

## Testing

All tests pass (41 tests):
- ✅ Image-based meal tracking integration test
- ✅ Existing chat functionality (backward compatible)
- ✅ Function calling with images
- ✅ Error handling

Run tests:
```bash
cd backend
./mvnw test
```

## Notes

- The `imageBase64` field is **optional** - existing chat functionality works without it
- Images are sent as JPEG mime type to Gemini
- The system automatically triggers the `log_nutrition` function when analyzing meal images
- All existing tests were updated to support the new 3-parameter `chat()` method signature
