import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// https://vite.dev/config/
export default defineConfig({
  base: './',
  plugins: [react()],
  server: {
    port: 5173,
    watch: {
      usePolling: true,
      interval: 1000,
      ignored: [
        '**/node_modules/**',
        '**/.git/**',
        '**/engine/bin/**',
        '**/dist/**',
        '**/*Faithful*/**',
        '**/*Release*/**',
        '**/*.zip',
        '**/*.txt'
      ]
    }
  }
});
