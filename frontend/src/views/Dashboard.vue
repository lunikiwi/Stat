<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
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
const successMessage = computed(() => store.successMessage);

const showQuickAddModal = ref(false);
const mealDescription = ref('');
const isSubmitting = ref(false);

const openQuickAdd = () => {
  showQuickAddModal.value = true;
  mealDescription.value = '';
  store.clearError();
  store.clearSuccessMessage();
};

const closeQuickAdd = () => {
  showQuickAddModal.value = false;
  mealDescription.value = '';
  isSubmitting.value = false;
};

const submitMeal = async () => {
  if (mealDescription.value.trim() && !isSubmitting.value) {
    isSubmitting.value = true;

    try {
      await store.addNutritionLog({ description: mealDescription.value.trim() });
      closeQuickAdd();
    } catch (error) {
      // Error is already set in the store
      isSubmitting.value = false;
    }
  }
};

// Auto-hide success message after 3 seconds
watch(successMessage, (newMessage) => {
  if (newMessage) {
    setTimeout(() => {
      store.clearSuccessMessage();
    }, 3000);
  }
});

onMounted(() => {
  store.loadMetrics();
});
</script>

<template>
  <div class="flex-1 overflow-y-auto pb-20 px-4 pt-6">
    <h1 class="text-2xl font-bold mb-6 text-gray-800 dark:text-gray-100">Dashboard</h1>

    <!-- Success Message (Toast) -->
    <Transition name="toast">
      <div
        v-if="successMessage"
        class="fixed top-4 left-1/2 -translate-x-1/2 z-50 max-w-md w-full mx-4"
      >
        <div class="bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 rounded-lg p-4 shadow-lg">
          <div class="flex items-start">
            <svg class="w-5 h-5 text-green-600 dark:text-green-400 mt-0.5 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
            </svg>
            <div class="flex-1">
              <p class="text-sm font-medium text-green-800 dark:text-green-200">{{ successMessage }}</p>
            </div>
            <button
              @click="store.clearSuccessMessage()"
              class="ml-3 text-green-600 dark:text-green-400 hover:text-green-800 dark:hover:text-green-200"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- Error Message -->
    <div
      v-if="error"
      class="max-w-2xl mx-auto mb-4 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4"
    >
      <div class="flex items-start">
        <svg class="w-5 h-5 text-red-600 dark:text-red-400 mt-0.5 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        <div class="flex-1">
          <h3 class="text-sm font-medium text-red-800 dark:text-red-200">Fehler beim Laden der Daten</h3>
          <p class="text-sm text-red-700 dark:text-red-300 mt-1">{{ error }}</p>
        </div>
        <button
          @click="store.clearError()"
          class="ml-3 text-red-600 dark:text-red-400 hover:text-red-800 dark:hover:text-red-200"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
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
      class="fixed bottom-24 right-6 w-14 h-14 bg-blue-600 hover:bg-blue-700 active:bg-blue-800 text-white rounded-full shadow-lg flex items-center justify-center transition-colors"
      @click="openQuickAdd"
      aria-label="Mahlzeit hinzufügen"
    >
      <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
      </svg>
    </button>

    <!-- Quick Add Modal -->
    <Teleport to="body">
      <Transition name="modal">
        <div
          v-if="showQuickAddModal"
          class="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/50 backdrop-blur-sm"
          @click.self="closeQuickAdd"
        >
          <div class="bg-white dark:bg-gray-800 rounded-t-3xl sm:rounded-2xl w-full sm:max-w-md p-6 shadow-xl">
            <!-- Header -->
            <div class="flex items-center justify-between mb-4">
              <h2 class="text-xl font-bold text-gray-800 dark:text-gray-100">Mahlzeit hinzufügen</h2>
              <button
                @click="closeQuickAdd"
                class="text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200 transition-colors"
                aria-label="Schließen"
              >
                <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>

            <!-- Input -->
            <div class="mb-6">
              <label for="meal-input" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Was hast du gegessen?
              </label>
              <textarea
                id="meal-input"
                v-model="mealDescription"
                rows="4"
                class="w-full px-4 py-3 border border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 placeholder-gray-400 dark:placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none"
                placeholder="z.B. 200g Hähnchenbrust mit Reis und Gemüse"
                @keydown.enter.meta="submitMeal"
                @keydown.enter.ctrl="submitMeal"
              ></textarea>
              <p class="text-xs text-gray-500 dark:text-gray-400 mt-2">
                Beschreibe deine Mahlzeit so genau wie möglich
              </p>
            </div>

            <!-- Actions -->
            <div class="flex gap-3">
              <button
                @click="closeQuickAdd"
                class="flex-1 px-4 py-3 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 rounded-xl hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors font-medium"
              >
                Abbrechen
              </button>
              <button
                @click="submitMeal"
                :disabled="!mealDescription.trim() || isSubmitting"
                class="flex-1 px-4 py-3 bg-blue-600 hover:bg-blue-700 disabled:bg-gray-300 dark:disabled:bg-gray-600 disabled:cursor-not-allowed text-white rounded-xl transition-colors font-medium flex items-center justify-center gap-2"
              >
                <svg
                  v-if="isSubmitting"
                  class="animate-spin h-5 w-5 text-white"
                  xmlns="http://www.w3.org/2000/svg"
                  fill="none"
                  viewBox="0 0 24 24"
                >
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                <span>{{ isSubmitting ? 'Speichern...' : 'Hinzufügen' }}</span>
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.3s ease;
}

.modal-enter-active .bg-white,
.modal-leave-active .bg-white {
  transition: transform 0.3s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .bg-white,
.modal-leave-from .dark\:bg-gray-800 {
  transform: translateY(100%);
}

.modal-leave-to .bg-white,
.modal-leave-to .dark\:bg-gray-800 {
  transform: translateY(100%);
}

.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s ease;
}

.toast-enter-from {
  opacity: 0;
  transform: translate(-50%, -1rem);
}

.toast-leave-to {
  opacity: 0;
  transform: translate(-50%, -1rem);
}
</style>
