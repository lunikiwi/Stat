<script setup lang="ts">
import type { HealthMetrics } from '../types';

defineProps<{
  metrics: HealthMetrics | null;
}>();
</script>

<template>
  <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-md p-6">
    <h2 class="text-lg font-semibold mb-4 text-gray-800 dark:text-gray-100">Health</h2>

    <div v-if="metrics" class="grid grid-cols-2 gap-4">
      <div class="text-center">
        <div class="text-3xl font-bold text-blue-600 dark:text-blue-400">
          {{ metrics.sleepScore }}
        </div>
        <div class="text-sm text-gray-600 dark:text-gray-400 mt-1">Sleep Score</div>
      </div>

      <div class="text-center">
        <div class="text-3xl font-bold text-green-600 dark:text-green-400">
          {{ metrics.bodyBattery }}
        </div>
        <div class="text-sm text-gray-600 dark:text-gray-400 mt-1">Body Battery</div>
      </div>
    </div>

    <div v-else class="text-center text-gray-500 dark:text-gray-400 py-4">
      <p class="text-sm">Keine Daten verfügbar</p>
    </div>
  </div>
</template>
