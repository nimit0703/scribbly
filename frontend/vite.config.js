import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue(), tailwindcss()],
  define: {
    global: 'window'
  },
  server: {
    https:false,
    port: 3000, // 👈 change this to any port you want
    host: true  // optional: allows access from LAN/IP
  }
})
