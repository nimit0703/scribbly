<template>
    <div class="flex flex-col h-full bg-gradient-to-br from-slate-50 to-slate-100 rounded-xl shadow-lg overflow-hidden">
        <!-- Header with tools -->
        <div class="bg-white border-b border-slate-200 p-4 shadow-sm">
            <div class="flex flex-wrap items-center justify-between gap-4">
                <!-- Drawing Tools -->
                <div class="flex items-center gap-3">
                    <!-- Brush Size -->
                    <div v-if="isDrawer" class="flex items-center gap-2">
                        <label class="text-sm font-medium text-slate-700">Size:</label>
                        <div class="flex items-center gap-1">
                            <button 
                                @click="setBrushSize(2)"
                                :class="brushSize === 2 ? 'bg-blue-500 text-white' : 'bg-slate-200 text-slate-700 hover:bg-slate-300'"
                                class="w-8 h-8 rounded-full transition-colors duration-200 flex items-center justify-center text-xs font-medium"
                            >
                                S
                            </button>
                            <button 
                                @click="setBrushSize(5)"
                                :class="brushSize === 5 ? 'bg-blue-500 text-white' : 'bg-slate-200 text-slate-700 hover:bg-slate-300'"
                                class="w-8 h-8 rounded-full transition-colors duration-200 flex items-center justify-center text-xs font-medium"
                            >
                                M
                            </button>
                            <button 
                                @click="setBrushSize(10)"
                                :class="brushSize === 10 ? 'bg-blue-500 text-white' : 'bg-slate-200 text-slate-700 hover:bg-slate-300'"
                                class="w-8 h-8 rounded-full transition-colors duration-200 flex items-center justify-center text-xs font-medium"
                            >
                                L
                            </button>
                        </div>
                    </div>

                    <!-- Color Palette -->
                    <div class="flex items-center gap-2">
                        <label class="text-sm font-medium text-slate-700">Color:</label>
                        <div class="flex gap-1">
                            <button 
                                v-for="color in colors" 
                                :key="color"
                                @click="setColor(color)"
                                :class="currentColor === color ? 'ring-2 ring-slate-400 ring-offset-2' : ''"
                                :style="{ backgroundColor: color }"
                                class="w-8 h-8 rounded-full border-2 border-white shadow-sm hover:scale-110 transition-transform duration-200"
                            ></button>
                        </div>
                    </div>
                </div>

                <!-- Action Buttons -->
                <div v-if="isDrawer" class="flex items-center gap-2">
                    <button 
                        @click="clearCanvas"
                        class="px-4 py-2 bg-red-500 hover:bg-red-600 text-white rounded-lg transition-colors duration-200 font-medium text-sm shadow-sm"
                    >
                        Clear
                    </button>
                    <button 
                        @click="toggleEraser"
                        :class="isEraser ? 'bg-orange-500 hover:bg-orange-600' : 'bg-slate-500 hover:bg-slate-600'"
                        class="px-4 py-2 text-white rounded-lg transition-colors duration-200 font-medium text-sm shadow-sm"
                    >
                        {{ isEraser ? 'Draw' : 'Eraser' }}
                    </button>
                </div>
            </div>
        </div>

        <!-- Canvas Container -->
        <div class="flex-1 p-4">
            <div class="relative h-full bg-white rounded-lg shadow-inner border-2 border-slate-200 overflow-hidden">
                <!-- Canvas -->
                <canvas 
                    ref="canvas" 
                    class="w-full h-full cursor-crosshair touch-none"
                    :class="{ 'cursor-grab': isEraser }"
                    @mousedown="handleMouseDown"
                    @mouseup="handleMouseUp"
                    @mousemove="handleMouseMove"
                    @mouseleave="handleMouseUp"
                    @touchstart="handleTouchStart"
                    @touchend="handleTouchEnd"
                    @touchmove="handleTouchMove"
                ></canvas>

                <!-- Connection Status -->
                <div class="absolute top-3 right-3">
                    <div 
                        :class="connectionStatus === 'connected' ? 'bg-green-500' : 'bg-red-500'"
                        class="w-3 h-3 rounded-full shadow-sm"
                        :title="connectionStatus === 'connected' ? 'Connected' : 'Disconnected'"
                    ></div>
                </div>

                <!-- Current Brush Preview -->
                <div class="absolute bottom-3 left-3 bg-white rounded-lg shadow-lg p-2 border border-slate-200">
                    <div class="flex items-center gap-2">
                        <div 
                            :style="{ 
                                backgroundColor: isEraser ? '#ffffff' : currentColor,
                                width: Math.max(brushSize, 8) + 'px',
                                height: Math.max(brushSize, 8) + 'px',
                                border: isEraser ? '2px solid #e2e8f0' : '1px solid #e2e8f0'
                            }"
                            class="rounded-full"
                        ></div>
                        <span class="text-xs text-slate-600 font-medium">
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
const remoteDrawingStates  = ref({})
// Drawing state
const currentColor = ref('#000000');
const brushSize = ref(5);
const isEraser = ref(false);

