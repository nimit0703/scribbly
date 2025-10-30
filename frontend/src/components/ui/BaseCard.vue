<!-- src/components/ui/BaseCard.vue -->
<template>
    <div class="bg-white rounded-xl shadow-sm border border-primary-200 overflow-hidden">
        <div v-if="header || $slots.header" class="bg-primary-700 px-4 py-3 border-b border-primary-600">
            <slot name="header">
                <h3 class="text-sm font-semibold text-white flex items-center gap-2">
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