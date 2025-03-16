<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-title">
        <h2>欢迎登录系统</h2>
      </div>
      
      <el-tabs v-model="activeTab" class="login-tabs">
        <el-tab-pane label="手机号登录" name="phone">
          <el-form ref="phoneFormRef" :model="phoneForm" :rules="phoneRules" label-position="top">
            <!-- 手机号输入 -->
            <el-form-item prop="phone" label="手机号">
              <el-input v-model="phoneForm.phone" placeholder="请输入手机号" prefix-icon="el-icon-mobile">
              </el-input>
            </el-form-item>
            
            <!-- 图形验证码 -->
            <el-form-item prop="captcha" label="图形验证码">
              <Captcha 
                v-model="phoneForm.captcha" 
                @captcha-loaded="handleCaptchaLoaded" 
                @captcha-refreshed="handleCaptchaRefreshed"
              />
            </el-form-item>
            
            <!-- 短信验证码 -->
            <el-form-item prop="smsCode" label="短信验证码">
              <div class="sms-container">
                <el-input v-model="phoneForm.smsCode" placeholder="请输入短信验证码"></el-input>
                <el-button 
                  type="primary" 
                  :disabled="smsBtnDisabled" 
                  @click="getSmsCode"
                >
                  {{ smsButtonText }}
                </el-button>
              </div>
            </el-form-item>
            
            <!-- 登录按钮 -->
            <el-form-item>
              <el-button type="primary" class="login-button" @click="submitPhoneLogin">
                登录
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        
        <el-tab-pane label="邮箱登录" name="email">
          <el-form ref="emailFormRef" :model="emailForm" :rules="emailRules" label-position="top">
            <!-- 邮箱输入 -->
            <el-form-item prop="email" label="邮箱">
              <el-input v-model="emailForm.email" placeholder="请输入邮箱" prefix-icon="el-icon-message">
              </el-input>
            </el-form-item>
            
            <!-- 密码输入 -->
            <el-form-item prop="password" label="密码">
              <el-input v-model="emailForm.password" type="password" placeholder="请输入密码" show-password>
              </el-input>
            </el-form-item>
            
            <!-- 登录按钮 -->
            <el-form-item>
              <el-button type="primary" class="login-button" @click="submitEmailLogin">
                登录
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        
        <el-tab-pane label="第三方登录" name="oauth">
          <div class="oauth-container">
            <p class="oauth-tip">请选择以下第三方账号登录：</p>
            <div class="oauth-buttons">
              <el-button @click="oauthLogin('wechat')" class="oauth-btn wechat-btn">
                <i class="wechat-icon"></i>
                微信登录
              </el-button>
              <el-button @click="oauthLogin('alipay')" class="oauth-btn alipay-btn">
                <i class="alipay-icon"></i>
                支付宝登录
              </el-button>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
      
      <div class="register-link">
        <span>还没有账号？</span>
        <router-link to="/register">立即注册</router-link>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { request } from '@/utils'
import Captcha from '@/components/Captcha.vue'

