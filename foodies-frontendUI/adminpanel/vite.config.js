import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],

  server: {
    port: 5174,     // dev server port
    // strictPort: true, // uncomment to fail if 5174 is taken (no auto-increment)
  },
  
})
