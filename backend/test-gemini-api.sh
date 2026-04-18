#!/bin/bash
# Test script to verify Gemini API endpoint and authentication
# Usage: ./test-gemini-api.sh YOUR_API_KEY

API_KEY="${1:-changeme}"

if [ "$API_KEY" = "changeme" ]; then
    echo "❌ Please provide your Gemini API key as an argument"
    echo "Usage: ./test-gemini-api.sh YOUR_API_KEY"
    exit 1
fi

echo "🧪 Testing Gemini API endpoint..."
echo "📍 Endpoint: https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"
echo ""

RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" \
  -X POST "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${API_KEY}" \
  -H "Content-Type: application/json" \
  -d '{
    "contents": [{
      "parts": [{
        "text": "Say hello in one word"
      }]
    }],
    "generationConfig": {
      "temperature": 0.7,
      "maxOutputTokens": 50
    }
  }')

HTTP_STATUS=$(echo "$RESPONSE" | grep "HTTP_STATUS" | cut -d: -f2)
BODY=$(echo "$RESPONSE" | sed '/HTTP_STATUS/d')

echo "📊 HTTP Status: $HTTP_STATUS"
echo ""

if [ "$HTTP_STATUS" = "200" ]; then
    echo "✅ SUCCESS! Gemini API is working correctly"
    echo ""
    echo "📝 Response:"
    echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"
elif [ "$HTTP_STATUS" = "404" ]; then
    echo "❌ ERROR 404: Endpoint not found"
    echo "The API endpoint might have changed. Check Google AI Studio documentation."
    echo ""
    echo "Response:"
    echo "$BODY"
elif [ "$HTTP_STATUS" = "401" ] || [ "$HTTP_STATUS" = "403" ]; then
    echo "❌ ERROR $HTTP_STATUS: Authentication failed"
    echo "Please check your API key at: https://aistudio.google.com/app/apikey"
    echo ""
    echo "Response:"
    echo "$BODY"
else
    echo "❌ ERROR: Unexpected status code"
    echo ""
    echo "Response:"
    echo "$BODY"
fi

exit 0
