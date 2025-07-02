<template>
    <canvas ref="canvas" class="w-full h-[500px] border bg-white touch-none"></canvas>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue';

const props = defineProps({
    client: Object,
    roomId: String,
    username: String,
});

const canvas = ref(null);
let ctx;
let isDrawing = false;

const draw = (x, y, color = '#000000', width = 3) => {
    ctx.strokeStyle = color;
    ctx.lineWidth = width;
    ctx.lineTo(x, y);
    ctx.stroke();
    ctx.beginPath();
    ctx.moveTo(x, y);
};

const handleMouseDown = () => {
    isDrawing = true;
};

const handleMouseUp = () => {
    isDrawing = false;
    ctx.beginPath();
};

const handleMouseMove = (e) => {
    if (!isDrawing) return;

    const rect = canvas.value.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;

    draw(x, y);

    // Emit drawing event
    if (props.client && props.client.connected) {
        props.client.publish({
            destination: '/app/draw',
            body: JSON.stringify({
                roomId: props.roomId,
                x,
                y,
                color: '#000000',
                width: 3,
                isDragging: true,
            }),
        });
    }
};

const handleRemoteDraw = (data) => {
    const msg = JSON.parse(data.body);
    draw(msg.x, msg.y, msg.color, msg.width);
};

onMounted(() => {
    const c = canvas.value;
    ctx = c.getContext('2d');
    c.width = c.offsetWidth;
    c.height = c.offsetHeight;

    c.addEventListener('mousedown', handleMouseDown);
    c.addEventListener('mouseup', handleMouseUp);
    c.addEventListener('mousemove', handleMouseMove);

    // ✅ Only subscribe if client is connected and exists
    if (props.client && props.client.connected) {
        props.client.subscribe(`/topic/draw/${props.roomId}`, handleRemoteDraw);
    } else {
        console.warn('STOMP client not ready in CanvasBoard');
    }
});
watch(() => props.client, (newClient) => {
    if (newClient && newClient.connected) {
        newClient.subscribe(`/topic/draw/${props.roomId}`, handleRemoteDraw);
    }
});
onBeforeUnmount(() => {
    const c = canvas.value;
    c.removeEventListener('mousedown', handleMouseDown);
    c.removeEventListener('mouseup', handleMouseUp);
    c.removeEventListener('mousemove', handleMouseMove);
});
</script>
