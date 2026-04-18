# Test script to verify Gemini API endpoint and authentication (PowerShell)
# Usage: .\test-gemini-api.ps1 YOUR_API_KEY

param(
    [string]$ApiKey = "changeme"
)

if ($ApiKey -eq "changeme") {
    Write-Host "❌ Please provide your Gemini API key as an argument" -ForegroundColor Red
    Write-Host "Usage: .\test-gemini-api.ps1 YOUR_API_KEY"
    exit 1
}

Write-Host "🧪 Testing Gemini API endpoint..." -ForegroundColor Cyan
Write-Host "📍 Endpoint: https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"
Write-Host ""

$uri = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$ApiKey"
$body = @{
    contents = @(
        @{
            parts = @(
                @{
                    text = "Say hello in one word"
                }
            )
        }
    )
    generationConfig = @{
        temperature = 0.7
        maxOutputTokens = 50
    }
} | ConvertTo-Json -Depth 10

try {
    $response = Invoke-WebRequest -Uri $uri -Method Post -Body $body -ContentType "application/json" -ErrorAction Stop

    Write-Host "📊 HTTP Status: $($response.StatusCode)" -ForegroundColor Green
    Write-Host ""
    Write-Host "✅ SUCCESS! Gemini API is working correctly" -ForegroundColor Green
    Write-Host ""
    Write-Host "📝 Response:" -ForegroundColor Cyan
    $response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 10

} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Write-Host "📊 HTTP Status: $statusCode" -ForegroundColor Red
    Write-Host ""

    if ($statusCode -eq 404) {
        Write-Host "❌ ERROR 404: Endpoint not found" -ForegroundColor Red
        Write-Host "The API endpoint might have changed. Check Google AI Studio documentation."
    } elseif ($statusCode -eq 401 -or $statusCode -eq 403) {
        Write-Host "❌ ERROR $statusCode : Authentication failed" -ForegroundColor Red
        Write-Host "Please check your API key at: https://aistudio.google.com/app/apikey"
    } else {
        Write-Host "❌ ERROR: Unexpected status code" -ForegroundColor Red
    }

    Write-Host ""
    Write-Host "Response:" -ForegroundColor Yellow
    $_.Exception.Response | Out-String
}
