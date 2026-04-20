<script setup lang="ts">
import { ref, computed, nextTick, onMounted } from 'vue';
import { useAppStore } from '../stores/appStore';
import { sendChatMessage, ApiError } from '../services/api';
import ChatMessageComponent from '../components/ChatMessage.vue';
import type { ChatMessage } from '../types';

const store = useAppStore();
const messageInput = ref('');
const messagesContainer = ref<HTMLElement | null>(null);
const fileInput = ref<HTMLInputElement | null>(null);
const selectedImage = ref<string | null>(null);
const imagePreviewUrl = ref<string | null>(null);

const chatHistory = computed(() => store.chatHistory);
const isLoading = computed(() => store.isLoading);
const error = computed(() => store.error);

const scrollToBottom = async () => {
  await nextTick();
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight;
  }
};

const fileToBase64 = (file: File): Promise<string> => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => {
      if (typeof reader.result === 'string') {
        // Remove the data URL prefix (e.g., "data:image/jpeg;base64,")
        const base64 = reader.result.split(',')[1];
        resolve(base64);
      } else {
        reject(new Error('Failed to read file as base64'));
      }
    };
    reader.onerror = () => reject(reader.error);
    reader.readAsDataURL(file);
  });
};

const handleImageSelect = async (event: Event) => {
  const target = event.target as HTMLInputElement;
  const file = target.files?.[0];

  if (!file) return;

  // Validate file type
  if (!file.type.startsWith('image/')) {
    store.setError('Bitte wähle eine Bilddatei aus');
    return;
  }

  // Validate file size (max 5MB)
  if (file.size > 5 * 1024 * 1024) {
    store.setError('Bild ist zu groß. Maximal 5MB erlaubt.');
    return;
  }

  try {
    const base64 = await fileToBase64(file);
    selectedImage.value = base64;
    imagePreviewUrl.value = URL.createObjectURL(file);
  } catch (err) {
    store.setError('Fehler beim Laden des Bildes');
  }
};

const removeImage = () => {
  selectedImage.value = null;
  if (imagePreviewUrl.value) {
    URL.revokeObjectURL(imagePreviewUrl.value);
    imagePreviewUrl.value = null;
  }
  if (fileInput.value) {
    fileInput.value.value = '';
  }
};

const triggerFileInput = () => {
  fileInput.value?.click();
};

const handleSubmit = async () => {
  const message = messageInput.value.trim();
  if ((!message && !selectedImage.value) || isLoading.value) return;

  // Add user message to chat
  const userMessage: ChatMessage = {
    role: 'user',
    content: message || '📷 Bild gesendet',
  };
  store.addChatMessage(userMessage);
  messageInput.value = '';

  const imageToSend = selectedImage.value;
  removeImage();

  await scrollToBottom();

  // Send to API
  store.setLoading(true);
  store.clearError();

  try {
    const response = await sendChatMessage({
      currentMessage: message || 'Was ist das für eine Mahlzeit?',
      chatHistory: chatHistory.value.slice(0, -1), // Exclude the message we just added
      imageBase64: imageToSend,
    });

    // Add assistant response
    const assistantMessage: ChatMessage = {
      role: 'assistant',
      content: response.reply,
    };
    store.addChatMessage(assistantMessage);
    store.updateLatestMetricsUsed(response.metricsUsed);

    await scrollToBottom();
  } catch (err) {
    if (err instanceof ApiError) {
      store.setError(err.message);
    } else {
      store.setError('An unexpected error occurred');
    }
  } finally {
    store.setLoading(false);
  }
};

onMounted(() => {
  scrollToBottom();
});
</script>

<template>
  <div class="flex-1 flex flex-col pb-16">
    <!-- Messages Area -->
    <div
      ref="messagesContainer"
      class="flex-1 overflow-y-auto px-4 pt-6 pb-4 space-y-4"
    >
      <div v-if="chatHistory.length === 0" class="text-center text-gray-500 mt-8">
        <p class="text-lg">👋 Hallo! Ich bin dein AI Coach.</p>
        <p class="text-sm mt-2">Stelle mir eine Frage zu deiner Gesundheit, Ernährung oder Training.</p>
      </div>

      <ChatMessageComponent
        v-for="(message, index) in chatHistory"
        :key="index"
        :message="message"
      />

      <div v-if="isLoading" class="flex justify-start">
        <div class="bg-gray-200 dark:bg-gray-700 rounded-2xl px-4 py-3 max-w-[80%]">
          <div class="flex space-x-2">
            <div class="w-2 h-2 bg-gray-500 rounded-full animate-bounce" style="animation-delay: 0ms"></div>
            <div class="w-2 h-2 bg-gray-500 rounded-full animate-bounce" style="animation-delay: 150ms"></div>
            <div class="w-2 h-2 bg-gray-500 rounded-full animate-bounce" style="animation-delay: 300ms"></div>
          </div>
        </div>
      </div>

      <div v-if="error" class="bg-red-100 dark:bg-red-900 border border-red-400 dark:border-red-700 text-red-700 dark:text-red-200 px-4 py-3 rounded-lg">
        <p class="font-bold">Error</p>
        <p class="text-sm">{{ error }}</p>
      </div>
    </div>

    <!-- Input Area -->
    <div class="border-t border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 px-4 py-3">
      <!-- Image Preview -->
      <div v-if="imagePreviewUrl" class="mb-3 relative inline-block">
        <img
          :src="imagePreviewUrl"
          alt="Preview"
          class="h-20 w-20 object-cover rounded-lg border-2 border-blue-500"
        />
        <button
          @click="removeImage"
          type="button"
          class="absolute -top-2 -right-2 bg-red-500 hover:bg-red-600 text-white rounded-full w-6 h-6 flex items-center justify-center text-sm font-bold"
          :disabled="isLoading"
        >
          ×
        </button>
      </div>

      <form @submit.prevent="handleSubmit" class="flex gap-2">
        <!-- Hidden file input -->
        <input
          ref="fileInput"
          type="file"
          accept="image/*"
          @change="handleImageSelect"
          class="hidden"
        />

        <!-- Image upload button -->
        <button
          type="button"
          @click="triggerFileInput"
          :disabled="isLoading"
          class="px-3 py-2 bg-gray-200 hover:bg-gray-300 dark:bg-gray-700 dark:hover:bg-gray-600 disabled:bg-gray-400 text-gray-700 dark:text-gray-200 rounded-full transition-colors"
          title="Bild hochladen"
        >
          <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
          </svg>
        </button>

        <input
          v-model="messageInput"
          type="text"
          placeholder="Nachricht eingeben..."
          class="flex-1 px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-full focus:outline-none focus:ring-2 focus:ring-blue-500 dark:bg-gray-700 dark:text-white"
          :disabled="isLoading"
        />
        <button
          type="submit"
          :disabled="(!messageInput.trim() && !selectedImage) || isLoading"
          class="px-6 py-2 bg-blue-600 hover:bg-blue-700 disabled:bg-gray-400 text-white rounded-full font-medium transition-colors"
        >
          Senden
        </button>
      </form>
    </div>
  </div>
</template>
