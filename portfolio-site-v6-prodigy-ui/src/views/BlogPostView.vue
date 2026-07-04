<script setup lang="ts">
import { useRoute } from 'vue-router'
import BlogPostComponent from '../components/BlogPostComponent.vue'

const route = useRoute()
</script>

<template>
  <!-- Keyed on slug so navigating between two posts is a full remount, not a
  reactive param update - BlogPostComponent's reactions/comments/document-meta
  state is read once at setup and needs a fresh instance per post. -->
  <BlogPostComponent :key="route.params.slug as string" />
</template>
