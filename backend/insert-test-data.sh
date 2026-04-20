#!/bin/bash
# Insert test health data into InfluxDB for testing the Stat AI Coach

INFLUX_URL="http://localhost:8086"
INFLUX_TOKEN="super_secret_dev_token"
INFLUX_ORG="stat_home"
INFLUX_BUCKET="garmin_data"

echo "🏥 Inserting test health data into InfluxDB..."
echo ""

# Get current timestamp in nanoseconds
TIMESTAMP=$(date +%s)000000000

# Insert Body Battery data (measurement name must match query)
echo "📊 Inserting Body Battery data..."
curl -s -X POST "${INFLUX_URL}/api/v2/write?org=${INFLUX_ORG}&bucket=${INFLUX_BUCKET}&precision=ns" \
  -H "Authorization: Token ${INFLUX_TOKEN}" \
  -H "Content-Type: text/plain" \
  --data-binary "body_battery value=65 ${TIMESTAMP}"

# Insert Sleep Score data (measurement name must match query)
echo "😴 Inserting Sleep Score data..."
curl -s -X POST "${INFLUX_URL}/api/v2/write?org=${INFLUX_ORG}&bucket=${INFLUX_BUCKET}&precision=ns" \
  -H "Authorization: Token ${INFLUX_TOKEN}" \
  -H "Content-Type: text/plain" \
  --data-binary "sleep_score value=78 ${TIMESTAMP}"

# Insert Training Minutes data (measurement name must match query)
echo "🏋️  Inserting Training Minutes data..."
curl -s -X POST "${INFLUX_URL}/api/v2/write?org=${INFLUX_ORG}&bucket=${INFLUX_BUCKET}&precision=ns" \
  -H "Authorization: Token ${INFLUX_TOKEN}" \
  -H "Content-Type: text/plain" \
  --data-binary "training_minutes value=45 ${TIMESTAMP}"

# Insert Body Weight data (measurement name must match query)
echo "⚖️  Inserting Body Weight data..."
curl -s -X POST "${INFLUX_URL}/api/v2/write?org=${INFLUX_ORG}&bucket=${INFLUX_BUCKET}&precision=ns" \
  -H "Authorization: Token ${INFLUX_TOKEN}" \
  -H "Content-Type: text/plain" \
  --data-binary "body_weight value=75.5 ${TIMESTAMP}"

# Insert Water Intake data (measurement name must match query)
echo "💧 Inserting Water Intake data..."
curl -s -X POST "${INFLUX_URL}/api/v2/write?org=${INFLUX_ORG}&bucket=${INFLUX_BUCKET}&precision=ns" \
  -H "Authorization: Token ${INFLUX_TOKEN}" \
  -H "Content-Type: text/plain" \
  --data-binary "water_intake value=1500 ${TIMESTAMP}"

# Insert Nutrition data (measurement names must match query)
echo "🍎 Inserting Nutrition data..."
curl -s -X POST "${INFLUX_URL}/api/v2/write?org=${INFLUX_ORG}&bucket=${INFLUX_BUCKET}&precision=ns" \
  -H "Authorization: Token ${INFLUX_TOKEN}" \
  -H "Content-Type: text/plain" \
  --data-binary "nutrition_calories value=1850 ${TIMESTAMP}"

curl -s -X POST "${INFLUX_URL}/api/v2/write?org=${INFLUX_ORG}&bucket=${INFLUX_BUCKET}&precision=ns" \
  -H "Authorization: Token ${INFLUX_TOKEN}" \
  -H "Content-Type: text/plain" \
  --data-binary "nutrition_protein value=140 ${TIMESTAMP}"

curl -s -X POST "${INFLUX_URL}/api/v2/write?org=${INFLUX_ORG}&bucket=${INFLUX_BUCKET}&precision=ns" \
  -H "Authorization: Token ${INFLUX_TOKEN}" \
  -H "Content-Type: text/plain" \
  --data-binary "nutrition_carbs value=180 ${TIMESTAMP}"

curl -s -X POST "${INFLUX_URL}/api/v2/write?org=${INFLUX_ORG}&bucket=${INFLUX_BUCKET}&precision=ns" \
  -H "Authorization: Token ${INFLUX_TOKEN}" \
  -H "Content-Type: text/plain" \
  --data-binary "nutrition_fat value=60 ${TIMESTAMP}"

echo ""
echo "✅ Test data inserted successfully!"
echo ""
echo "📋 Summary of inserted data:"
echo "  - Body Battery: 65/100"
echo "  - Sleep Score: 78/100"
echo "  - Training Minutes: 45 min"
echo "  - Body Weight: 75.5 kg"
echo "  - Water Intake: 1500 ml"
echo "  - Calories: 1850 kcal"
echo "  - Protein: 140g"
echo "  - Carbs: 180g"
echo "  - Fat: 60g"
echo ""
echo "🧪 Now test the chat API:"
echo "curl -X POST http://localhost:8081/api/chat \\"
echo "  -H \"Content-Type: application/json\" \\"
echo "  -d '{\"currentMessage\": \"What should I eat today?\"}'"
