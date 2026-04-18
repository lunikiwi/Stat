# Stat Backend

Quarkus-based REST API for the Stat AI Coach application.

## Prerequisites

- Java 21+
- Maven 3.9+
- InfluxDB running (via Docker Compose)

## Environment Setup

### 1. Configure API Keys

Copy the example environment file and add your API keys:

```bash
cp .env.example .env
```

Then edit `backend/.env` and add your actual API keys:

```env
# Google AI Studio (Gemini) API Key
GEMINI_API_KEY=your_actual_gemini_key_here

# Spoonacular API Key
SPOONACULAR_API_KEY=your_actual_spoonacular_key_here
```

**Important:** The `.env` file is gitignored and should never be committed to version control.

### 2. Load Environment Variables

Quarkus will automatically read environment variables from your system. To load the `.env` file:

**Option A: Using direnv (Recommended)**
```bash
# Install direnv (if not already installed)
# On macOS: brew install direnv
# On Ubuntu: sudo apt install direnv

# Allow direnv in this directory
direnv allow .
```

**Option B: Manual export (Linux/Mac)**
```bash
export $(cat .env | xargs)
```

**Option C: PowerShell (Windows)**
```powershell
Get-Content .env | ForEach-Object {
    if ($_ -match '^([^=]+)=(.*)$') {
        [Environment]::SetEnvironmentVariable($matches[1], $matches[2], 'Process')
    }
}
```

**Option D: Set in IDE**
- IntelliJ IDEA: Run → Edit Configurations → Environment Variables
- VS Code: Add to `.vscode/launch.json`

## Running the Application

### Development Mode

```bash
./mvnw quarkus:dev
```

The API will be available at: http://localhost:8081

### Running Tests

```bash
./mvnw test
```

### Building for Production

```bash
./mvnw package
java -jar target/quarkus-app/quarkus-run.jar
```

## API Endpoints

- `POST /api/chat` - Send a chat message to the AI coach
- Health check: http://localhost:8081/q/health

## Configuration

Main configuration is in `src/main/resources/application.properties`.

API keys are loaded from environment variables:
- `GEMINI_API_KEY` - Google AI Studio API key
- `SPOONACULAR_API_KEY` - Spoonacular nutrition API key

## Documentation

- [Gemini Setup Guide](docs/GEMINI_SETUP.md) - How to configure Google AI Studio
- [REST Clients](docs/REST_CLIENTS.md) - External API integrations
- [InfluxDB Queries](docs/influxdb-queries.md) - Database query examples

## Troubleshooting

### Environment variables not loading

Make sure you've exported the variables before running `./mvnw quarkus:dev`. Check with:
```bash
echo $GEMINI_API_KEY
```

### API key errors

Test your Gemini API key directly:
```bash
./test-gemini-api.sh YOUR_API_KEY
```

Or on Windows:
```powershell
.\test-gemini-api.ps1 YOUR_API_KEY
```
