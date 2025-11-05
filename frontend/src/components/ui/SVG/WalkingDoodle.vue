<template>
  <div class="relative w-full h-64 bg-surface-light rounded-xl border-2 border-surface-dark overflow-hidden">
    
    <!-- Color particles -->
    <div 
      v-for="(particle, index) in particles" 
      :key="index"
      class="absolute w-3 h-3 rounded-full particle"
      :style="{
        left: particle.x + '%',
        top: particle.y + '%',
        backgroundColor: particle.color,
        animationDelay: particle.delay + 's'
      }"
    ></div>

  </div>
</template>

<script>
export default {
  data() {
    return {
      particles: []
    }
  },
  mounted() {
    this.generateParticles()
  },
  methods: {
    generateParticles() {
      const colors = [
        'var(--color-primary)',
        'var(--color-secondary)', 
        'var(--color-accent-green)',
        'var(--color-accent-yellow)',
        'var(--color-accent-red)',
        'var(--color-accent-purple)'
      ]
      
      this.particles = Array.from({ length: 20 }, (_, i) => ({
        x: Math.random() * 100,
        y: Math.random() * 100,
        color: colors[Math.floor(Math.random() * colors.length)],
        delay: Math.random() * 5
      }))
    }
  }
}
</script>

<style scoped>
.particle {
  animation: float-particle 8s ease-in-out infinite;
}

@keyframes float-particle {
  0%, 100% {
    transform: translate(0, 0) scale(1);
    opacity: 0.7;
  }
  25% {
    transform: translate(20px, -30px) scale(1.2);
    opacity: 1;
  }
  50% {
    transform: translate(-15px, -50px) scale(0.8);
    opacity: 0.5;
  }
  75% {
    transform: translate(-25px, -20px) scale(1.1);
    opacity: 0.9;
  }
}
</style>