export default {
  name: 'Login',
  components: {
    Captcha
  },
  setup() {
    // 当前激活的标签页
    const activeTab = ref('phone')
    
    // 手机号登录表单
    const phoneFormRef = ref(null)
    const phoneForm = reactive({
      phone: '',
      captcha: '',
      smsCode: '',
      captchaToken: ''
    })
    
    // 手机号表单验证规则
    const phoneRules = {
      phone: [
        { required: true, message: '请输入手机号', trigger: 'blur' },
        { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
      ],
      captcha: [
        { required: true, message: '请输入图形验证码', trigger: 'blur' },
        { min: 4, max: 6, message: '验证码长度在4到6个字符之间', trigger: 'blur' }
      ],
      smsCode: [
        { required: true, message: '请输入短信验证码', trigger: 'blur' },
        { min: 6, max: 6, message: '短信验证码必须是6位数字', trigger: 'blur' },
        { pattern: /^\d{6}$/, message: '短信验证码必须是6位数字', trigger: 'blur' }
      ]
    }
    
    // 邮箱登录表单
    const emailFormRef = ref(null)
    const emailForm = reactive({
      email: '',
      password: ''
    })
    
    // 邮箱表单验证规则
    const emailRules = {
      email: [
        { required: true, message: '请输入邮箱', trigger: 'blur' },
        { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
      ],
      password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { min: 6, message: '密码长度不能少于6个字符', trigger: 'blur' }
      ]
    }
    
    // 短信验证码按钮状态
    const smsBtnDisabled = ref(false)
    const smsButtonText = ref('获取验证码')
    const countDown = ref(60)
    let timer = null
    
    // 处理验证码加载完成事件
    const handleCaptchaLoaded = (token) => {
      phoneForm.captchaToken = token
    }
    
    // 处理验证码刷新事件
    const handleCaptchaRefreshed = () => {
      // 验证码刷新后，清空短信验证码
      phoneForm.smsCode = ''
    }
    
    // 获取短信验证码
    const getSmsCode = () => {
      // 验证手机号和图形验证码
      phoneFormRef.value.validateField(['phone', 'captcha'], valid => {
        if (!valid) {
          // 发送获取短信验证码的请求
          request.post('/api/auth/sms/send', {
            phone: phoneForm.phone,
            captcha: phoneForm.captcha,
            captchaToken: phoneForm.captchaToken
          })
            .then(response => {
              ElMessage.success('验证码发送成功，请注意查收')
              
              // 开始倒计时
              startCountDown()
            })
            .catch(error => {
              // 错误处理已在拦截器中完成
              console.error('发送验证码失败', error)
            })
        }
      })
    }
    
    // 开始倒计时
    const startCountDown = () => {
      smsBtnDisabled.value = true
      countDown.value = 60
      smsButtonText.value = `${countDown.value}秒后重新获取`
      
      clearInterval(timer)
      timer = setInterval(() => {
        countDown.value--
        smsButtonText.value = `${countDown.value}秒后重新获取`
        
        if (countDown.value <= 0) {
          clearInterval(timer)
          smsBtnDisabled.value = false
          smsButtonText.value = '获取验证码'
        }
      }, 1000)
    }
    
    // 提交手机号登录
    const submitPhoneLogin = () => {
      phoneFormRef.value.validate(valid => {
        if (valid) {
          // 发送登录请求
          request.post('/api/auth/login', {
            loginType: 'phone',
            principal: phoneForm.phone,
            credential: phoneForm.smsCode
          })
            .then(response => {
              handleLoginSuccess(response.data)
            })
            .catch(error => {
              // 错误处理已在拦截器中完成
              console.error('登录失败', error)
            })
        }
      })
    }
    
    // 提交邮箱登录
    const submitEmailLogin = () => {
      emailFormRef.value.validate(valid => {
        if (valid) {
          // 发送登录请求
          request.post('/api/auth/login', {
            loginType: 'email',
            principal: emailForm.email,
            credential: emailForm.password
          })
            .then(response => {
              handleLoginSuccess(response.data)
            })
            .catch(error => {
              // 错误处理已在拦截器中完成
              console.error('登录失败', error)
            })
        }
      })
    }
    
    // 第三方登录
    const oauthLogin = (platform) => {
      // 获取授权URL
      request.get(`/api/oauth/${platform}/auth-url`)
        .then(response => {
          if (response.data && response.data.authUrl) {
            const authUrl = response.data.authUrl;
            const state = response.data.state;
            
            // 使用新窗口打开授权页面
            const oauthWindow = window.open(authUrl, `${platform}Auth`, 'width=800,height=600');
            
            // 存储state以便后续验证
            localStorage.setItem('oauth_state', state);
            localStorage.setItem('oauth_platform', platform);
            
            // 开始轮询检查登录状态
            startPollingLoginStatus(oauthWindow);
          } else {
            ElMessage.error('获取授权链接失败');
          }
        })
        .catch(error => {
          // 错误处理已在拦截器中完成
          console.error('获取授权链接失败', error)
        });
    }
    
    // 轮询检查登录状态
    const startPollingLoginStatus = (authWindow) => {
      // 显示加载提示
      ElMessage({
        message: '正在等待授权完成...',
        type: 'info',
        duration: 0,
        showClose: true
      });
      
      // 检查窗口是否关闭
      const checkWindowClosed = setInterval(() => {
        if (authWindow.closed) {
          clearInterval(checkWindowClosed);
          clearInterval(checkLoginStatus);
          ElMessage.closeAll();
          ElMessage.warning('授权窗口已关闭');
        }
      }, 500);
      
      // 轮询检查登录状态
      const checkLoginStatus = setInterval(() => {
        const platform = localStorage.getItem('oauth_platform');
        const state = localStorage.getItem('oauth_state');
        
        if (!platform || !state) {
          clearInterval(checkLoginStatus);
          return;
        }
        
        request.get(`/api/oauth/${platform}/status`, {
          // 添加时间戳防止缓存
          params: { 
            state,
            t: new Date().getTime() 
          }
        })
          .then(response => {
            if (response.data && response.data.status === 'success') {
              // 登录成功
              clearInterval(checkLoginStatus);
              clearInterval(checkWindowClosed);
              ElMessage.closeAll();
              
              // 关闭授权窗口
              authWindow.close();
              
              // 处理登录成功
              handleLoginSuccess(response.data);
            }
          })
          .catch(error => {
            console.error('检查登录状态失败:', error);
          });
      }, 2000); // 每2秒检查一次
      
      // 10分钟后自动停止轮询
      setTimeout(() => {
        clearInterval(checkLoginStatus);
        clearInterval(checkWindowClosed);
        ElMessage.closeAll();
        ElMessage.warning('授权超时，请重试');
      }, 10 * 60 * 1000);
    }
    
    // 处理登录成功
    const handleLoginSuccess = (data) => {
      if (data && data.data && data.data.token) {
        // 存储token
        localStorage.setItem('token', data.data.token)
        localStorage.setItem('userInfo', JSON.stringify({
          userId: data.data.userId,
          username: data.data.username,
          role: data.data.role
        }))
        
        // 提示登录成功
        ElMessage.success('登录成功')
        
        // 跳转到首页或其他页面
        setTimeout(() => {
          window.location.href = '/'
        }, 1000)
      } else {
        ElMessage.error('登录失败，返回数据异常')
      }
    }
    
    // 组件挂载时不再需要手动获取验证码，由Captcha组件自动处理
    onMounted(() => {
      // 初始化操作
    })
    
    return {
      activeTab,
      phoneFormRef,
      phoneForm,
      phoneRules,
      emailFormRef,
      emailForm,
      emailRules,
      smsBtnDisabled,
      smsButtonText,
      getSmsCode,
      submitPhoneLogin,
      submitEmailLogin,
      oauthLogin,
      handleCaptchaLoaded,
      handleCaptchaRefreshed
    }
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #f5f7fa;
}

.login-box {
  width: 400px;
  padding: 30px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.login-title {
  text-align: center;
  margin-bottom: 30px;
}

.login-title h2 {
  font-size: 24px;
  color: #303133;
  margin: 0;
}

.login-tabs {
  margin-bottom: 20px;
}

.captcha-container, .sms-container {
  display: flex;
  align-items: center;
  gap: 10px;
}

.captcha-img {
  height: 40px;
  cursor: pointer;
  border-radius: 4px;
}

.login-button {
  width: 100%;
  margin-top: 10px;
}

.oauth-container {
  padding: 20px 0;
  text-align: center;
}

.oauth-tip {
  margin-bottom: 20px;
  color: #606266;
}

.oauth-buttons {
  display: flex;
  justify-content: center;
  gap: 20px;
}

.oauth-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px 20px;
}

.wechat-btn {
  background-color: #07C160;
  color: white;
  border: none;
}

.alipay-btn {
  background-color: #1677FF;
  color: white;
  border: none;
}

.wechat-icon, .alipay-icon {
  display: inline-block;
  width: 24px;
  height: 24px;
  margin-right: 5px;
  background-size: contain;
  background-repeat: no-repeat;
}

.wechat-icon {
  background-image: url('data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0id2hpdGUiPjxwYXRoIGQ9Ik04LjQsNy4xQzguMzgsNC41LDEwLjY3LDIuNCwxMy4zNywyLjM4YzIuNy0wLjAyLDQuOTIsMi4wNCw1LjAxLDQuNjJjMC4wOSwyLjU4LTEuOTUsNC43Ny00LjU0LDQuOWMtMi41OCwwLjEzLTQuNzktMS44NS00Ljk2LTQuNDJDOC44Niw3LjM1LDguNjEsNy4yMyw4LjQsNy4xeiBNMTMuMzMsMTEuOWMxLjEtMC4wMSwyLjExLTAuNTgsMi42OS0xLjUyYzAuNTgtMC45NCwwLjYtMi4xMSwwLjA1LTMuMDZjLTAuNTUtMC45Ni0xLjU0LTEuNTYtMi42NC0xLjU5Yy0xLjEtMC4wMy0yLjEyLDAuNS0yLjcxLDEuNDFjLTAuNiwwLjkxLTAuNjcsMi4wOC0wLjE2LDMuMDZDMTEuMDcsMTEuMTksMTIuMTUsMTEuODcsMTMuMzMsMTEuOXoiLz48cGF0aCBkPSJNMTguOTEsMTAuOWMyLjQ1LTAuMDIsNC40NiwxLjk5LDQuNDksNC40NGMwLjAyLDIuNDUtMS45Niw0LjQ2LTQuNDIsNC41Yy0yLjQ1LDAuMDQtNC40Ni0xLjk2LTQuNTItNC40MUMxNC40LDEyLjkzLDE2LjM5LDEwLjkzLDE4LjkxLDEwLjl6Ii8+PHBhdGggZD0iTTcuNzcsMTAuODRjMi40NiwwLjAyLDQuNDUsMi4wNyw0LjQyLDQuNTNjLTAuMDMsMi40NS0yLjA0LDQuNDEtNC40OSw0LjM5Yy0yLjQ1LTAuMDItNC40NC0yLjA0LTQuNDMtNC40OUMzLjI5LDEyLjgyLDUuMywxMC44Miw3Ljc3LDEwLjg0eiIvPjwvc3ZnPg==');
}

.alipay-icon {
  background-image: url('data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0id2hpdGUiPjxwYXRoIGQ9Ik0yMiwxMmMwLDUuNTItNC40OCwxMC0xMCwxMFMyLDE3LjUyLDIsMTJTNi40OCwyLDEyLDJTMjIsNi40OCwyMiwxMnogTTEwLjUsOS4yNUg4LjI1djEuNWgyLjI1djEuNUg4LjI1djEuNWgyLjI1VjE1YzAsMC40MSwwLjM0LDAuNzUsMC43NSwwLjc1aDEuNWMwLjQxLDAsMC43NS0wLjM0LDAuNzUtMC43NXYtMS4yNWgyLjI1di0xLjVIMTMuNXYtMS41aDIuMjV2LTEuNUgxMy41VjhjMC0wLjQxLTAuMzQtMC43NS0wLjc1LTAuNzVoLTEuNUMxMC44NCw3LjI1LDEwLjUsNy41OSwxMC41LDhWOS4yNXoiLz48L3N2Zz4=');
}

.register-link {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: #606266;
}

.register-link a {
  color: #409EFF;
  text-decoration: none;
}

.register-link a:hover {
  text-decoration: underline;
}
</style> 