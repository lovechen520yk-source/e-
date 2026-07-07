<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast, showLoadingToast, closeToast } from 'vant'
import { login } from '@/api/auth'
import SlideCaptcha from '@/components/SlideCaptcha.vue'

const router = useRouter()
const route = useRoute()

const username = ref('')
const password = ref('')
const captchaToken = ref('')

const showPwd = ref(false)
const submitting = ref(false)
const showCaptcha = ref(false)

const slideCaptchaRef = ref<InstanceType<typeof SlideCaptcha>>()

/** 点击登录 - 验证表单后弹出滑块验证 */
const handleLoginClick = () => {
  if (!username.value.trim()) {
    showToast('请输入用户名')
    return
  }
  if (!password.value) {
    showToast('请输入密码')
    return
  }
  showCaptcha.value = true
}

/** 滑块验证通过回调 - 自动提交登录 */
const onCaptchaVerified = async (token: string) => {
  captchaToken.value = token
  showCaptcha.value = false

  submitting.value = true
  showLoadingToast({ message: '登录中...', forbidClick: true })

  try {
    const data = await login({
      username: username.value.trim(),
      password: password.value,
      captchaToken: token
    })

    localStorage.setItem('token', data.token)
    localStorage.setItem('username', data.username)

    closeToast()
    showToast('登录成功')

    const redirect = (route.query.redirect as string) || '/records'
    router.replace(redirect)
  } catch {
    captchaToken.value = ''
    closeToast()
    showToast('登录失败，请重试')
  } finally {
    submitting.value = false
  }
}

/** 关闭验证码弹窗 */
const onCaptchaClose = () => {
  showCaptcha.value = false
}

/** 跳转注册 */
const goRegister = () => {
  router.push('/register')
}

// ====== 动态粒子背景 ======
const canvasRef = ref<HTMLCanvasElement>()
let animationId = 0

interface Particle {
  x: number
  y: number
  vx: number
  vy: number
  r: number
  alpha: number
}

onMounted(() => {
  initParticles()
})

onUnmounted(() => {
  cancelAnimationFrame(animationId)
})

const initParticles = () => {
  const canvas = canvasRef.value
  if (!canvas) return

  const ctx = canvas.getContext('2d')
  if (!ctx) return

  canvas.width = window.innerWidth
  canvas.height = window.innerHeight

  const particles: Particle[] = []
  const count = Math.min(80, Math.floor(canvas.width / 10))

  for (let i = 0; i < count; i++) {
    particles.push({
      x: Math.random() * canvas.width,
      y: Math.random() * canvas.height,
      vx: (Math.random() - 0.5) * 0.8,
      vy: (Math.random() - 0.5) * 0.8,
      r: Math.random() * 3 + 1,
      alpha: Math.random() * 0.5 + 0.1
    })
  }

  const animate = () => {
    ctx.clearRect(0, 0, canvas.width, canvas.height)

    for (const p of particles) {
      p.x += p.vx
      p.y += p.vy

      if (p.x < 0 || p.x > canvas.width) p.vx *= -1
      if (p.y < 0 || p.y > canvas.height) p.vy *= -1

      ctx.beginPath()
      ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
      ctx.fillStyle = `rgba(102, 126, 234, ${p.alpha})`
      ctx.fill()
    }

    // 连线
    for (let i = 0; i < particles.length; i++) {
      for (let j = i + 1; j < particles.length; j++) {
        const dx = particles[i].x - particles[j].x
        const dy = particles[i].y - particles[j].y
        const dist = Math.sqrt(dx * dx + dy * dy)
        if (dist < 120) {
          ctx.beginPath()
          ctx.moveTo(particles[i].x, particles[i].y)
          ctx.lineTo(particles[j].x, particles[j].y)
          ctx.strokeStyle = `rgba(102, 126, 234, ${0.12 * (1 - dist / 120)})`
          ctx.lineWidth = 0.6
          ctx.stroke()
        }
      }
    }

    animationId = requestAnimationFrame(animate)
  }

  animate()

  // 窗口缩放时重置
  const onResize = () => {
    canvas.width = window.innerWidth
    canvas.height = window.innerHeight
  }
  window.addEventListener('resize', onResize)
}
</script>

