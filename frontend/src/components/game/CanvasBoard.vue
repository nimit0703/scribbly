<!-- src/components/game/CanvasBoard.vue -->
<template>
    <div class="flex flex-col h-full bg-primary-50 rounded-xl overflow-hidden">
        <!-- Header with tools -->
        <div v-if="isDrawer" class="bg-white border-b border-primary-200 p-4">
            <div class="flex flex-wrap items-center justify-between gap-4">
                <!-- Drawing Tools -->
                <div class="flex items-center gap-4">
                    <!-- Brush Size -->
                    <div class="flex items-center gap-2">
                        <label class="text-sm font-medium text-primary-700">Size:</label>
                        <div class="flex items-center gap-1">
                            <button v-for="size in brushSizes" :key="size.value" @click="setBrushSize(size.value)"
                                :class="brushSize === size.value ? 'bg-primary-700 text-white' : 'bg-primary-100 text-primary-700 hover:bg-primary-200'"
                                class="w-8 h-8 rounded-lg transition-colors duration-200 flex items-center justify-center text-xs font-medium">
                                {{ size.label }}
                            </button>
                        </div>
                    </div>

                    <!-- Color Palette -->
                    <div class="flex items-center gap-2">
                        <label class="text-sm font-medium text-primary-700">Color:</label>
                        <div class="flex gap-1.5">
                            <button v-for="color in colors" :key="color" @click="setColor(color)"
                                :class="currentColor === color ? 'ring-2 ring-primary-900 ring-offset-1' : 'hover:ring-2 hover:ring-primary-300'"
                                :style="{ backgroundColor: color }"
                                class="w-7 h-7 rounded-lg border-2 border-primary-200 transition-all duration-200"></button>
                        </div>
                    </div>
                </div>

                <!-- Action Buttons -->
                <div class="flex items-center gap-2">
                    <BaseButton variant="secondary" size="sm" @click="toggleEraser">
                        {{ isEraser ? 'Draw' : 'Eraser' }}
                    </BaseButton>
                    <BaseButton variant="danger" size="sm" @click="clearCanvas">
                        Clear
                    </BaseButton>
                </div>
            </div>
        </div>

        <!-- Canvas Container -->
        <div class="flex-1 p-4">
            <div class="relative h-full bg-white rounded-lg border-2 border-primary-200 overflow-hidden">
                <!-- Canvas -->
                <canvas ref="canvas" class="w-full h-full touch-none"
                    :class="isDrawer ? (isEraser ? 'cursor-crosshair' : 'cursor-crosshair') : 'cursor-default'"
                    @mousedown="handleMouseDown" @mouseup="handleMouseUp" @mousemove="handleMouseMove"
                    @mouseleave="handleMouseUp" @touchstart="handleTouchStart" @touchend="handleTouchEnd"
                    @touchmove="handleTouchMove"></canvas>

                <!-- Connection Status -->
                <div class="absolute top-3 right-3">
                    <div :class="connectionStatus === 'connected' ? 'bg-green-500' : 'bg-red-500'"
                        class="w-3 h-3 rounded-full shadow-sm"
                        :title="connectionStatus === 'connected' ? 'Connected' : 'Disconnected'"></div>
                </div>

                <!-- Current Brush Preview -->
                <div v-if="isDrawer"
                    class="absolute bottom-3 left-3 bg-white rounded-lg shadow-lg p-2.5 border border-primary-200">
                    <div class="flex items-center gap-2.5">
                        <div :style="{
                            backgroundColor: isEraser ? '#ffffff' : currentColor,
                            width: Math.max(brushSize, 8) + 'px',
                            height: Math.max(brushSize, 8) + 'px',
                            border: isEraser ? '2px solid #cbd5e1' : '1px solid #e2e8f0'
                        }" class="rounded-full"></div>
                        <span class="text-xs text-primary-700 font-medium">
                            {{ isEraser ? 'Eraser' : 'Brush' }} ({{ brushSize }}px)
                        </span>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch, computed } from 'vue';
import BaseButton from '../ui/BaseButton.vue';

const props = defineProps({
    client: Object,
    roomId: String,
    username: String,
    isDrawer: Boolean
});

const canvas = ref(null);
let ctx;
let isDrawing = false;
let lastX = ref(0);
let lastY = ref(0);
const remoteLastPositions = ref({});
const remoteDrawingStates = ref({});

// Drawing state
const currentColor = ref('#000000');
const brushSize = ref(5);
const isEraser = ref(false);

