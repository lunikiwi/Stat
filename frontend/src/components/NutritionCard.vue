<script setup lang="ts">
import type { NutritionMetrics } from '../types';

defineProps<{
  metrics: NutritionMetrics | null;
}>();

const getProgressPercentage = (current: number, total: number) => {
  return Math.min((current / total) * 100, 100);
};
</script>

<template>
  <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-md p-6">
    <h2 class="text-lg font-semibold mb-4 text-gray-800 dark:text-gray-100">Nutrition</h2>

    <div v-if="metrics" class="space-y-4">
      <!-- Calories -->
      <div>
        <div class="flex justify-between text-sm mb-1">
          <span class="text-gray-600 dark:text-gray-400">Kalorien</span>
          <span class="font-semibold text-gray-800 dark:text-gray-100">
            {{ metrics.calories }} / {{ metrics.calories + metrics.caloriesRemaining }}
          </span>
        </div>
        <div class="text-xs text-gray-500 dark:text-gray-500 mb-2">
          {{ metrics.caloriesRemaining }} verbleibend
        </div>
      </div>

      <!-- Macros -->
      <div class="space-y-3">
        <!-- Protein -->
        <div>
          <div class="flex justify-between text-sm mb-1">
            <span class="text-gray-600 dark:text-gray-400">Protein</span>
            <span class="font-semibold text-gray-800 dark:text-gray-100">{{ metrics.protein }}g</span>
          </div>
          <div class="w-full bg-gray-200 dark:bg-gray-700 rounded-full h-2">
            <div
              class="bg-red-500 h-2 rounded-full transition-all"
              :style="{ width: `${getProgressPercentage(metrics.protein, 150)}%` }"
            ></div>
          </div>
        </div>

        <!-- Carbs -->
        <div>
          <div class="flex justify-between text-sm mb-1">
            <span class="text-gray-600 dark:text-gray-400">Carbs</span>
            <span class="font-semibold text-gray-800 dark:text-gray-100">{{ metrics.carbs }}g</span>
          </div>
          <div class="w-full bg-gray-200 dark:bg-gray-700 rounded-full h-2">
            <div
              class="bg-yellow-500 h-2 rounded-full transition-all"
              :style="{ width: `${getProgressPercentage(metrics.carbs, 250)}%` }"
            ></div>
          </div>
        </div>

        <!-- Fat -->
        <div>
          <div class="flex justify-between text-sm mb-1">
            <span class="text-gray-600 dark:text-gray-400">Fat</span>
            <span class="font-semibold text-gray-800 dark:text-gray-100">{{ metrics.fat }}g</span>
          </div>
          <div class="w-full bg-gray-200 dark:bg-gray-700 rounded-full h-2">
            <div
              class="bg-purple-500 h-2 rounded-full transition-all"
              :style="{ width: `${getProgressPercentage(metrics.fat, 70)}%` }"
            ></div>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="text-center text-gray-500 dark:text-gray-400 py-4">
      <p class="text-sm">Keine Daten verfügbar</p>
    </div>
  </div>
</template>
