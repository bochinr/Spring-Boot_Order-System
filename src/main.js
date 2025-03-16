import axios from 'axios'
import { request } from './utils'

// 将自定义的请求实例设置为全局默认
axios.defaults.baseURL = request.defaults.baseURL
axios.defaults.timeout = request.defaults.timeout
axios.defaults.withCredentials = request.defaults.withCredentials

// 全局使用拦截器
axios.interceptors.request.use(
    request.interceptors.request.handlers[0].fulfilled,
    request.interceptors.request.handlers[0].rejected
)

axios.interceptors.response.use(
    request.interceptors.response.handlers[0].fulfilled,
    request.interceptors.response.handlers[0].rejected
)

// 将请求实例挂载到全局
app.config.globalProperties.$axios = request
app.config.globalProperties.$http = request 