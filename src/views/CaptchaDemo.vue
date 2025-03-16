<template>
  <div class="captcha-demo-container">
    <h2>验证码组件示例</h2>
    
    <div class="demo-section">
      <h3>基本使用方法</h3>
      <div class="demo-box">
        <p class="demo-description">基础验证码组件，包含输入框和验证码图片，点击图片可刷新：</p>
        <div class="captcha-wrapper">
          <Captcha v-model="captchaValue" @captcha-verified="handleVerified" />
        </div>
        <div class="demo-info" v-if="verifiedInfo">
          <el-alert
            :title="verifiedInfo.message"
            :type="verifiedInfo.type"
            show-icon
          />
        </div>
        <div class="demo-actions">
          <el-button type="primary" @click="verifyCaptcha">验证</el-button>
          <el-button @click="resetCaptcha">重置</el-button>
        </div>
      </div>
    </div>
    
    <div class="demo-section">
      <h3>在表单中使用</h3>
      <div class="demo-box">
        <p class="demo-description">在表单中集成验证码组件：</p>
        <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" placeholder="请输入用户名" />
          </el-form-item>
          
          <el-form-item label="验证码" prop="captcha">
            <Captcha 
              v-model="form.captcha" 
              @captcha-loaded="handleCaptchaLoaded"
              @captcha-refreshed="handleCaptchaRefreshed" 
            />
          </el-form-item>
          
          <el-form-item>
            <el-button type="primary" @click="submitForm">提交表单</el-button>
            <el-button @click="resetForm">重置表单</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
    
    <div class="demo-section">
      <h3>API 说明</h3>
      <div class="api-box">
        <h4>Props</h4>
        <el-table :data="propsData" style="width: 100%">
          <el-table-column prop="name" label="参数" width="180" />
          <el-table-column prop="description" label="说明" />
          <el-table-column prop="type" label="类型" width="180" />
          <el-table-column prop="default" label="默认值" width="180" />
        </el-table>
        
        <h4>Events</h4>
        <el-table :data="eventsData" style="width: 100%">
          <el-table-column prop="name" label="事件名" width="180" />
          <el-table-column prop="description" label="说明" />
          <el-table-column prop="params" label="参数" />
        </el-table>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import Captcha from '@/components/Captcha.vue'

export default {
  name: 'CaptchaDemo',
  components: {
    Captcha
  },
  setup() {
    // 验证码值
    const captchaValue = ref('')
    // 验证结果信息
    const verifiedInfo = ref(null)
    // 验证码token
    const captchaToken = ref('')
    
    // 验证函数
    const verifyCaptcha = () => {
      if (!captchaValue.value) {
        verifiedInfo.value = {
          message: '请输入验证码',
          type: 'warning'
        }
        return
      }
      
      // 模拟验证过程
      setTimeout(() => {
        const success = captchaValue.value.length >= 4
        verifiedInfo.value = {
          message: success ? '验证码验证成功' : '验证码验证失败',
          type: success ? 'success' : 'error'
        }
      }, 500)
    }
    
    // 重置验证码
    const resetCaptcha = () => {
      captchaValue.value = ''
      verifiedInfo.value = null
    }
    
    // 处理验证事件
    const handleVerified = (data) => {
      console.log('验证码已验证:', data)
      // 这里可以在输入时就进行后端验证
    }
    
    // 表单相关
    const formRef = ref(null)
    const form = reactive({
      username: '',
      captcha: '',
      captchaToken: ''
    })
    
    const rules = {
      username: [
        { required: true, message: '请输入用户名', trigger: 'blur' },
        { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
      ],
      captcha: [
        { required: true, message: '请输入验证码', trigger: 'blur' },
        { min: 4, max: 6, message: '验证码长度在 4 到 6 个字符', trigger: 'blur' }
      ]
    }
    
    // 处理验证码加载事件
    const handleCaptchaLoaded = (token) => {
      form.captchaToken = token
      console.log('验证码已加载, token:', token)
    }
    
    // 处理验证码刷新事件
    const handleCaptchaRefreshed = () => {
      console.log('验证码已刷新')
    }
    
    // 提交表单
    const submitForm = () => {
      formRef.value.validate((valid) => {
        if (valid) {
          ElMessage.success('表单验证通过，可以提交')
          console.log('表单数据:', form)
        } else {
          ElMessage.error('表单验证失败')
        }
      })
    }
    
    // 重置表单
    const resetForm = () => {
      formRef.value.resetFields()
    }
    
    // API 文档数据
    const propsData = [
      {
        name: 'modelValue / v-model',
        description: '验证码输入值',
        type: 'String',
        default: '""'
      },
      {
        name: 'captchaApi',
        description: '验证码获取API路径',
        type: 'String',
        default: '"/api/captcha/image"'
      },
      {
        name: 'placeholder',
        description: '输入框占位文本',
        type: 'String',
        default: '"请输入验证码"'
      },
      {
        name: 'disabled',
        description: '是否禁用组件',
        type: 'Boolean',
        default: 'false'
      }
    ]
    
    const eventsData = [
      {
        name: 'update:modelValue',
        description: '验证码输入值变化时触发',
        params: '新的验证码值'
      },
      {
        name: 'captcha-loaded',
        description: '验证码加载完成时触发',
        params: '验证码token'
      },
      {
        name: 'captcha-refreshed',
        description: '验证码刷新时触发',
        params: '无'
      },
      {
        name: 'captcha-verified',
        description: '验证码通过前端验证时触发',
        params: '{value: 验证码值, token: 验证码token}'
      }
    ]
    
    return {
      captchaValue,
      verifiedInfo,
      captchaToken,
      verifyCaptcha,
      resetCaptcha,
      handleVerified,
      formRef,
      form,
      rules,
      handleCaptchaLoaded,
      handleCaptchaRefreshed,
      submitForm,
      resetForm,
      propsData,
      eventsData
    }
  }
}
</script>

<style scoped>
.captcha-demo-container {
  max-width: 800px;
  margin: 20px auto;
  padding: 20px;
}

h2 {
  text-align: center;
  margin-bottom: 30px;
  color: #303133;
}

.demo-section {
  margin-bottom: 40px;
}

h3 {
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #ebeef5;
  color: #303133;
}

.demo-box {
  background-color: #f9f9f9;
  padding: 20px;
  border-radius: 8px;
}

.demo-description {
  margin-bottom: 20px;
  color: #606266;
}

.captcha-wrapper {
  max-width: 400px;
  margin-bottom: 20px;
}

.demo-info {
  margin: 15px 0;
}

.demo-actions {
  margin-top: 20px;
}

.api-box {
  background-color: #f9f9f9;
  padding: 20px;
  border-radius: 8px;
}

h4 {
  margin: 20px 0 10px;
  color: #303133;
}

/* ElementPlus表格样式覆盖 */
:deep(.el-table) {
  margin-bottom: 30px;
}
</style> 