<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showLoadingToast, closeToast } from 'vant'
import { register } from '@/api/auth'
import SlideCaptcha from '@/components/SlideCaptcha.vue'

const router = useRouter()

const username = ref('')
const password = ref('')
const confirmPwd = ref('')
const showPwd = ref(false)
const showConfirmPwd = ref(false)
const submitting = ref(false)
const showCaptcha = ref(false)

const slideCaptchaRef = ref<InstanceType<typeof SlideCaptcha>>()

/** 点击注册 - 验证表单后弹出滑块验证 */
const handleRegisterClick = () => {
  if (!username.value.trim()) {
    showToast('请输入用户名')
    return
  }
  if (username.value.trim().length < 3) {
    showToast('用户名至少3个字符')
    return
  }
  if (!password.value) {
    showToast('请输入密码')
    return
  }
  if (password.value.length < 6) {
    showToast('密码长度不能少于6位')
    return
  }
  if (password.value !== confirmPwd.value) {
    showToast('两次密码输入不一致')
    return
  }
  showCaptcha.value = true
}

/** 滑块验证通过回调 - 自动提交注册 */
const onCaptchaVerified = async (token: string) => {
  showCaptcha.value = false

  submitting.value = true
  showLoadingToast({ message: '注册中...', forbidClick: true })

  try {
    const data = await register({
      username: username.value.trim(),
      password: password.value,
      captchaToken: token
    })

    localStorage.setItem('token', data.token)
    localStorage.setItem('username', data.username)

    closeToast()
    showToast('注册成功')

    router.replace('/config')
  } catch {
    closeToast()
    showToast('注册失败，请重试')
  } finally {
    submitting.value = false
  }
}

/** 关闭验证码弹窗 */
const onCaptchaClose = () => {
  showCaptcha.value = false
}

// 密码强度计算
const pwdStrength = computed(() => {
  const pwd = password.value
  if (!pwd) return { level: 0, label: '', color: '' }
  let score = 0
  if (pwd.length >= 8) score++
  if (pwd.length >= 12) score++
  if (/[a-z]/.test(pwd) && /[A-Z]/.test(pwd)) score++
  if (/\d/.test(pwd)) score++
  if (/[^a-zA-Z0-9]/.test(pwd)) score++
  if (score <= 1) return { level: 1, label: '弱', color: '#ff6b6b' }
  if (score <= 3) return { level: 2, label: '中', color: '#ffa94d' }
  return { level: 3, label: '强', color: '#69db7c' }
})

/** 跳转登录 */
const goLogin = () => {
  router.replace('/login')
}

// ====== 动态粒子背景（复用登录页） ======
const canvasRef = ref<HTMLCanvasElement>()
let animationId = 0

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

  const particles: { x: number; y: number; vx: number; vy: number; r: number; alpha: number }[] = []
  const count = Math.min(60, Math.floor(canvas.width / 12))

  for (let i = 0; i < count; i++) {
    particles.push({
      x: Math.random() * canvas.width,
      y: Math.random() * canvas.height,
      vx: (Math.random() - 0.5) * 0.6,
      vy: (Math.random() - 0.5) * 0.6,
      r: Math.random() * 2.5 + 0.8,
      alpha: Math.random() * 0.4 + 0.08
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
      ctx.fillStyle = `rgba(118, 75, 162, ${p.alpha})`
      ctx.fill()
    }

    for (let i = 0; i < particles.length; i++) {
      for (let j = i + 1; j < particles.length; j++) {
        const dx = particles[i].x - particles[j].x
        const dy = particles[i].y - particles[j].y
        const dist = Math.sqrt(dx * dx + dy * dy)
        if (dist < 100) {
          ctx.beginPath()
          ctx.moveTo(particles[i].x, particles[i].y)
          ctx.lineTo(particles[j].x, particles[j].y)
          ctx.strokeStyle = `rgba(118, 75, 162, ${0.1 * (1 - dist / 100)})`
          ctx.lineWidth = 0.5
          ctx.stroke()
        }
      }
    }

    animationId = requestAnimationFrame(animate)
  }

  animate()

  const onResize = () => {
    canvas.width = window.innerWidth
    canvas.height = window.innerHeight
  }
  window.addEventListener('resize', onResize)
}
</script>

