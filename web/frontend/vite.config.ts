import react from '@vitejs/plugin-react';
import { defineConfig } from 'vitest/config';

// Allow the backend port to be overridden via VITE_API_PORT in the shell or
// .env.local.  Defaults to 8000 (the standard dev port).
const apiPort = process.env['VITE_API_PORT'] ?? '8000';

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      // Forward all /api/* requests to the FastAPI backend.
      '/api': {
        target: `http://localhost:${apiPort}`,
        changeOrigin: true,
      },
    },
  },
  // Pre-bundle React, Emotion, and MUI together and dedupe React so a single
  // React instance reaches MUI's ThemeProvider. Without this the dev server
  // splits React across pre-bundles and ThemeProvider's hooks fail at runtime.
  resolve: {
    dedupe: ['react', 'react-dom'],
  },
  optimizeDeps: {
    include: [
      'react',
      'react-dom',
      'react/jsx-runtime',
      '@emotion/react',
      '@emotion/styled',
      '@mui/material',
    ],
  },
  test: {
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
    include: ['src/**/*.{test,spec}.{ts,tsx}'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'lcov'],
      include: ['src/**/*.{ts,tsx}'],
      exclude: [
        'src/test/**',
        'src/**/*.test.*',
        'src/**/*.spec.*',
        'src/**/*.d.ts',
        'src/main.tsx',
      ],
      thresholds: {
        statements: 90,
        branches: 90,
        functions: 90,
        lines: 90,
      },
    },
  },
});
