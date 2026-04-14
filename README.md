# Stat

A self-hosted AI personal coach that actually knows your current physical state.

Most fitness apps exist in their own little worlds. Garmin knows your sleep and workouts, your food tracker knows your calories, and ChatGPT is smart but knows nothing about your day. **Stat** connects these pieces.

It grabs your daily Garmin health data (via a Python scraper) and your nutritional tracking, and feeds it into an LLM. Instead of checking three different apps, you get a simple dashboard and a chat where you can ask: *"I slept horribly but have a hard interval session in 2 hours. What should I eat right now?"* – and the AI answers based on your actual live data.

## System Architecture

The project is split into three simple parts:

1. **Data Ingestion (Python & InfluxDB):** A dockerized Python script (`garminconnect`) that scrapes your daily metrics and stores them as time-series data in InfluxDB.
2. **Backend (Java / Quarkus):** The brain. It reads the InfluxDB data, pulls in food macros, builds the prompt, and talks to the OpenAI/Gemini API.
3. **Frontend (Vue/React):** A clean, mobile-first dashboard and chat interface.

## Agentic Development Workflow

This project is built using AI coding agents (Roo Code / Cline) and follows a strict Spec-Driven (SDD) and Test-Driven (TDD) workflow to keep the architecture clean.

- `/specs/`: The absolute source of truth. Contains markdown-based contracts for all APIs and UI components.
- `/skills/`: Specialized system prompts for the AI agents (like architectural reviews or TDD rules).
- `.roomodes` & `.clinerules`: Define strict roles and rules to stop the AI from hallucinating or breaking the architecture.

## Getting Started

### 1. Infrastructure (Docker)
Create a `.env` file in the root directory (do not commit this file):
```env
GARMIN_EMAIL=your_email
GARMIN_PASSWORD=your_password
INFLUX_USER=stat_admin
INFLUX_PASSWORD=your_secure_password
INFLUX_ORG=stat_home
INFLUX_BUCKET=garmin_data
INFLUX_TOKEN=your_secure_token
```

Start the data scraper and database:
```bash
docker compose up -d
```

InfluxDB will be available at http://localhost:8086.

2. Backend (Quarkus)
```bash
cd backend
./mvnw compile quarkus:dev
```

3. Frontend
```bash
cd frontend
npm install
npm run dev
```

MIT License - see the LICENSE file for details.
