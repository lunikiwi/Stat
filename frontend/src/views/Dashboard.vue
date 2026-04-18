<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { useAppStore } from '../stores/appStore';
import HealthCard from '../components/HealthCard.vue';
import NutritionCard from '../components/NutritionCard.vue';
import TrainingCard from '../components/TrainingCard.vue';

const store = useAppStore();

const healthMetrics = computed(() => store.healthMetrics);
const nutritionMetrics = computed(() => store.nutritionMetrics);
const trainingMetrics = computed(() => store.trainingMetrics);
const isLoading = computed(() => store.isLoading);
const error = computed(() => store.error);

onMounted(() => {
  store.loadMetrics();
});
</script>

<template>
  <div class="flex-1 overflow-y-auto pb-20 px-4 pt-6">
    <h1 class="text-2xl font-bold mb-6 text-gray-800 dark:text-gray-100">Dashboard</h1>

    <!-- Error Message -->
    <div
      v-if="error"
      class="max-w-2xl mx-auto mb-4 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4"
    >
      <div class="flex items-start">
        <svg class="w-5 h-5 text-red-600 dark:text-red-400 mt-0.5 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        <div>
          <h3 class="text-sm font-medium text-red-800 dark:text-red-200">Fehler beim Laden der Daten</h3>
          <p class="text-sm text-red-700 dark:text-red-300 mt-1">{{ error }}</p>
        </div>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="isLoading" class="space-y-4 max-w-2xl mx-auto">
      <div v-for="i in 3" :key="i" class="bg-white dark:bg-gray-800 rounded-2xl shadow-md p-6 animate-pulse">
        <div class="h-6 bg-gray-200 dark:bg-gray-700 rounded w-1/4 mb-4"></div>
        <div class="space-y-3">
          <div class="h-4 bg-gray-200 dark:bg-gray-700 rounded"></div>
          <div class="h-4 bg-gray-200 dark:bg-gray-700 rounded w-5/6"></div>
        </div>
      </div>
    </div>

    <!-- Content -->
    <div v-else class="space-y-4 max-w-2xl mx-auto">
      <HealthCard :metrics="healthMetrics" />
      <NutritionCard :metrics="nutritionMetrics" />
      <TrainingCard :metrics="trainingMetrics" />
    </div>

    <!-- Quick Add FAB -->
    <button
      class="fixed bottom-24 right-6 w-14 h-14 bg-blue-600 hover:bg-blue-700 text-white rounded-full shadow-lg flex items-center justify-center transition-colors"
      @click="() => {}"
    >
      <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
      </svg>
    </button>
  </div>
</template>