// Brush sizes configuration
const brushSizes = [
    { value: 2, label: 'S' },
    { value: 5, label: 'M' },
    { value: 10, label: 'L' }
];

// Available colors - more muted palette
const colors = [
    '#0f172a', // Dark grey
    '#475569', // Medium grey
    '#94a3b8', // Light grey
    '#ffffff', // White
    '#ef4444', // Red
    '#f97316', // Orange
    '#eab308', // Yellow
    '#22c55e', // Green
    '#06b6d4', // Cyan
    '#3b82f6', // Blue
    '#8b5cf6', // Purple
    '#ec4899'  // Pink
];

// Connection status
const connectionStatus = computed(() => {
    return props.client && props.client.connected ? 'connected' : 'disconnected';
});

// Drawing functions
const draw = (x, y, color = '#000000', width = 3, erase = false) => {
    const clampedX = Math.max(0, Math.min(x, canvas.value.width));
    const clampedY = Math.max(0, Math.min(y, canvas.value.height));

    ctx.lineWidth = width;
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';

    if (erase) {
        ctx.globalCompositeOperation = 'destination-out';
    } else {
        ctx.globalCompositeOperation = 'source-over';
        ctx.strokeStyle = color;
    }

    ctx.lineTo(clampedX, clampedY);
    ctx.stroke();

    lastX.value = clampedX;
    lastY.value = clampedY;
};

const getMousePos = (e) => {
    const rect = canvas.value.getBoundingClientRect();
    const scaleX = canvas.value.width / rect.width;
    const scaleY = canvas.value.height / rect.height;

    return {
        x: (e.clientX - rect.left) * scaleX,
        y: (e.clientY - rect.top) * scaleY
    };
};

const getTouchPos = (e) => {
    const rect = canvas.value.getBoundingClientRect();
    const scaleX = canvas.value.width / rect.width;
    const scaleY = canvas.value.height / rect.height;

    return {
        x: (e.touches[0].clientX - rect.left) * scaleX,
        y: (e.touches[0].clientY - rect.top) * scaleY
    };
};

// Tool functions
const setColor = (color) => {
    currentColor.value = color;
    isEraser.value = false;
};

const setBrushSize = (size) => {
    brushSize.value = size;
};

const toggleEraser = () => {
    isEraser.value = !isEraser.value;
};

const clearCanvas = () => {
    if (!props.isDrawer) return;

    ctx.clearRect(0, 0, canvas.value.width, canvas.value.height);

    if (props.client && props.client.connected) {
        props.client.publish({
            destination: '/app/clear',
            body: JSON.stringify({
                roomId: props.roomId,
                username: props.username,
            }),
        });
    }
};

// Mouse event handlers
const handleMouseDown = (e) => {
    if (!props.isDrawer) return;

    isDrawing = true;
    const pos = getMousePos(e);
    lastX.value = pos.x;
    lastY.value = pos.y;

    ctx.beginPath();
    ctx.moveTo(pos.x, pos.y);

    if (props.client && props.client.connected) {
        props.client.publish({
            destination: '/app/draw-start',
            body: JSON.stringify({
                roomId: props.roomId,
                username: props.username,
                x: lastX.value,
                y: lastY.value,
            }),
        });
    }
};

const handleMouseUp = () => {
    if (!isDrawing) return;
    isDrawing = false;

    if (props.client && props.client.connected) {
        props.client.publish({
            destination: '/app/draw-end',
            body: JSON.stringify({
                roomId: props.roomId,
                username: props.username,
            }),
        });
    }
};

const handleMouseMove = (e) => {
    if (!isDrawing || !props.isDrawer) return;

    const pos = getMousePos(e);
    draw(pos.x, pos.y, currentColor.value, brushSize.value, isEraser.value);

    if (props.client && props.client.connected) {
        props.client.publish({
            destination: '/app/draw',
            body: JSON.stringify({
                roomId: props.roomId,
                username: props.username,
                x: pos.x,
                y: pos.y,
                lastX: lastX.value,
                lastY: lastY.value,
                color: currentColor.value,
                width: brushSize.value,
                isEraser: isEraser.value,
            }),
        });
    }
};

const handleTouchStart = (e) => {
    if (!props.isDrawer) return;

    e.preventDefault();
    const touch = getTouchPos(e);
    lastX.value = touch.x;
    lastY.value = touch.y;
    isDrawing = true;

    ctx.beginPath();
    ctx.moveTo(touch.x, touch.y);

    if (props.client && props.client.connected) {
        props.client.publish({
            destination: '/app/draw-start',
            body: JSON.stringify({
                roomId: props.roomId,
                username: props.username,
                x: lastX.value,
                y: lastY.value,
            }),
        });
    }
};

