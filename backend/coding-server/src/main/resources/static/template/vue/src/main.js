import { createApp } from 'vue'
import App from './App.vue'

const app = createApp(App)
app.mount('#app')

/*
  通用模板骨架。AI 在生成项目时会覆盖此文件，添加：
  - import router from './router'     → app.use(router)
  - import { createPinia } from 'pinia' → app.use(createPinia())
  - import './styles/global.css'
  - import 'animate.css'
  等全局配置。
*/
