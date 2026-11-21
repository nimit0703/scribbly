import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  // sockjs-client expects Node's `global`
  define: { global: 'window' },
  server: {
    // The Spring Boot server; the browser only ever talks to the Vite origin.
    proxy: { '/ws': { target: 'http://localhost:8080', ws: true } },
  },
  test: { environment: 'node' },
})
