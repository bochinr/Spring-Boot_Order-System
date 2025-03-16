import axios from 'axios'
import { ElMessage } from 'element-plus'

// 创建axios实例
const service = axios.create({
    baseURL: process.env.VUE_APP_BASE_API || '', // API基础URL，可从环境变量获取
    timeout: 15000, // 请求超时时间
    withCredentials: true // 跨域请求时发送cookie
})

// 需要验证码的接口路径
const captchaRequiredApis = [
    '/api/auth/sms/send',
    '/api/auth/register',
    '/api/auth/reset-password'
]

// 不需要token的白名单接口
const whiteListApis = [
    '/api/auth/login',
    '/api/captcha/image',
    '/api/oauth'
]

// 记录重试次数
const retryCount = new Map()
// 最大重试次数
const MAX_RETRY_COUNT = 1

// 是否正在刷新token
let isRefreshing = false
// 重试队列
let retryQueue = []

// 刷新token的函数（如果有刷新token的接口）
const refreshToken = async () => {
    try {
        const refreshToken = localStorage.getItem('refreshToken')
        if (!refreshToken) {
            return Promise.reject(new Error('No refresh token'))
        }

        const response = await axios.post('/api/auth/refresh-token', {
            refreshToken
        })

        if (response.data && response.data.data && response.data.data.token) {
            localStorage.setItem('token', response.data.data.token)
            if (response.data.data.refreshToken) {
                localStorage.setItem('refreshToken', response.data.data.refreshToken)
            }
            return response.data.data.token
        } else {
            return Promise.reject(new Error('Failed to refresh token'))
        }
    } catch (error) {
        return Promise.reject(error)
    }
}

// 检查API是否在指定列表中
const isApiInList = (url, apiList) => {
    return apiList.some(api => url.startsWith(api))
}

// 请求拦截器
service.interceptors.request.use(
    config => {
        // 重试计数
        const url = config.url
        if (!retryCount.has(url)) {
            retryCount.set(url, 0)
        }

        // 1. 如果不在白名单中，自动添加token到请求头
        if (!isApiInList(url, whiteListApis)) {
            const token = localStorage.getItem('token')
            if (token) {
                config.headers['Authorization'] = `Bearer ${token}`
            }
        }

        // 2. 检查是否需要添加验证码参数
        if (isApiInList(url, captchaRequiredApis)) {
            const captchaToken = localStorage.getItem('captchaToken')
            if (captchaToken && !config.params?.captchaToken && !config.data?.captchaToken) {
                // 根据请求方法决定如何附加参数
                if (config.method === 'get') {
                    config.params = {
                        ...config.params,
                        captchaToken
                    }
                } else {
                    // 如果是POST/PUT等方法，添加到请求体
                    if (typeof config.data === 'string') {
                        // 如果data是字符串，尝试解析为JSON
                        try {
                            const data = JSON.parse(config.data)
                            config.data = JSON.stringify({
                                ...data,
                                captchaToken
                            })
                        } catch (e) {
                            console.error('Failed to parse request data', e)
                        }
                    } else {
                        // 如果是对象，直接添加
                        config.data = {
                            ...config.data,
                            captchaToken
                        }
                    }
                }
            }
        }

        return config
    },
    error => {
        console.error('请求拦截器错误:', error)
        return Promise.reject(error)
    }
)

// 响应拦截器
service.interceptors.response.use(
    // 成功响应处理
    response => {
        // 重置重试计数
        retryCount.delete(response.config.url)

        // 如果接口返回了新的token，自动更新存储
        const newToken = response.headers['new-token'] || response.headers['x-token']
        if (newToken) {
            localStorage.setItem('token', newToken)
        }

        // 如果响应中包含验证码token，保存以备后用
        if (response.data?.data?.captchaToken) {
            localStorage.setItem('captchaToken', response.data.data.captchaToken)
        }

        // 只返回响应数据部分
        return response
    },

    // 错误响应处理
    async error => {
        if (!error.response) {
            ElMessage.error('网络连接失败，请检查网络设置')
            return Promise.reject(error)
        }

        const { response, config } = error
        const status = response.status
        const url = config.url

        // 处理401错误（未授权）
        if (status === 401) {
            // 获取当前重试次数
            const currentRetry = retryCount.get(url) || 0

            // 如果没有超过最大重试次数，进行token刷新并重试
            if (currentRetry < MAX_RETRY_COUNT) {
                retryCount.set(url, currentRetry + 1)

                // 如果当前没有在刷新token，则开始刷新
                if (!isRefreshing) {
                    isRefreshing = true

                    try {
                        // 尝试刷新token
                        const newToken = await refreshToken()

                        // token刷新成功，重试队列中所有请求
                        retryQueue.forEach(retry => retry(newToken))
                        retryQueue = []

                        // 用新token重试当前请求
                        config.headers['Authorization'] = `Bearer ${newToken}`
                        return service(config)
                    } catch (refreshError) {
                        // token刷新失败，清空队列
                        retryQueue.forEach(retry => retry(null))
                        retryQueue = []

                        // 清除登录状态
                        localStorage.removeItem('token')
                        localStorage.removeItem('refreshToken')
                        localStorage.removeItem('userInfo')

                        // 跳转到登录页
                        window.location.href = '/login'

                        return Promise.reject(refreshError)
                    } finally {
                        isRefreshing = false
                    }
                } else {
                    // 已经在刷新token，将请求加入队列
                    return new Promise(resolve => {
                        retryQueue.push(token => {
                            if (token) {
                                config.headers['Authorization'] = `Bearer ${token}`
                                resolve(service(config))
                            } else {
                                // token刷新失败的处理
                                resolve(Promise.reject(error))
                            }
                        })
                    })
                }
            } else {
                // 超过重试次数，清除登录状态并跳转到登录页
                localStorage.removeItem('token')
                localStorage.removeItem('refreshToken')
                localStorage.removeItem('userInfo')

                // 显示错误消息
                ElMessage.error('登录已过期，请重新登录')

                // 跳转到登录页，保存当前URL以便登录后返回
                const currentPath = window.location.pathname
                window.location.href = `/login?redirect=${encodeURIComponent(currentPath)}`

                return Promise.reject(error)
            }
        }

        // 处理其他错误状态码
        switch (status) {
            case 400:
                ElMessage.error(response.data?.message || '请求参数错误')
                break
            case 403:
                ElMessage.error('您没有权限执行此操作')
                break
            case 404:
                ElMessage.error('请求的资源不存在')
                break
            case 500:
                ElMessage.error('服务器内部错误')
                break
            default:
                ElMessage.error(response.data?.message || `请求失败(${status})`)
        }

        return Promise.reject(error)
    }
)

export default service 