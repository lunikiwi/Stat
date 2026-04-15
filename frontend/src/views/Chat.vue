<script setup lang="ts">
import { ref, computed, nextTick, onMounted } from 'vue';
import { useAppStore } from '../stores/appStore';
import { sendChatMessage, ApiError } from '../services/api';
import ChatMessageComponent from '../components/ChatMessage.vue';
import type { ChatMessage } from '../types';

const store = useAppStore();
const messageInput = ref('');
const messagesContainer = ref<HTMLElement | null>(null);

const chatHistory = computed(() => store.chatHistory);
const isLoading = computed(() => store.isLoading);
const error = computed(() => store.error);

const scrollToBottom = async () => {
  await nextTick();
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight;
  }
};

const handleSubmit = async () => {
  const message = messageInput.value.trim();
  if (!message || isLoading.value) return;

  // Add user message to chat
  const userMessage: ChatMessage = {
    role: 'user',
    content: message,
  };
  store.addChatMessage(userMessage);
  messageInput.value = '';

  await scrollToBottom();

  // Send to API
  store.setLoading(true);
  store.clearError();

  try {
    const response = await sendChatMessage({
      currentMessage: message,
      chatHistory: chatHistory.value.slice(0, -1), // Exclude the message we just added
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
      <form @submit.prevent="handleSubmit" class="flex gap-2">
        <input
          v-model="messageInput"
          type="text"
          placeholder="Nachricht eingeben..."
          class="flex-1 px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-full focus:outline-none focus:ring-2 focus:ring-blue-500 dark:bg-gray-700 dark:text-white"
          :disabled="isLoading"
        />
        <button
          type="submit"
          :disabled="!messageInput.trim() || isLoading"
          class="px-6 py-2 bg-blue-600 hover:bg-blue-700 disabled:bg-gray-400 text-white rounded-full font-medium transition-colors"
        >
          Senden
        </button>
      </form>
    </div>
  </div>
</template>
