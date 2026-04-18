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

# Insert Body Battery data (scale 0-100)
Write-Host "📊 Inserting Body Battery data..." -ForegroundColor Yellow
$body = "health,metric=body_battery value=65 $TIMESTAMP"
Invoke-RestMethod -Uri "$INFLUX_URL/api/v2/write?org=$INFLUX_ORG&bucket=$INFLUX_BUCKET&precision=ns" `
    -Method Post -Headers $headers -Body $body | Out-Null

# Insert Sleep Score data (scale 0-100)
Write-Host "😴 Inserting Sleep Score data..." -ForegroundColor Yellow
$body = "health,metric=sleep_score value=78 $TIMESTAMP"
Invoke-RestMethod -Uri "$INFLUX_URL/api/v2/write?org=$INFLUX_ORG&bucket=$INFLUX_BUCKET&precision=ns" `
    -Method Post -Headers $headers -Body $body | Out-Null

# Insert Heart Rate data (bpm)
Write-Host "❤️  Inserting Heart Rate data..." -ForegroundColor Yellow
$body = "health,metric=heart_rate value=72 $TIMESTAMP"
Invoke-RestMethod -Uri "$INFLUX_URL/api/v2/write?org=$INFLUX_ORG&bucket=$INFLUX_BUCKET&precision=ns" `
    -Method Post -Headers $headers -Body $body | Out-Null

# Insert Stress Level data (scale 0-100)
Write-Host "😰 Inserting Stress Level data..." -ForegroundColor Yellow
$body = "health,metric=stress_level value=35 $TIMESTAMP"
Invoke-RestMethod -Uri "$INFLUX_URL/api/v2/write?org=$INFLUX_ORG&bucket=$INFLUX_BUCKET&precision=ns" `
    -Method Post -Headers $headers -Body $body | Out-Null

# Insert Steps data
Write-Host "🚶 Inserting Steps data..." -ForegroundColor Yellow
$body = "health,metric=steps value=8500 $TIMESTAMP"
Invoke-RestMethod -Uri "$INFLUX_URL/api/v2/write?org=$INFLUX_ORG&bucket=$INFLUX_BUCKET&precision=ns" `
    -Method Post -Headers $headers -Body $body | Out-Null

# Insert Calories data
Write-Host "🔥 Inserting Calories data..." -ForegroundColor Yellow
$body = "health,metric=calories value=2100 $TIMESTAMP"
Invoke-RestMethod -Uri "$INFLUX_URL/api/v2/write?org=$INFLUX_ORG&bucket=$INFLUX_BUCKET&precision=ns" `
    -Method Post -Headers $headers -Body $body | Out-Null

Write-Host ""
Write-Host "✅ Test data inserted successfully!" -ForegroundColor Green
Write-Host ""
Write-Host "📋 Summary of inserted data:" -ForegroundColor Cyan
Write-Host "  - Body Battery: 65/100"
Write-Host "  - Sleep Score: 78/100"
Write-Host "  - Heart Rate: 72 bpm"
Write-Host "  - Stress Level: 35/100"
Write-Host "  - Steps: 8,500"
Write-Host "  - Calories: 2,100"
Write-Host ""
Write-Host "🧪 Now test the chat API:" -ForegroundColor Cyan
Write-Host 'curl -X POST http://localhost:8081/api/chat -H "Content-Type: application/json" -d "{\"currentMessage\": \"What should I eat today?\"}"'
