import { createRouter, createWebHistory } from 'vue-router';
import Dashboard from '../views/Dashboard.vue';
import Chat from '../views/Chat.vue';
import NutritionJournal from '../views/NutritionJournal.vue';

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'dashboard',
      component: Dashboard,
    },
    {
      path: '/chat',
      name: 'chat',
      component: Chat,
    },
    {
      path: '/nutrition',
      name: 'nutrition',
      component: NutritionJournal,
    },
  ],
});

export default router;
