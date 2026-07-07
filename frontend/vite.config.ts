import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { VantResolver } from '@vant/auto-import-resolver'

export default defineConfig({
  plugins: [
    vue(),
    AutoImport({
      resolvers: [VantResolver()]
    }),
    Components({
      resolvers: [VantResolver()]
    })
  ],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  resolve: {
    alias: {
      '@': '/src'
    }
  },
  build: {
    // 生产环境移除 console 和 debugger
    minify: 'terser',
    terserOptions: {
      compress: {
        drop_console: true,
        drop_debugger: true
      }
    },
    rollupOptions: {
      output: {
        // 代码分包策略
        manualChunks(id: string) {
          // 将 vue 相关框架打入 vendor
          if (id.includes('node_modules/vue') ||
              id.includes('node_modules/vue-router') ||
              id.includes('node_modules/@vue')) {
            return 'vendor-vue'
          }
          // 将 vant UI 单独打包
          if (id.includes('node_modules/vant') ||
              id.includes('node_modules/@vant')) {
            return 'vendor-vant'
          }
          // 将 echarts 单独打包
          if (id.includes('node_modules/echarts') ||
              id.includes('node_modules/vue-echarts')) {
            return 'vendor-echarts'
          }
          // 将其他第三方依赖打包
          if (id.includes('node_modules')) {
            return 'vendor'
          }
        }
      }
    },
    // 生成 sourcemap 条件（生产环境不需要）
    sourcemap: false,
    // 启用 CSS 代码分割
    cssCodeSplit: true
  }
})
