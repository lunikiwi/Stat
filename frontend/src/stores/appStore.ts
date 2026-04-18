import { defineStore } from 'pinia';
import type { ChatMessage, HealthMetrics, NutritionMetrics, TrainingMetrics, MetricsUsed } from '../types';
import { fetchMetrics } from '../services/api';

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

    async loadMetrics() {
      this.setLoading(true);
      this.clearError();

      try {
        const response = await fetchMetrics();

        // Update health metrics
        this.updateHealthMetrics({
          sleepScore: response.health.sleepScore,
          bodyBattery: response.health.bodyBattery,
          trainingLoadMinutes48h: response.health.trainingLoadMinutes48h,
        });

        // Update nutrition metrics with calculated caloriesRemaining
        const targetCalories = 2500; // Default target, could be made configurable
        this.updateNutritionMetrics({
          calories: response.nutrition.calories,
          protein: response.nutrition.protein,
          carbs: response.nutrition.carbs,
          fat: response.nutrition.fat,
          caloriesRemaining: targetCalories - response.nutrition.calories,
        });
      } catch (error) {
        const errorMessage = error instanceof Error ? error.message : 'Failed to load metrics';
        this.setError(errorMessage);
        console.error('Error loading metrics:', error);
      } finally {
        this.setLoading(false);
      }
    },
  },
});
