<template>
  <button :class="['default-btn-styles', variantClasses, { 'opacity-75 cursor-not-allowed': disabled }]" :disabled="disabled">
    <!-- Slot for dynamic content like text or icons -->
    <slot>Default Text</slot>
  </button>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  variant: {
    type: String,
    default: 'primary',
    validator: (value) => ['primary', 'secondary', 'danger'].includes(value),
  },
  disabled: {
    type: Boolean,
    default: false,
  },
});

const baseStyles = 'font-bold cursor-pointer py-2 px-4 rounded transition duration-150 ease-in-out';

const variantClasses = computed(() => {
  switch (props.variant) {
    case 'primary':
      return 'px-3 py-1.5 text-sm font-medium bg-text text-bg border border-text rounded-lg shadow-md hover:bg-surface hover:text-text transition';
    case 'secondary':
      return 'bg-gray-500 hover:bg-gray-700 text-white';
    case 'danger':
      return 'bg-red-500 hover:bg-red-700 text-white';
    default:
      return 'bg-blue-500 hover:bg-blue-700 text-white';
  }
});
</script>

<style scoped>
/* You can add extra custom CSS here if needed, or use @apply for utility groups */
</style>
