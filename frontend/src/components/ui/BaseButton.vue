<!-- src/components/ui/BaseButton.vue -->
<template>
    <button :type="type" :disabled="disabled" :class="buttonClasses" @click="$emit('click', $event)">
        <span class="flex items-center justify-center gap-4">
            <slot name="icon-left" />
            <slot />
            <div v-if="$slots['icon-right']"
                class="opacity-0 group-hover:opacity-100 group-focus:opacity-100 transition-opacity duration-200">
                <slot name="icon-right" />
            </div>
        </span>
    </button>
</template>

<script setup>
import { computed, useSlots } from 'vue';

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

const slots = useSlots();

defineEmits(['click']);

const baseClasses = 'inline-flex items-center justify-center font-medium rounded transition-all duration-200 focus:outline-none disabled:opacity-50 disabled:cursor-not-allowed group';

const variantClasses = {
    primary: 'bg-notion-accent hover:bg-notion-accent-hover text-white border border-transparent hover:border-notion-border-hover',
    secondary: 'bg-notion-gray-dark hover:bg-notion-gray-darker text-notion-text-primary border border-transparent hover:border-notion-border',
    danger: 'bg-red-600 hover:bg-red-700 text-white border border-transparent hover:border-red-500',
    ghost: 'bg-transparent hover:bg-notion-gray-dark text-notion-text-primary border border-transparent hover:border-notion-border',
    accent: 'bg-blue-600 hover:bg-blue-700 text-white border border-transparent hover:border-blue-500'
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