<script setup lang="ts">
import { RouterView } from 'vue-router';
import BottomNav from './components/BottomNav.vue';
</script>

<template>
  <div id="app" class="min-h-screen bg-gray-50 dark:bg-gray-900 flex flex-col">
    <RouterView class="flex-1 flex flex-col" />
    <BottomNav />
  </div>
</template>

<style scoped>
#app {
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}
</style>
