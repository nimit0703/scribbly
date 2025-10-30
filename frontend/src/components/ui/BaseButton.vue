<!-- src/components/ui/BaseButton.vue -->
<template>
    <button :type="type" :disabled="disabled" :class="buttonClasses" @click="$emit('click', $event)">
        <slot name="icon-left" />
        <slot />
        <slot name="icon-right" />
    </button>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
    variant: {
        type: String,
        default: 'primary',
        validator: (value) => ['primary', 'secondary', 'danger', 'ghost', 'accent'].includes(value)
    },
    size: {
        type: String,
        default: 'md',
        validator: (value) => ['sm', 'md', 'lg'].includes(value)
    },
    disabled: {
        type: Boolean,
        default: false
    },
    type: {
        type: String,
        default: 'button'
    }
});

defineEmits(['click']);

const baseClasses = 'inline-flex items-center justify-center gap-2 font-medium rounded-lg transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed';

const variantClasses = {
    primary: 'bg-primary-700 hover:bg-primary-800 text-white focus:ring-primary-500 disabled:hover:bg-primary-700',
    secondary: 'bg-primary-200 hover:bg-primary-300 text-primary-900 focus:ring-primary-400',
    danger: 'bg-red-600 hover:bg-red-700 text-white focus:ring-red-500',
    ghost: 'bg-transparent hover:bg-primary-100 text-black focus:ring-primary-400',
    accent: 'bg-accent-600 hover:bg-accent-700 text-white focus:ring-accent-500'
};

const sizeClasses = {
    sm: 'px-3 py-1.5 text-sm',
    md: 'px-4 py-2 text-sm',
    lg: 'px-6 py-3 text-base'
};

const buttonClasses = computed(() => {
    return `${baseClasses} ${variantClasses[props.variant]} ${sizeClasses[props.size]}`;
});
</script>