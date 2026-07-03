<script setup lang="ts">
import { RouterView } from 'vue-router'
import NavbarComponent from './components/NavbarComponent.vue'
import FooterComponent from './components/FooterComponent.vue'
</script>

<template>
  <div class="flex min-h-screen flex-col">
    <NavbarComponent />
    <main class="flex w-full flex-1 justify-center">
      <RouterView />
    </main>
    <FooterComponent />
  </div>
</template>
