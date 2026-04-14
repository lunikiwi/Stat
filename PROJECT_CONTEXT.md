# Project Context: AI Personal Coach App

## Vision
A self-hosted, AI-driven personal coach web app. It acts as a central intelligence connecting health data (Garmin), nutrition data (Spoonacular API), and an LLM (OpenAI/Gemini) to provide highly contextual, proactive, and reactive coaching advice.

## Tech Stack
- **Data Scraping & Storage:** `arpanghosh8453/garmin-grafana` (Docker, Python, InfluxDB).
- **Backend:** Java with Quarkus. Acts as the brain, querying InfluxDB, communicating with external APIs (Nutrition, LLM), and serving the frontend.
- **Frontend:** React/Vue with TypeScript. Mobile-optimized Single Page Application (SPA).
- **External APIs:** OpenAI (or Gemini) for LLM logic, Spoonacular/Edamam for nutrition tracking.

## Architecture Flow
1. Garmin Python Scraper runs in Docker -> Stores time-series data in InfluxDB.
2. User opens Frontend -> Requests dashboard data from Quarkus.
3. Quarkus queries InfluxDB -> Returns recent stats (Sleep, Body Battery, Workouts).
4. User asks chat a question -> Quarkus builds a "Super-Prompt" containing recent InfluxDB stats + Nutrition API data -> Sends to LLM -> Returns answer to user.

## Core Rules for AI Agents
- **NO unnecessary dependencies:** Stick to the defined Tech Stack.
- **Security:** API Keys are NEVER exposed in the frontend. All external calls go through Quarkus.
- **Simplicity:** Prefer readable, standard implementations over complex abstractions.
