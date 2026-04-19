# User Profile Configuration

The AI Coach uses a personalized user profile to provide tailored coaching advice. This profile is configured via environment variables in the `.env` file.

## Setup

1. Copy the example file:
   ```bash
   cp .env.example .env
   ```

2. Edit `.env` and fill in your personal information:
   ```bash
   # User Profile (Personal Data)
   STAT_USER_NAME=Your Name
   STAT_USER_GOAL=Your fitness/health goal
   STAT_USER_DIET=Your dietary preferences and restrictions
   STAT_USER_METRICS=Your personal metrics (age, weight, height, activity level)
   STAT_USER_PERSONA=Your preferred coaching style
   ```

## Configuration Options

### STAT_USER_NAME
Your name. The AI coach will use this to personalize responses.

**Example:**
```
STAT_USER_NAME=Max
```

### STAT_USER_GOAL
Your primary fitness or health goal. Be specific about what you want to achieve.

**Examples:**
```
STAT_USER_GOAL=Fokus auf Fettabbau bei gleichzeitigem Erhalt der Kraftleistung (leichtes Defizit)
STAT_USER_GOAL=Improve endurance for marathon training
STAT_USER_GOAL=Build muscle mass while maintaining low body fat
```

### STAT_USER_DIET
Your dietary preferences, restrictions, and nutritional approach.

**Examples:**
```
STAT_USER_DIET=Allesesser, aber Fokus auf unverarbeitete Lebensmittel und hohe Proteinzufuhr (ca. 2g/kg). Keine Allergien.
STAT_USER_DIET=Vegetarian, high protein, no dairy allergies
STAT_USER_DIET=Flexible dieting (IIFYM), tracking macros
```

### STAT_USER_METRICS
Your personal metrics that help the coach understand your context.

**Examples:**
```
STAT_USER_METRICS=Männlich, 26 Jahre alt, 180cm, 78kg. Aktivitätslevel: Bürojob + 4x Kraftsport pro Woche
STAT_USER_METRICS=Female, 32 years old, 165cm, 62kg. Activity: Office job + 3x running per week
STAT_USER_METRICS=Male, 28 years old, 175cm, 85kg. Activity: Active job + 5x gym per week
```

### STAT_USER_PERSONA
Define how you want the AI coach to communicate with you.

**Examples:**
```
STAT_USER_PERSONA=Du bist ein direkter, wissenschaftlich fundierter Coach. Antworte präzise, verzichte auf Floskeln und nutze das "Du".
STAT_USER_PERSONA=You are a supportive and encouraging coach. Use simple language and focus on motivation.
STAT_USER_PERSONA=You are a no-nonsense, data-driven coach. Be direct and focus on facts.
```

## Privacy & Security

⚠️ **IMPORTANT:** The `.env` file contains your personal information and is **NOT** committed to Git.

- The `.env` file is listed in `.gitignore`
- Never commit your `.env` file to version control
- Only `.env.example` (with placeholder values) is committed to the repository
- Each user configures their own `.env` file locally

## Default Values

If environment variables are not set, the following defaults are used:

```properties
stat.user.name=User
stat.user.goal=Improve overall health and fitness
stat.user.diet=Balanced diet
stat.user.metrics=Standard metrics
stat.user.persona=You are a helpful and motivating coach
```

## How It Works

The user profile is loaded by [`UserProfileConfig`](../src/main/java/dev/stat/chat/config/UserProfileConfig.java) and used by [`ChatService`](../src/main/java/dev/stat/chat/service/ChatService.java) to generate a system context instruction for the LLM.

This system context is sent with every chat request to ensure the AI coach:
- Knows who you are
- Understands your goals
- Respects your dietary preferences
- Considers your personal metrics
- Communicates in your preferred style

## Testing

The configuration is tested in [`UserProfileConfigTest`](../src/test/java/dev/stat/chat/config/UserProfileConfigTest.java).

Run tests:
```bash
./mvnw test -Dtest=UserProfileConfigTest
```
