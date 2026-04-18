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

echo ""
echo "✅ Test data inserted successfully!"
echo ""
echo "📋 Summary of inserted data:"
echo "  - Body Battery: 65/100"
echo "  - Sleep Score: 78/100"
echo "  - Training Minutes: 45 min"
echo ""
echo "🧪 Now test the chat API:"
echo "curl -X POST http://localhost:8081/api/chat \\"
echo "  -H \"Content-Type: application/json\" \\"
echo "  -d '{\"currentMessage\": \"What should I eat today?\"}'"
