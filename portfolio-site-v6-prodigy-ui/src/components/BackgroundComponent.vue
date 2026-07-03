<template>
  <div
    class="pointer-events-none absolute inset-4 -z-10 flex items-stretch overflow-hidden opacity-10 md:inset-10 dark:invert"
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
