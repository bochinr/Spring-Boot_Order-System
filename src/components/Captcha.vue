<template>
  <div class="captcha-component">
    <el-input
      v-model="captchaValue"
      :placeholder="placeholder"
      :disabled="disabled"
      :class="{ 'is-invalid': !isValid && isDirty }"
      @input="handleInput"
      @blur="handleBlur"
    />
    <div class="captcha-image-container" @click="refreshCaptcha">
      <img v-if="!loading && captchaImage" :src="captchaImage" alt="验证码" class="captcha-image" />
      <div v-else class="captcha-loading">
        <el-icon class="is-loading"><Loading /></el-icon>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, computed, watch } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import axios from 'axios'

export default {
  name: 'CaptchaComponent',
  components: {
    Loading
  },
  props: {
    // 绑定值
    modelValue: {
      type: String,
      default: ''
    },
    // API路径
    captchaApi: {
      type: String,
      default: '/api/captcha/image'
    },
    // 提示文字
    placeholder: {
      type: String,
      default: '请输入验证码'
    },
    // 是否禁用
    disabled: {
      type: Boolean,
      default: false
    }
  },
  emits: ['update:modelValue', 'captcha-loaded', 'captcha-refreshed', 'captcha-verified'],
  setup(props, { emit }) {
    // 验证码图片 Base64 数据
    const captchaImage = ref('')
    // 验证码 token
    const captchaToken = ref('')
    // 验证码值
    const captchaValue = computed({
      get: () => props.modelValue,
      set: (value) => emit('update:modelValue', value)
    })
    // 加载状态
    const loading = ref(true)
    // 是否被修改过
    const isDirty = ref(false)
    // 是否有效
    const isValid = ref(true)

    // 获取图形验证码
    const getCaptcha = async () => {
      loading.value = true
      try {
        // 添加时间戳防止缓存
        const timestamp = new Date().getTime()
        const url = `${props.captchaApi}?t=${timestamp}`
        
        const response = await axios.get(url)
        if (response.data && response.data.code === 200) {
          captchaImage.value = `data:image/png;base64,${response.data.data.image}`
          captchaToken.value = response.data.data.token
          emit('captcha-loaded', captchaToken.value)
        } else {
          console.error('获取验证码失败:', response.data.message || '未知错误')
        }
      } catch (error) {
        console.error('获取验证码异常:', error)
      } finally {
        loading.value = false
      }
    }

    // 刷新验证码
    const refreshCaptcha = () => {
      if (loading.value) return
      getCaptcha()
      captchaValue.value = ''
      isDirty.value = false
      isValid.value = true
      emit('captcha-refreshed')
    }

    // 处理输入
    const handleInput = () => {
      isDirty.value = true
      validateCaptcha()
    }

    // 处理失焦
    const handleBlur = () => {
      isDirty.value = true
      validateCaptcha()
    }

    // 验证验证码
    const validateCaptcha = () => {
      // 简单的前端验证：非空且长度适当
      isValid.value = captchaValue.value.length >= 4 && captchaValue.value.length <= 6
      
      // 如果需要后端验证可以在这里添加
      if (isValid.value && isDirty.value) {
        emit('captcha-verified', {
          value: captchaValue.value,
          token: captchaToken.value
        })
      }
    }

    // 组件挂载时获取验证码
    onMounted(() => {
      getCaptcha()
    })

    // 监听 disabled 属性变化，如果从禁用变为启用，自动刷新验证码
    watch(() => props.disabled, (newVal, oldVal) => {
      if (oldVal === true && newVal === false) {
        refreshCaptcha()
      }
    })

    return {
      captchaImage,
      captchaToken,
      captchaValue,
      loading,
      isDirty,
      isValid,
      refreshCaptcha,
      handleInput,
      handleBlur
    }
  }
}
</script>

<style scoped>
.captcha-component {
  display: flex;
  align-items: center;
  width: 100%;
}

.captcha-image-container {
  margin-left: 10px;
  height: 40px;
  min-width: 100px;
  border-radius: 4px;
  overflow: hidden;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f0f2f5;
  border: 1px solid #dcdfe6;
}

.captcha-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.captcha-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  font-size: 20px;
  color: #909399;
}

.is-invalid {
  border-color: #f56c6c;
}

.is-invalid:focus {
  border-color: #f56c6c;
  box-shadow: 0 0 0 1px rgba(245, 108, 108, 0.2);
}
</style> 