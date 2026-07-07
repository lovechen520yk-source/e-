import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import 'vant/lib/index.css'
import './assets/main.css'

// 仅在开发环境加载 vconsole 调试面板
if (import.meta.env.DEV) {
  import('vconsole').then(({ default: VConsole }) => new VConsole())
}

const app = createApp(App)

app.use(router)
app.mount('#app')
