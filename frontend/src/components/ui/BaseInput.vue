<!-- src/components/ui/BaseInput.vue -->
<template>
  <label v-if="label" :for="id" class="block text-sm font-medium text-notion-text-secondary">
    {{ label }}
  </label>
  <div class="relative group">
    <div v-if="$slots.icon"
      class="absolute inset-y-0 right-3 flex items-center pointer-events-none text-notion-text-muted opacity-0 group-hover:opacity-100 group-focus-within:opacity-100 transition-opacity duration-200">
      <slot name="icon" />
    </div>
    <input :id="id" :type="type" :value="modelValue" :placeholder="placeholder" :disabled="disabled"
      :class="inputClasses" @input="$emit('update:modelValue', $event.target.value)" />
  </div>
  <p v-if="error" class="text-red-400 text-sm mt-1 flex items-center gap-1">
    <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
        d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
    </svg>
    {{ error }}
  </p>
</template>

<script setup>
import { computed, useSlots } from 'vue';

const props = defineProps({
  id: String,
  label: String,
  modelValue: [String, Number],
  type: {
    type: String,
    default: 'text'
  },
  placeholder: String,
  error: String,
  disabled: Boolean,
});

const slots = useSlots()

defineEmits(['update:modelValue']);

const inputClasses = computed(() => {
  const hasIcon = !!slots.icon;
  const baseClasses = 'w-full bg-transparent border border-transparent rounded px-3 py-2 text-notion-text-primary placeholder-notion-text-muted transition-all duration-200 focus:bg-notion-gray-dark focus:border-notion-border-hover focus:outline-none hover:bg-notion-gray-dark hover:border-notion-border';
  const iconClasses = hasIcon ? 'pr-1' : 'pr-3';
  const errorClasses = props.error ? '!border-red-400' : '';

  return `${baseClasses} ${iconClasses} ${errorClasses}`;
});
</script>