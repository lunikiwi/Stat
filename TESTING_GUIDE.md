# Testing Guide - Stat AI Coach

## Quick Test: LLM Only (No InfluxDB needed)

The simplest way to test if Gemini is working:

### 1. Test Gemini API Directly

First, verify your API key works:

**Windows PowerShell:**
```powershell
cd backend
.\test-gemini-api.ps1 REDACTED_KEY (rotated already)
```

**Expected Output:**
```
✅ SUCCESS! Gemini API is working correctly
```

### 2. Test Backend API with curl

Start only the backend (InfluxDB errors are OK for this test):

```bash
cd backend

# Load environment variables (PowerShell)
Get-Content .env | ForEach-Object {
    if ($_ -match '^([^=]+)=(.*)$') {
        [Environment]::SetEnvironmentVariable($matches[1], $matches[2], 'Process')
    }
}

# Start backend
./mvnw quarkus:dev
```

Then in another terminal, test the chat endpoint:

```bash
curl -X POST http://localhost:8081/api/chat \
  -H "Content-Type: application/json" \
  -d "{\"currentMessage\": \"Hello, what should I eat today?\"}"
```

**Expected Response:**
```json
{
  "response": "Based on your current health data...",
  "metricsUsed": {...}
}
```

**Note:** You might see InfluxDB connection errors in the backend logs, but the LLM should still respond with a generic answer.

---

## Full Stack Test (All Services)

To test everything including health data from InfluxDB:

### 1. Start InfluxDB

```bash
# From project root
docker-compose up -d influxdb
```

Verify it's running:
```bash
docker ps
```

### 2. Start Backend

```bash
cd backend

# Load environment variables (PowerShell)
Get-Content .env | ForEach-Object {
    if ($_ -match '^([^=]+)=(.*)$') {
        [Environment]::SetEnvironmentVariable($matches[1], $matches[2], 'Process')
    }
}

# Start backend
./mvnw quarkus:dev
```

Backend will be at: http://localhost:8081

### 3. Start Frontend

```bash
cd frontend
npm install  # if not done yet
npm run dev
```

Frontend will be at: http://localhost:5173

### 4. Test in Browser

1. Open http://localhost:5173
2. Navigate to the Chat page
3. Send a message: "What should I eat today?"
4. You should get an AI response from Gemini

---

## Testing Scenarios

### Scenario A: Just LLM (Quickest)
**What you need:**
- ✅ Backend running
- ✅ Gemini API key in `.env`
- ❌ InfluxDB (not needed)
- ❌ Frontend (not needed)

**Test with:**
```bash
curl -X POST http://localhost:8081/api/chat \
  -H "Content-Type: application/json" \
  -d "{\"currentMessage\": \"Hello\"}"
```

### Scenario B: Backend + Frontend (No real data)
**What you need:**
- ✅ Backend running
- ✅ Frontend running
- ✅ Gemini API key
- ❌ InfluxDB (LLM will work without health data)

**Test with:**
- Open browser to http://localhost:5173
- Use the chat interface

### Scenario C: Full Stack (With health data)
**What you need:**
- ✅ InfluxDB running (docker-compose)
- ✅ Backend running
- ✅ Frontend running
- ✅ Gemini API key
- ✅ Health data in InfluxDB

**Test with:**
- Open browser to http://localhost:5173
- View dashboard (shows health metrics)
- Use chat (AI uses your actual health data)

---

## Troubleshooting

### "Environment variable not found"

Make sure you loaded the `.env` file before starting the backend:

**PowerShell:**
```powershell
cd backend
Get-Content .env | ForEach-Object {
    if ($_ -match '^([^=]+)=(.*)$') {
        [Environment]::SetEnvironmentVariable($matches[1], $matches[2], 'Process')
    }
}
```

Verify:
```powershell
echo $env:GEMINI_API_KEY
```

### "InfluxDB connection failed"

This is OK if you're just testing the LLM! The chat will still work, just without personalized health data.

To fix it, start InfluxDB:
```bash
docker-compose up -d influxdb
```

### "Gemini API 404 error"

The endpoint might have changed. Test directly:
```powershell
.\test-gemini-api.ps1 YOUR_API_KEY
```

### "CORS error in browser"

Make sure:
1. Backend is running on port 8081
2. Frontend is running on port 5173
3. Check `application.properties` has correct CORS settings

---

## Quick Start Commands

**Minimal Test (LLM only):**
```powershell
# Terminal 1: Start backend
cd backend
Get-Content .env | ForEach-Object { if ($_ -match '^([^=]+)=(.*)$') { [Environment]::SetEnvironmentVariable($matches[1], $matches[2], 'Process') } }
./mvnw quarkus:dev

# Terminal 2: Test with curl
curl -X POST http://localhost:8081/api/chat -H "Content-Type: application/json" -d "{\"currentMessage\": \"Hello\"}"
```

**Full Stack:**
```bash
# Terminal 1: InfluxDB
docker-compose up influxdb

# Terminal 2: Backend
cd backend
# Load .env first (see above)
./mvnw quarkus:dev

# Terminal 3: Frontend
cd frontend
npm run dev

# Browser: http://localhost:5173
```
