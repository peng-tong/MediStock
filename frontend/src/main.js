// 应用入口：挂载 Vue + 路由 + Element Plus（中文语言包）
import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
// Element Plus 基础样式先引入，global.css 中的主题变量与样式覆盖放在其后
import 'element-plus/dist/index.css'
import './assets/styles/global.css'
import App from './App.vue'
import router from './router'

createApp(App)
  .use(router)
  .use(ElementPlus, { locale: zhCn })
  .mount('#app')
