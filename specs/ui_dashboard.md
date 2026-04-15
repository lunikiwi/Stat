# Specification: UI Dashboard & Chat (Mobile SPA)

**Concept:** A mobile-first Single Page Application with two main tabs (Dashboard & Coach). Minimalist, clean design using rounded cards and a bottom navigation bar.

## State Management & Routing
- Use a router (Vue Router / React Router) for the two main views.
- Use a state manager (Pinia / Redux/Context) to persist the `chatHistory` and the latest health/nutrition metrics across tab switches.

## View 1: Dashboard (`/`)
A scrollable overview of the current day using rounded cards.
1. **Health Card:** Displays 'Sleep Score' and 'Body Battery' (use circular progress indicators or simple bold numbers).
2. **Nutrition Card:** Displays Macros (Protein, Carbs, Fat) as simple progress bars or text, and remaining Calories.
3. **Training Card:** Displays the next planned workout.
4. **Quick-Add Action:** A floating action button (FAB) or prominent button to quickly add a meal (opens a simple modal/prompt).

## View 2: Coach Chat (`/chat`)
The primary AI interaction interface.
1. **Message List:** Scrollable area. User messages right-aligned, Coach messages left-aligned. Must render Markdown.
2. **Input Area:** Bottom-fixed text input with a submit button.
3. **API Integration:** Submitting a message updates the local state and sends the `chatHistory` array to `POST /api/chat` (as defined in `api_chat.md`).

## Bottom Navigation
A fixed bottom bar to switch between the 'Dashboard' and 'Coach' views.