// Available colors
const colors = [
    '#000000', '#ffffff', '#ef4444', '#f97316', '#eab308', 
    '#22c55e', '#06b6d4', '#3b82f6', '#8b5cf6', '#ec4899'
];

// Connection status
const connectionStatus = computed(() => {
    return props.client && props.client.connected ? 'connected' : 'disconnected';
});

// Drawing functions
const draw = (x, y, color = '#000000', width = 3, erase = false) => {
    // Ensure coordinates are within canvas bounds
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
    
    // Don't use beginPath here - we want continuous strokes during drawing
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
    
    // Emit clear event
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
    
    // ctx.beginPath();
    // Emit stroke end event
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
        console.log("publishedd");
        
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
};;


const handleTouchStart = (e) => {
    if (!props.isDrawer) return;

    e.preventDefault();
    const touch = getTouchPos(e);
    lastX.value = touch.x;
    lastY.value = touch.y;
    isDrawing = true;

    ctx.beginPath();
    ctx.moveTo(touch.x, touch.y);

    // Emit stroke start event
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

    // ctx.beginPath();

    // Emit stroke end event
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
    if (!isDrawing || !props.isDrawer) return

    const touch = getTouchPos(e);
    draw(touch.x, touch.y, currentColor.value, brushSize.value, isEraser.value);

    // Emit drawing event
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
    
    // Don't draw our own strokes
    console.log("msg",msg);
    console.log("props",props);
    
    if (msg.username === props.username) return;
    console.log("otherone drawed");
    
    // Ensure we have a valid starting position
    const lastPos = remoteLastPositions.value[msg.username];
    if (!lastPos) {
        // If no last position, just update it and return
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
    
    // Update the last position for this user
    remoteLastPositions.value[msg.username] = { x: msg.x, y: msg.y };
};

const handleRemoteClear = (data) => {
    // const msg = JSON.parse(data.body);
    //
    // Don't clear for our own clear action
    // if (msg.username === props.username) return;
    console.log("clearrr");
    
    ctx.clearRect(0, 0, canvas.value.width, canvas.value.height);
    remoteLastPositions.value = {};
    remoteDrawingStates.value = {};
};
// Updated remote drawing handlers:
const handleRemoteDrawStart = (data) => {
    const msg = JSON.parse(data.body);
    
    if (msg.username === props.username) return;
    
    // Mark this user as actively drawing and set their starting position
    remoteDrawingStates.value[msg.username] = true;
    remoteLastPositions.value[msg.username] = { x: msg.x, y: msg.y };
     // Start a new path for remote drawing
    ctx.beginPath();
    ctx.moveTo(msg.x, msg.y);
};

const handleRemoteDrawEnd = (data) => {
    const msg = JSON.parse(data.body);
    if (msg.username !== props.username) {
        remoteDrawingStates.value[msg.username] = false;

        // ctx.beginPath();
    }
};
// Resize handler
const resizeCanvas = () => {
    const c = canvas.value;
    const rect = c.getBoundingClientRect();
    
    // Only resize if size actually changed
    if (c.width === rect.width && c.height === rect.height) return;
    
    // Clear and resize - don't try to preserve stretched content
    c.width = rect.width;
    c.height = rect.height;
    
    // Reset all remote positions when canvas resizes
    remoteLastPositions.value = {};
    remoteDrawingStates.value = {};
};

// Lifecycle
onMounted(() => {
    const c = canvas.value;
    ctx = c.getContext('2d');
    
    // Set initial canvas size
    resizeCanvas();
    
    // Setup resize observer
    const resizeObserver = new ResizeObserver(resizeCanvas);
    resizeObserver.observe(c.parentElement);
    
    // Subscribe to drawing events
    if (props.client && props.client.connected) {
        props.client.subscribe(`/topic/draw-start/${props.roomId}`, handleRemoteDrawStart);
        props.client.subscribe(`/topic/draw/${props.roomId}`, handleRemoteDraw);
        props.client.subscribe(`/topic/clear/${props.roomId}`, handleRemoteClear);
        props.client.subscribe(`/topic/timer-ended/${props.roomId}`, handleRemoteClear);
        props.client.subscribe(`/topic/draw-end/${props.roomId}`, handleRemoteDrawEnd);
    }
});

// Watch for client changes
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
    // Cleanup is handled automatically by Vue
});
</script>