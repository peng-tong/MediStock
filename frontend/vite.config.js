import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// Vite 配置：开发服务器 5173，/api 代理到后端（后端默认 8080，可用环境变量覆盖）
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: process.env.MEDISTOCK_API || 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
