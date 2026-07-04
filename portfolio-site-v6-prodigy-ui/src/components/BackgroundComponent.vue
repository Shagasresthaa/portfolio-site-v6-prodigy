<template>
  <div
    class="pointer-events-none fixed top-16 right-8 bottom-8 left-8 -z-10 flex items-stretch overflow-hidden opacity-10 md:top-32 md:right-16 md:bottom-16 md:left-16 dark:invert"
  >
    <!-- Left side with telescope and laptop - hidden on mobile -->
    <div class="hidden flex-1 flex-col md:flex">
      <!-- Telescope - top half -->
      <div class="flex-1 bg-[url('/assets/telescope.svg')] bg-contain bg-left bg-no-repeat" />

      <!-- Laptop - bottom half with rotation -->
      <div
        class="origin-center flex-1 -rotate-18 scale-[0.65] bg-[url('/assets/laptop.svg')] bg-contain bg-right bg-no-repeat"
      />
    </div>

    <!-- Right side with astronaut - visible on all screens -->
    <div class="flex-1 bg-[url('/assets/astronaut.svg')] bg-contain bg-center bg-no-repeat" />
  </div>
</template>
