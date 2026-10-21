import { createApp } from 'vue'
import { createPinia } from 'pinia'
import TDesign from 'tdesign-vue-next'
import 'tdesign-vue-next/es/style/index.css'
import './style.css'
import App from './App.vue'
import router from './router'

createApp(App).use(createPinia()).use(TDesign).use(router).mount('#app')
