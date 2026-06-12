<template>
  <div id="app-container">
    <router-view />
    <!-- 全局智能客服悬浮窗 -->
    <Chatbot />
  </div>
</template>

<script setup>
import Chatbot from './components/Chatbot.vue'
</script>

<style>
#app-container {
  min-height: 100vh;
  position: relative;
}
</style>
