<script setup lang="ts">
import { computed } from 'vue';
import { marked } from 'marked';
import type { ChatMessage } from '../types';

const props = defineProps<{
  message: ChatMessage;
}>();

const isUser = computed(() => props.message.role === 'user');

const renderedContent = computed(() => {
  if (isUser.value) {
    return props.message.content;
  }
  // Render markdown for assistant messages
  return marked(props.message.content, { breaks: true });
});
</script>

<template>
  <div :class="['flex', isUser ? 'justify-end' : 'justify-start']">
    <div
      :class="[
        'rounded-2xl px-4 py-3 max-w-[80%]',
        isUser
          ? 'bg-blue-600 text-white'
          : 'bg-gray-200 dark:bg-gray-700 text-gray-800 dark:text-gray-100',
      ]"
    >
      <div v-if="isUser" class="whitespace-pre-wrap">{{ message.content }}</div>
      <div
        v-else
        class="markdown-content prose prose-sm max-w-none dark:prose-invert"
        v-html="renderedContent"
      ></div>
    </div>
  </div>
</template>
