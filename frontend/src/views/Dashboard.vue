<script setup lang="ts">
import { computed } from 'vue';
import { useAppStore } from '../stores/appStore';
import HealthCard from '../components/HealthCard.vue';
import NutritionCard from '../components/NutritionCard.vue';
import TrainingCard from '../components/TrainingCard.vue';

const store = useAppStore();

const healthMetrics = computed(() => store.healthMetrics);
const nutritionMetrics = computed(() => store.nutritionMetrics);
const trainingMetrics = computed(() => store.trainingMetrics);
</script>

<template>
  <div class="flex-1 overflow-y-auto pb-20 px-4 pt-6">
    <h1 class="text-2xl font-bold mb-6 text-gray-800 dark:text-gray-100">Dashboard</h1>

    <div class="space-y-4 max-w-2xl mx-auto">
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
