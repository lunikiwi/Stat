# Specification: Metrics API

**Endpoint:** `GET /api/metrics`
**Purpose:** Provides the latest health and nutrition data for the dashboard view without triggering a chat interaction.

## Data Points (Aggregated from InfluxDB)
- **Sleep Score:** Last recorded value within the last 24h.
- **Body Battery:** Most recent value.
- **Training Load:** Sum of training minutes in the last 48h.
- **Nutrition:** Sum of calories, protein, carbs, and fat for the current day (since 00:00).

## Response (200 OK)
```json
{
  "health": {
    "sleepScore": 85,
    "bodyBattery": 45,
    "trainingLoadMinutes48h": 120
  },
  "nutrition": {
    "calories": 1850,
    "protein": 140,
    "carbs": 180,
    "fat": 60
  }
}
