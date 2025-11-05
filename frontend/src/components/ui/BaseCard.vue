<!-- src/components/ui/BaseCard.vue -->
<template>
    <div class="bg-notion-gray-darker rounded-lg border border-notion-border overflow-hidden">
        <div v-if="header || $slots.header" class="bg-notion-gray-dark px-4 py-3 border-b border-notion-border">
            <slot name="header">
                <h3 class="text-sm font-semibold text-notion-text-primary flex items-center gap-2">
                    <slot name="header-icon" />
                    {{ header }}
                </h3>
            </slot>
        </div>
        <div :class="noPadding ? '' : 'p-4'">
            <slot />
        </div>
    </div>
</template>

<script setup>
defineProps({
    header: String,
    noPadding: {
        type: Boolean,
        default: false
    }
});
</script>