<template>
  <div class="register-page">
    <canvas ref="canvasRef" class="bg-canvas" />
    <div class="bg-overlay" />

    <div class="register-container">
      <div class="register-card">
        <!-- 标题 -->
        <div class="register-header">
          <div class="logo-icon">
            <van-icon name="friends-o" size="28" color="#764ba2" />
          </div>
          <h1 class="register-title">创建账号</h1>
          <p class="register-subtitle">注册后即可开始记录工时</p>
        </div>

        <!-- 注册表单 -->
        <div class="register-form">
          <!-- 用户名 -->
          <div class="input-group">
            <div class="input-icon">
              <van-icon name="user-o" size="18" color="#764ba2" />
            </div>
            <input
              v-model="username"
              type="text"
              placeholder="请输入用户名（至少3个字符）"
              class="input-field"
              maxlength="50"
              autocomplete="username"
              @keyup.enter="handleRegisterClick"
            />
          </div>

          <!-- 密码 -->
          <div class="input-group">
            <div class="input-icon">
              <img
                src="https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=minimalist+lock+icon+password+input+dark+purple+glowing+gradient+style+24x24+SVG+simple&image_size=square"
                alt="lock"
                class="input-icon-img"
              />
            </div>
            <input
              v-model="password"
              :type="showPwd ? 'text' : 'password'"
              placeholder="请输入密码（至少6位）"
              class="input-field"
              maxlength="50"
              autocomplete="new-password"
              @keyup.enter="handleRegisterClick"
            />
            <div class="input-suffix" @click="showPwd = !showPwd">
              <van-icon :name="showPwd ? 'eye-o' : 'closed-eye'" size="18" color="#999" />
            </div>
          </div>

          <!-- 密码强度 -->
          <div v-if="password" class="strength-bar">
            <div class="strength-track">
              <div
                class="strength-fill"
                :style="{
                  width: pwdStrength.level * 33.3 + '%',
                  background: pwdStrength.color
                }"
              />
            </div>
            <span class="strength-label" :style="{ color: pwdStrength.color }">
              密码强度：{{ pwdStrength.label }}
            </span>
          </div>

          <!-- 确认密码 -->
          <div class="input-group">
            <div class="input-icon">
              <img
                src="https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=minimalist+checkmark+verified+icon+confirm+password+input+dark+purple+glowing+gradient+style+24x24+SVG+simple&image_size=square"
                alt="checked"
                class="input-icon-img"
              />
            </div>
            <input
              v-model="confirmPwd"
              :type="showConfirmPwd ? 'text' : 'password'"
              placeholder="请再次输入密码"
              class="input-field"
              maxlength="50"
              autocomplete="new-password"
              @keyup.enter="handleRegisterClick"
            />
            <div class="input-suffix" @click="showConfirmPwd = !showConfirmPwd">
              <van-icon :name="showConfirmPwd ? 'eye-o' : 'closed-eye'" size="18" color="#999" />
            </div>
          </div>

          <!-- 注册按钮 -->
          <button
            class="register-btn"
            :disabled="submitting"
            @click="handleRegisterClick"
          >
            <span v-if="submitting" class="btn-loading" />
            <span v-else>注 册</span>
          </button>
        </div>

        <!-- 登录入口 -->
        <div class="register-footer">
          <span class="footer-text">已有账号？</span>
          <span class="footer-link" @click="goLogin">去登录</span>
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
.register-page {
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

.bg-overlay {
  position: absolute;
  inset: 0;
  z-index: 1;
  background:
    radial-gradient(ellipse at 50% 0%, rgba(118, 75, 162, 0.25) 0%, transparent 60%),
    radial-gradient(ellipse at 100% 100%, rgba(102, 126, 234, 0.2) 0%, transparent 50%),
    radial-gradient(ellipse at 0% 100%, rgba(240, 147, 251, 0.15) 0%, transparent 50%);
}

/* ====== 注册容器 ====== */
.register-container {
  position: relative;
  z-index: 2;
  width: 100%;
  max-width: 380px;
  padding: 20px;
}

.register-card {
  background: rgba(255, 255, 255, 0.06);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  padding: 36px 28px 28px;
  box-shadow:
    0 8px 40px rgba(0, 0, 0, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.06);
}

/* ====== 标题区 ====== */
.register-header {
  text-align: center;
  margin-bottom: 32px;
}

.logo-icon {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: rgba(118, 75, 162, 0.12);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  border: 1px solid rgba(118, 75, 162, 0.2);
}

.register-title {
  font-size: 24px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 8px;
  letter-spacing: 1px;
}

.register-subtitle {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.5);
  margin: 0;
}

/* ====== 表单 ====== */
.register-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
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
  border-color: rgba(118, 75, 162, 0.5);
  box-shadow: 0 0 0 3px rgba(118, 75, 162, 0.1);
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
  height: 48px;
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

/* 密码强度条 */
.strength-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 4px;
  margin-top: -4px;
}

.strength-track {
  flex: 1;
  height: 4px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 2px;
  overflow: hidden;
}

.strength-fill {
  height: 100%;
  border-radius: 2px;
  transition: width 0.3s, background 0.3s;
}

.strength-label {
  font-size: 12px;
  flex-shrink: 0;
  min-width: 80px;
  text-align: right;
}

/* 注册按钮 */
.register-btn {
  margin-top: 4px;
  height: 50px;
  border: none;
  border-radius: 12px;
  font-size: 17px;
  font-weight: 600;
  color: #fff;
  cursor: pointer;
  background: linear-gradient(135deg, #764ba2 0%, #667eea 100%);
  transition: transform 0.2s, box-shadow 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  letter-spacing: 4px;
}

.register-btn:active {
  transform: scale(0.98);
}

.register-btn:disabled {
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
.register-footer {
  text-align: center;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.footer-text {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.4);
}

.footer-link {
  font-size: 14px;
  color: #764ba2;
  font-weight: 500;
  cursor: pointer;
  margin-left: 4px;
  transition: opacity 0.2s;
}

.footer-link:active {
  opacity: 0.7;
}
</style>
