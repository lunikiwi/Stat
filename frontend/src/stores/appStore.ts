import { defineStore } from 'pinia';
import type { ChatMessage, HealthMetrics, NutritionMetrics, TrainingMetrics, MetricsUsed } from '../types';

interface AppState {
  chatHistory: ChatMessage[];
  healthMetrics: HealthMetrics | null;
  nutritionMetrics: NutritionMetrics | null;
  trainingMetrics: TrainingMetrics | null;
  latestMetricsUsed: MetricsUsed | null;
  isLoading: boolean;
  error: string | null;
}

export const useAppStore = defineStore('app', {
  state: (): AppState => ({
    chatHistory: [],
    healthMetrics: null,
    nutritionMetrics: null,
    trainingMetrics: null,
    latestMetricsUsed: null,
    isLoading: false,
    error: null,
  }),

  actions: {
    addChatMessage(message: ChatMessage) {
      this.chatHistory.push(message);
    },

    updateHealthMetrics(metrics: HealthMetrics) {
      this.healthMetrics = metrics;
    },

    updateNutritionMetrics(metrics: NutritionMetrics) {
      this.nutritionMetrics = metrics;
    },

    updateTrainingMetrics(metrics: TrainingMetrics) {
      this.trainingMetrics = metrics;
    },

    updateLatestMetricsUsed(metrics: MetricsUsed) {
      this.latestMetricsUsed = metrics;
    },

    setLoading(loading: boolean) {
      this.isLoading = loading;
    },

    setError(error: string | null) {
      this.error = error;
    },

    clearError() {
      this.error = null;
    },
  },
});
