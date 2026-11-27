import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Spring 백엔드(8080)로 /api 요청을 넘기는 프록시. 목 API를 쓰는 동안은 쓰이지 않는다.
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
});
