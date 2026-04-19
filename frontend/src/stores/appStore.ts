import { defineStore } from 'pinia';
import type { ChatMessage, HealthMetrics, NutritionMetrics, TrainingMetrics, MetricsUsed, NutritionLogRequest } from '../types';
import { fetchMetrics, logNutrition } from '../services/api';

interface AppState {
  chatHistory: ChatMessage[];
  healthMetrics: HealthMetrics | null;
  nutritionMetrics: NutritionMetrics | null;
  trainingMetrics: TrainingMetrics | null;
  latestMetricsUsed: MetricsUsed | null;
  isLoading: boolean;
  error: string | null;
  successMessage: string | null;
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
    successMessage: null,
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

    setSuccessMessage(message: string | null) {
      this.successMessage = message;
    },

    clearSuccessMessage() {
      this.successMessage = null;
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

    async addNutritionLog(request: NutritionLogRequest) {
      this.clearError();
      this.clearSuccessMessage();

      try {
        await logNutrition(request);
        this.setSuccessMessage('Mahlzeit erfolgreich gespeichert!');

        // Reload metrics to reflect the new nutrition data
        await this.loadMetrics();
      } catch (error) {
        const errorMessage = error instanceof Error ? error.message : 'Failed to log nutrition';
        this.setError(errorMessage);
        throw error;
      }
    },
  },
});
