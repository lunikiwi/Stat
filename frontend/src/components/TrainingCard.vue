<script setup lang="ts">
import type { TrainingMetrics } from '../types';

defineProps<{
  metrics: TrainingMetrics | null;
}>();
</script>

<template>
  <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-md p-6">
    <h2 class="text-lg font-semibold mb-4 text-gray-800 dark:text-gray-100">Training</h2>

    <div v-if="metrics" class="text-center py-2">
      <div class="text-sm text-gray-600 dark:text-gray-400 mb-2">Nächstes Workout</div>
      <div class="text-lg font-semibold text-gray-800 dark:text-gray-100">
        {{ metrics.nextWorkout }}
      </div>
    </div>

    <div v-else class="text-center text-gray-500 dark:text-gray-400 py-4">
      <p class="text-sm">Kein Workout geplant</p>
    </div>
  </div>
</template>
