import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 开发服务器：5173端口，/kb开头的请求全部代理到网关9001，
// 这样前端代码里不用写死后端地址，也没有跨域问题
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/kb': {
        target: 'http://localhost:9001',
        changeOrigin: true
      }
    }
  },
  // 生产预览：npm run build后npm run preview用这个。
  // host 0.0.0.0=监听所有网卡，局域网设备才能进来；
  // proxy不写时preview默认复用server.proxy，/kb照样转网关
  preview: {
    port: 4173,
    host: '0.0.0.0'
  }
})
