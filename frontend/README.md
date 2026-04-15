# Stat Frontend

Mobile-first Single Page Application for the Stat AI Coach.

## Tech Stack

- **Framework:** Vue 3 with TypeScript
- **Build Tool:** Vite
- **Styling:** Tailwind CSS
- **Routing:** Vue Router
- **State Management:** Pinia
- **Markdown Rendering:** Marked

## Project Structure

```
src/
├── components/       # Reusable UI components
│   ├── BottomNav.vue
│   ├── ChatMessage.vue
│   ├── HealthCard.vue
│   ├── NutritionCard.vue
│   └── TrainingCard.vue
├── views/           # Route views
│   ├── Dashboard.vue
│   └── Chat.vue
├── stores/          # Pinia stores
│   └── appStore.ts
├── router/          # Vue Router configuration
│   └── index.ts
├── services/        # API services
│   └── api.ts
├── types/           # TypeScript type definitions
│   └── index.ts
├── App.vue          # Root component
├── main.ts          # Application entry point
└── style.css        # Global styles (Tailwind)
```

## Development

```bash
# Install dependencies
npm install

# Start dev server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview
```

## Environment Variables

Copy `.env.example` to `.env` and configure:

```
VITE_API_BASE_URL=http://localhost:8080
```

## Features

### Dashboard (`/`)
- Health metrics (Sleep Score, Body Battery)
- Nutrition tracking (Macros, Calories)
- Training overview
- Quick-add meal button (FAB)

### Coach Chat (`/chat`)
- AI-powered chat interface
- Markdown rendering for coach responses
- Persistent chat history (via Pinia)
- Real-time API integration

## Design Principles

- **Mobile-First:** Optimized for mobile devices
- **Clean UI:** Rounded cards, clear vertical lines
- **State Persistence:** Data persists across tab switches
- **Error Handling:** Robust error handling for API calls
