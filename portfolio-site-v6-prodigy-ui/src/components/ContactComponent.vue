<template>
  <div class="mx-auto w-full max-w-4xl px-4 py-12">
    <div class="mb-6 text-center">
      <h1 class="font-salsa mb-3 text-4xl md:text-5xl">Get In Touch</h1>
      <p class="font-kalam text-ink-muted text-lg md:text-xl">
        Have a question or want to work together? Drop me a message!
      </p>
    </div>

    <form
      class="border-ink-muted/20 bg-surface-muted/70 font-salsa flex flex-col gap-8 rounded-2xl border p-8 shadow-xl backdrop-blur-md"
      @submit.prevent="handleSubmit"
    >
      <label class="flex items-center gap-2 text-base">
        <input
          v-model="postAnonymously"
          type="checkbox"
          class="accent-(--color-primary) h-4 w-4 rounded"
        />
        Send anonymously (name will be hidden)
      </label>

      <div v-if="!postAnonymously">
        <label class="mb-2 flex items-center gap-2 text-base font-semibold" for="name">
          <FontAwesomeIcon :icon="faUser" class="size-4" aria-hidden="true" />
          Name *
        </label>
        <input
          id="name"
          v-model="form.name"
          type="text"
          required
          maxlength="100"
          placeholder="Your name"
          class="border-ink-muted/30 bg-surface focus:border-(--color-primary) font-salsa w-full rounded border p-3 text-base placeholder:text-ink-muted/70 focus:outline-none"
        />
      </div>

      <div>
        <label class="mb-2 flex items-center gap-2 text-base font-semibold" for="email">
          <FontAwesomeIcon :icon="faEnvelope" class="size-4" aria-hidden="true" />
          Email *
        </label>
        <input
          id="email"
          v-model="form.email"
          type="email"
          required
          maxlength="254"
          placeholder="your@email.com"
          :class="emailError ? 'border-danger' : 'border-ink-muted/30 focus:border-(--color-primary)'"
          class="bg-surface font-salsa w-full rounded border p-3 text-base placeholder:text-ink-muted/70 focus:outline-none"
          @input="emailError = null"
        />
        <p v-if="emailError" class="text-danger mt-1 text-sm">{{ emailError }}</p>
      </div>

      <div>
        <label class="mb-2 flex items-center gap-2 text-base font-semibold" for="subject">
          <FontAwesomeIcon :icon="faTag" class="size-4" aria-hidden="true" />
          Subject (Optional)
        </label>
        <input
          id="subject"
          v-model="form.subject"
          type="text"
          maxlength="150"
          placeholder="What's this about?"
          class="border-ink-muted/30 bg-surface focus:border-(--color-primary) font-salsa w-full rounded border p-3 text-base placeholder:text-ink-muted/70 focus:outline-none"
        />
      </div>

      <div>
        <label class="mb-2 flex items-center gap-2 text-base font-semibold" for="message">
          <FontAwesomeIcon :icon="faMessage" class="size-4" aria-hidden="true" />
          Message *
        </label>
        <textarea
          id="message"
          v-model="form.message"
          required
          rows="8"
          maxlength="5000"
          placeholder="Your message..."
          class="border-ink-muted/30 bg-surface focus:border-(--color-primary) font-salsa w-full rounded border p-3 text-base placeholder:text-ink-muted/70 focus:outline-none"
        />
      </div>

      <button
        type="submit"
        class="bg-secondary text-secondary-contrast font-salsa group flex w-full items-center justify-center gap-2 rounded-lg px-8 py-3 transition hover:opacity-90"
      >
        <FontAwesomeIcon
          :icon="faPaperPlane"
          class="size-4 transition-transform group-hover:translate-x-1"
          aria-hidden="true"
        />
        Send Message
      </button>

      <p v-if="submitted" class="text-secondary -mt-4 text-center text-sm">
        Thanks! Your message has been noted.
      </p>
    </form>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome'
import {
  faEnvelope,
  faMessage,
  faPaperPlane,
  faTag,
  faUser,
} from '@fortawesome/free-solid-svg-icons'

// Practical RFC 5322-ish email pattern (not the full spec, but rejects obvious garbage).
const EMAIL_REGEX = /^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?)+$/

const form = reactive({
  name: '',
  email: '',
  subject: '',
  message: '',
})
const postAnonymously = ref(false)
const submitted = ref(false)
const emailError = ref<string | null>(null)

function handleSubmit() {
  submitted.value = false

  const name = form.name.trim()
  const email = form.email.trim()
  const subject = form.subject.trim()
  const message = form.message.trim()

  if (!EMAIL_REGEX.test(email)) {
    emailError.value = 'Enter a valid email address.'
    return
  }
  emailError.value = null

  const payload = {
    name: postAnonymously.value ? undefined : name || undefined,
    email,
    subject: subject || undefined,
    message,
  }
  console.log('Contact form submitted:', payload)

  submitted.value = true
  Object.assign(form, { name: '', email: '', subject: '', message: '' })
  postAnonymously.value = false
}
</script>
