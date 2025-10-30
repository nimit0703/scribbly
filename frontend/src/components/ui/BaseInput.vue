<!-- src/components/ui/BaseInput.vue -->
<template>
  <div class="space-y-1.5">
    <label v-if="label" :for="id" class="block text-sm font-medium text-primary-700">
      {{ label }}
    </label>
    <div class="relative">
      <div v-if="$slots.icon" class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-primary-400">
        <slot name="icon" />
      </div>
      <input
        :id="id"
        :type="type"
        :value="modelValue"
        :placeholder="placeholder"
        :disabled="disabled"
        :class="inputClasses"
        @input="$emit('update:modelValue', $event.target.value)"
      />
    </div>
    <p v-if="error" class="text-sm text-red-600 flex items-center gap-1">
      <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
      </svg>
      {{ error }}
    </p>
  </div>
</template>

<script setup>
import { computed } from 'vue';

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
  disabled: Boolean
});

defineEmits(['update:modelValue']);

const inputClasses = computed(() => {
  const hasIcon = !!props.$slots?.icon;
  const baseClasses = 'w-full pr-3 py-2.5 bg-primary-50 border rounded-lg focus:outline-none focus:ring-2 focus:border-transparent transition-all text-primary-900 placeholder-primary-400';
  const iconClasses = hasIcon ? 'pl-10' : 'pl-3';
  const errorClasses = props.error ? 'border-red-300 focus:ring-red-500' : 'border-primary-200 focus:ring-primary-500';
  
  return `${baseClasses} ${iconClasses} ${errorClasses}`;
});
</script>