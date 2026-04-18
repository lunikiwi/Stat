# Insert test health data into InfluxDB for testing the Stat AI Coach (PowerShell)

$INFLUX_URL = "http://localhost:8086"
$INFLUX_TOKEN = "super_secret_dev_token"
$INFLUX_ORG = "stat_home"
$INFLUX_BUCKET = "garmin_data"

Write-Host "🏥 Inserting test health data into InfluxDB..." -ForegroundColor Cyan
Write-Host ""

# Get current timestamp in nanoseconds
$TIMESTAMP = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds() * 1000000

$headers = @{
    "Authorization" = "Token $INFLUX_TOKEN"
    "Content-Type" = "text/plain"
}

# Insert Body Battery data
Write-Host "📊 Inserting Body Battery data..." -ForegroundColor Yellow
$body = "body_battery value=65 $TIMESTAMP"
Invoke-RestMethod -Uri "$INFLUX_URL/api/v2/write?org=$INFLUX_ORG&bucket=$INFLUX_BUCKET&precision=ns" `
    -Method Post -Headers $headers -Body $body | Out-Null

# Insert Sleep Score data
Write-Host "😴 Inserting Sleep Score data..." -ForegroundColor Yellow
$body = "sleep_score value=78 $TIMESTAMP"
Invoke-RestMethod -Uri "$INFLUX_URL/api/v2/write?org=$INFLUX_ORG&bucket=$INFLUX_BUCKET&precision=ns" `
    -Method Post -Headers $headers -Body $body | Out-Null

# Insert Training Minutes data
Write-Host "🏋️  Inserting Training Minutes data..." -ForegroundColor Yellow
$body = "training_minutes value=45 $TIMESTAMP"
Invoke-RestMethod -Uri "$INFLUX_URL/api/v2/write?org=$INFLUX_ORG&bucket=$INFLUX_BUCKET&precision=ns" `
    -Method Post -Headers $headers -Body $body | Out-Null

# Insert Nutrition data
Write-Host "🍎 Inserting Nutrition data..." -ForegroundColor Yellow
$body = "nutrition_calories value=1850 $TIMESTAMP"
Invoke-RestMethod -Uri "$INFLUX_URL/api/v2/write?org=$INFLUX_ORG&bucket=$INFLUX_BUCKET&precision=ns" `
    -Method Post -Headers $headers -Body $body | Out-Null

$body = "nutrition_protein value=140 $TIMESTAMP"
Invoke-RestMethod -Uri "$INFLUX_URL/api/v2/write?org=$INFLUX_ORG&bucket=$INFLUX_BUCKET&precision=ns" `
    -Method Post -Headers $headers -Body $body | Out-Null

$body = "nutrition_carbs value=180 $TIMESTAMP"
Invoke-RestMethod -Uri "$INFLUX_URL/api/v2/write?org=$INFLUX_ORG&bucket=$INFLUX_BUCKET&precision=ns" `
    -Method Post -Headers $headers -Body $body | Out-Null

$body = "nutrition_fat value=60 $TIMESTAMP"
Invoke-RestMethod -Uri "$INFLUX_URL/api/v2/write?org=$INFLUX_ORG&bucket=$INFLUX_BUCKET&precision=ns" `
    -Method Post -Headers $headers -Body $body | Out-Null

Write-Host ""
Write-Host "✅ Test data inserted successfully!" -ForegroundColor Green
Write-Host ""
Write-Host "📋 Summary of inserted data:" -ForegroundColor Cyan
Write-Host "  - Body Battery: 65/100"
Write-Host "  - Sleep Score: 78/100"
Write-Host "  - Training Minutes: 45 min"
Write-Host "  - Calories: 1850 kcal"
Write-Host "  - Protein: 140g"
Write-Host "  - Carbs: 180g"
Write-Host "  - Fat: 60g"
Write-Host ""
Write-Host "🧪 Now test the chat API:" -ForegroundColor Cyan
Write-Host 'curl -X POST http://localhost:8081/api/chat -H "Content-Type: application/json" -d "{\"currentMessage\": \"What should I eat today?\"}"'
