<script setup lang="ts">
import { onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useAppStore } from '../stores/appStore';

const store = useAppStore();
const router = useRouter();

const entries = computed(() => store.nutritionEntries);
const isLoading = computed(() => store.isLoading);
const error = computed(() => store.error);
const successMessage = computed(() => store.successMessage);

onMounted(async () => {
  await store.loadNutritionEntries();
});

const formatTime = (timestamp: string) => {
  const date = new Date(timestamp);
  return date.toLocaleTimeString('de-DE', { hour: '2-digit', minute: '2-digit' });
};

const handleDelete = async (timestamp: string) => {
  if (confirm('Möchtest du diesen Eintrag wirklich löschen?')) {
    try {
      await store.removeNutritionEntry(timestamp);
    } catch (error) {
      // Error is already handled in the store
      console.error('Failed to delete entry:', error);
    }
  }
};

const goBack = () => {
  router.push('/');
};

const getTotalCalories = computed(() => {
  return entries.value.reduce((sum, entry) => sum + entry.calories, 0);
});

const getTotalProtein = computed(() => {
  return entries.value.reduce((sum, entry) => sum + entry.proteinGrams, 0);
});

const getTotalCarbs = computed(() => {
  return entries.value.reduce((sum, entry) => sum + entry.carbsGrams, 0);
});

const getTotalFat = computed(() => {
  return entries.value.reduce((sum, entry) => sum + entry.fatGrams, 0);
});
</script>

<template>
  <div class="min-h-screen bg-gray-50 dark:bg-gray-900 pb-20">
    <!-- Header -->
    <div class="bg-white dark:bg-gray-800 shadow-sm sticky top-0 z-10">
      <div class="max-w-2xl mx-auto px-4 py-4 flex items-center">
        <button
          @click="goBack"
          class="mr-4 p-2 rounded-full hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
          aria-label="Zurück"
        >
          <svg class="w-6 h-6 text-gray-700 dark:text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
          </svg>
        </button>
        <h1 class="text-xl font-semibold text-gray-800 dark:text-gray-100">Ernährungs-Journal</h1>
      </div>
    </div>

    <div class="max-w-2xl mx-auto px-4 py-6">
      <!-- Success Message -->
      <div
        v-if="successMessage"
        class="mb-4 p-4 bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 rounded-lg"
      >
        <p class="text-sm text-green-800 dark:text-green-200">{{ successMessage }}</p>
      </div>

      <!-- Error Message -->
      <div
        v-if="error"
        class="mb-4 p-4 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg"
      >
        <p class="text-sm text-red-800 dark:text-red-200">{{ error }}</p>
      </div>

      <!-- Summary Card -->
      <div v-if="entries.length > 0" class="bg-white dark:bg-gray-800 rounded-2xl shadow-md p-6 mb-6">
        <h2 class="text-lg font-semibold mb-4 text-gray-800 dark:text-gray-100">Tages-Übersicht</h2>
        <div class="grid grid-cols-2 gap-4">
          <div class="text-center">
            <p class="text-2xl font-bold text-gray-800 dark:text-gray-100">{{ getTotalCalories }}</p>
            <p class="text-sm text-gray-600 dark:text-gray-400">Kalorien</p>
          </div>
          <div class="text-center">
            <p class="text-2xl font-bold text-red-500">{{ getTotalProtein }}g</p>
            <p class="text-sm text-gray-600 dark:text-gray-400">Protein</p>
          </div>
          <div class="text-center">
            <p class="text-2xl font-bold text-yellow-500">{{ getTotalCarbs }}g</p>
            <p class="text-sm text-gray-600 dark:text-gray-400">Carbs</p>
          </div>
          <div class="text-center">
            <p class="text-2xl font-bold text-purple-500">{{ getTotalFat }}g</p>
            <p class="text-sm text-gray-600 dark:text-gray-400">Fat</p>
          </div>
        </div>
      </div>

      <!-- Loading State -->
      <div v-if="isLoading" class="text-center py-8">
        <div class="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-gray-900 dark:border-gray-100"></div>
        <p class="mt-2 text-sm text-gray-600 dark:text-gray-400">Lade Einträge...</p>
      </div>

      <!-- Empty State -->
      <div
        v-else-if="entries.length === 0"
        class="bg-white dark:bg-gray-800 rounded-2xl shadow-md p-8 text-center"
      >
        <svg class="w-16 h-16 mx-auto text-gray-400 dark:text-gray-600 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
        </svg>
        <h3 class="text-lg font-semibold text-gray-800 dark:text-gray-100 mb-2">Keine Einträge</h3>
        <p class="text-sm text-gray-600 dark:text-gray-400">Du hast heute noch keine Mahlzeiten eingetragen.</p>
      </div>

      <!-- Entries List -->
      <div v-else class="space-y-3">
        <div
          v-for="entry in entries"
          :key="entry.timestamp"
          class="bg-white dark:bg-gray-800 rounded-2xl shadow-md p-4 flex items-center justify-between hover:shadow-lg transition-shadow"
        >
          <div class="flex-1">
            <div class="flex items-center mb-2">
              <span class="text-sm font-semibold text-gray-800 dark:text-gray-100">
                {{ formatTime(entry.timestamp) }}
              </span>
            </div>
            <div class="grid grid-cols-4 gap-2 text-sm">
              <div>
                <p class="font-semibold text-gray-800 dark:text-gray-100">{{ entry.calories }}</p>
                <p class="text-xs text-gray-600 dark:text-gray-400">kcal</p>
              </div>
              <div>
                <p class="font-semibold text-red-500">{{ entry.proteinGrams }}g</p>
                <p class="text-xs text-gray-600 dark:text-gray-400">Protein</p>
              </div>
              <div>
                <p class="font-semibold text-yellow-500">{{ entry.carbsGrams }}g</p>
                <p class="text-xs text-gray-600 dark:text-gray-400">Carbs</p>
              </div>
              <div>
                <p class="font-semibold text-purple-500">{{ entry.fatGrams }}g</p>
                <p class="text-xs text-gray-600 dark:text-gray-400">Fat</p>
              </div>
            </div>
          </div>
          <button
            @click="handleDelete(entry.timestamp)"
            class="ml-4 p-2 rounded-full hover:bg-red-50 dark:hover:bg-red-900/20 transition-colors group"
            aria-label="Eintrag löschen"
          >
            <svg class="w-5 h-5 text-gray-400 group-hover:text-red-500 transition-colors" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
            </svg>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