const handleTouchEnd = (e) => {
    if (!props.isDrawer) return;

    e.preventDefault();
    isDrawing = false;

    if (props.client && props.client.connected) {
        props.client.publish({
            destination: '/app/draw-end',
            body: JSON.stringify({
                roomId: props.roomId,
                username: props.username,
            }),
        });
    }
};

const handleTouchMove = (e) => {
    e.preventDefault();
    if (!isDrawing || !props.isDrawer) return;

    const touch = getTouchPos(e);
    draw(touch.x, touch.y, currentColor.value, brushSize.value, isEraser.value);

    if (props.client && props.client.connected) {
        props.client.publish({
            destination: '/app/draw',
            body: JSON.stringify({
                roomId: props.roomId,
                username: props.username,
                x: touch.x,
                y: touch.y,
                lastX: lastX.value,
                lastY: lastY.value,
                color: currentColor.value,
                width: brushSize.value,
                isEraser: isEraser.value,
            }),
        });
    }
};

// Remote drawing handlers
const handleRemoteDraw = (data) => {
    const msg = JSON.parse(data.body);

    if (msg.username === props.username) return;

    const lastPos = remoteLastPositions.value[msg.username];
    if (!lastPos) {
        remoteLastPositions.value[msg.username] = { x: msg.x, y: msg.y };
        return;
    }

    ctx.lineWidth = msg.width;
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';

    if (msg.isEraser) {
        ctx.globalCompositeOperation = 'destination-out';
    } else {
        ctx.globalCompositeOperation = 'source-over';
        ctx.strokeStyle = msg.color;
    }

    ctx.lineTo(msg.x, msg.y);
    ctx.stroke();

    remoteLastPositions.value[msg.username] = { x: msg.x, y: msg.y };
};

const handleRemoteClear = () => {
    ctx.clearRect(0, 0, canvas.value.width, canvas.value.height);
    remoteLastPositions.value = {};
    remoteDrawingStates.value = {};
};

const handleRemoteDrawStart = (data) => {
    const msg = JSON.parse(data.body);

    if (msg.username === props.username) return;

    remoteDrawingStates.value[msg.username] = true;
    remoteLastPositions.value[msg.username] = { x: msg.x, y: msg.y };

    ctx.beginPath();
    ctx.moveTo(msg.x, msg.y);
};

const handleRemoteDrawEnd = (data) => {
    const msg = JSON.parse(data.body);
    if (msg.username !== props.username) {
        remoteDrawingStates.value[msg.username] = false;
    }
};

// Resize handler
const resizeCanvas = () => {
    const c = canvas.value;
    const rect = c.getBoundingClientRect();

    if (c.width === rect.width && c.height === rect.height) return;

    c.width = rect.width;
    c.height = rect.height;

    remoteLastPositions.value = {};
    remoteDrawingStates.value = {};
};

// Lifecycle
onMounted(() => {
    const c = canvas.value;
    ctx = c.getContext('2d');

    resizeCanvas();

    const resizeObserver = new ResizeObserver(resizeCanvas);
    resizeObserver.observe(c.parentElement);

    if (props.client && props.client.connected) {
        props.client.subscribe(`/topic/draw-start/${props.roomId}`, handleRemoteDrawStart);
        props.client.subscribe(`/topic/draw/${props.roomId}`, handleRemoteDraw);
        props.client.subscribe(`/topic/clear/${props.roomId}`, handleRemoteClear);
        props.client.subscribe(`/topic/timer-ended/${props.roomId}`, handleRemoteClear);
        props.client.subscribe(`/topic/draw-end/${props.roomId}`, handleRemoteDrawEnd);
    }
});

watch(() => props.client, (newClient) => {
    if (newClient && newClient.connected) {
        newClient.subscribe(`/topic/draw-start/${props.roomId}`, handleRemoteDrawStart);
        newClient.subscribe(`/topic/draw/${props.roomId}`, handleRemoteDraw);
        newClient.subscribe(`/topic/draw-end/${props.roomId}`, handleRemoteDrawEnd);
        newClient.subscribe(`/topic/clear/${props.roomId}`, handleRemoteClear);
        newClient.subscribe(`/topic/timer-ended/${props.roomId}`, handleRemoteClear);
    }
});

onBeforeUnmount(() => {
    // Cleanup handled by Vue
});
</script>