<template>
  <div class="login-page">
    <!-- 动态 Canvas 背景 -->
    <canvas ref="canvasRef" class="bg-canvas" />

    <!-- 渐变背景遮罩 -->
    <div class="bg-overlay" />

    <!-- 登录卡片 -->
    <div class="login-container">
      <div class="login-card">
        <!-- 标题 -->
        <div class="login-header">
          <div class="logo-icon">
            <van-icon name="clock-o" size="28" color="#667eea" />
          </div>
          <h1 class="login-title">工时管理系统</h1>
          <p class="login-subtitle">欢迎回来，请登录您的账号</p>
        </div>

        <!-- 登录表单 -->
        <div class="login-form">
          <!-- 用户名 -->
          <div class="input-group">
            <div class="input-icon">
              <van-icon name="user-o" size="18" color="#667eea" />
            </div>
            <input
              v-model="username"
              type="text"
              placeholder="请输入用户名"
              class="input-field"
              maxlength="50"
              autocomplete="username"
              @keyup.enter="handleLoginClick"
            />
          </div>

          <!-- 密码 -->
          <div class="input-group">
            <div class="input-icon">
              <img
                src="https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=minimalist+lock+icon+for+password+input+field+login+page+dark+theme+glowing+purple+blue+gradient+style+simple+icon+24x24+SVG+style&image_size=square"
                alt="lock"
                class="input-icon-img"
              />
            </div>
            <input
              v-model="password"
              :type="showPwd ? 'text' : 'password'"
              placeholder="请输入密码"
              class="input-field"
              maxlength="50"
              autocomplete="current-password"
              @keyup.enter="handleLoginClick"
            />
            <div class="input-suffix" @click="showPwd = !showPwd">
              <van-icon :name="showPwd ? 'eye-o' : 'closed-eye'" size="18" color="#999" />
            </div>
          </div>

          <!-- 登录按钮 -->
          <button
            class="login-btn"
            :disabled="submitting"
            @click="handleLoginClick"
          >
            <span v-if="submitting" class="btn-loading" />
            <span v-else>登 录</span>
          </button>
        </div>

        <!-- 注册入口 -->
        <div class="login-footer">
          <span class="footer-text">还没有账号？</span>
          <span class="footer-link" @click="goRegister">立即注册</span>
        </div>
      </div>
    </div>

    <!-- 滑块验证码弹窗 -->
    <SlideCaptcha
      v-if="showCaptcha"
      ref="slideCaptchaRef"
      @verified="onCaptchaVerified"
      @close="onCaptchaClose"
    />
  </div>
</template>

<style scoped>
/* ====== 页面容器 ====== */
.login-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: #0f0f1a;
}

/* ====== Canvas 动态背景 ====== */
.bg-canvas {
  position: absolute;
  inset: 0;
  z-index: 0;
}

/* ====== 渐变遮罩 ====== */
.bg-overlay {
  position: absolute;
  inset: 0;
  z-index: 1;
  background:
    radial-gradient(ellipse at 50% 0%, rgba(102, 126, 234, 0.25) 0%, transparent 60%),
    radial-gradient(ellipse at 100% 100%, rgba(118, 75, 162, 0.2) 0%, transparent 50%),
    radial-gradient(ellipse at 0% 100%, rgba(240, 147, 251, 0.15) 0%, transparent 50%);
}

/* ====== 登录容器 ====== */
.login-container {
  position: relative;
  z-index: 2;
  width: 100%;
  max-width: 380px;
  padding: 20px;
}

.login-card {
  background: rgba(255, 255, 255, 0.06);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  padding: 28px 28px 20px;
  box-shadow:
    0 8px 40px rgba(0, 0, 0, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.06);
}

/* ====== 标题区 ====== */
.login-header {
  text-align: center;
  margin-bottom: 24px;
}

.logo-icon {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  background: rgba(102, 126, 234, 0.12);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 12px;
  border: 1px solid rgba(102, 126, 234, 0.2);
}

.login-title {
  font-size: 22px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 6px;
  letter-spacing: 1px;
}

.login-subtitle {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.5);
  margin: 0;
}

/* ====== 表单 ====== */
.login-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.input-group {
  position: relative;
  display: flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  transition: border-color 0.25s, box-shadow 0.25s;
}

.input-group:focus-within {
  border-color: rgba(102, 126, 234, 0.5);
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.input-icon {
  flex-shrink: 0;
  width: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.input-icon-img {
  width: 20px;
  height: 20px;
  object-fit: contain;
  opacity: 0.85;
}

.input-field {
  flex: 1;
  height: 44px;
  background: none;
  border: none;
  outline: none;
  font-size: 15px;
  color: #fff;
  padding: 0 12px 0 0;
  min-width: 0;
}

.input-field::placeholder {
  color: rgba(255, 255, 255, 0.3);
}

.input-suffix {
  flex-shrink: 0;
  width: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

/* 登录按钮 */
.login-btn {
  margin-top: 4px;
  height: 46px;
  border: none;
  border-radius: 12px;
  font-size: 17px;
  font-weight: 600;
  color: #fff;
  cursor: pointer;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  transition: transform 0.2s, box-shadow 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  letter-spacing: 4px;
}

.login-btn:active {
  transform: scale(0.98);
}

.login-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.btn-loading {
  width: 20px;
  height: 20px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ====== 底部 ====== */
.login-footer {
  text-align: center;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.footer-text {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.4);
}

.footer-link {
  font-size: 14px;
  color: #667eea;
  font-weight: 500;
  cursor: pointer;
  margin-left: 4px;
  transition: opacity 0.2s;
}

.footer-link:active {
  opacity: 0.7;
}
</style>
