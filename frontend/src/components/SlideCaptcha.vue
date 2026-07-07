<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, nextTick } from 'vue'
import { getSlideCaptcha, verifySlideCaptcha } from '@/api/auth'
import { showToast } from 'vant'

const emit = defineEmits<{
  verified: [token: string]
  close: []
}>()

// === 状态 ===
const captchaKey = ref('')
const bgImage = ref('')
const pieceImage = ref('')
const pieceY = ref(0)
const pieceSize = ref(44)
const loading = ref(true)
const loadError = ref(false)
const verifying = ref(false)
const verified = ref(false)

// 滑块状态
const sliderValue = ref(0)
const isDragging = ref(false)
const sliderTrackRef = ref<HTMLDivElement>()
const bgImgRef = ref<HTMLImageElement>()

// 滑块轨道尺寸（由图片实际宽度决定）
const trackWidth = ref(260)

/** 拼图块的偏移量（像素） */
const pieceOffset = computed(() => {
  const maxOffset = trackWidth.value - pieceSize.value
  return (sliderValue.value / 100) * maxOffset
})

/** 加载滑块验证码 */
const loadCaptcha = async () => {
  loading.value = true
  loadError.value = false
  sliderValue.value = 0
  verified.value = false
  try {
    const data = await getSlideCaptcha()
    captchaKey.value = data.captchaKey
    bgImage.value = data.bgImage
    pieceImage.value = data.pieceImage
    pieceY.value = data.pieceY
    pieceSize.value = data.pieceSize
  } catch {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

/** 图片加载完成 - 获取实际宽度 */
const onBgLoaded = () => {
  nextTick(() => {
    if (bgImgRef.value) {
      trackWidth.value = bgImgRef.value.clientWidth || bgImgRef.value.naturalWidth || 260
    }
  })
}

/** 图片加载失败 */
const onBgError = () => {
  loadError.value = true
  loading.value = false
}

/** 暴露给父组件的初始化方法 */
const init = () => {
  loadCaptcha()
}

defineExpose({ init })

// === 拖拽事件 ===
const startDrag = (e: MouseEvent | TouchEvent) => {
  if (verified.value || verifying.value) return
  isDragging.value = true
  e.preventDefault()
}

const onDrag = (e: MouseEvent | TouchEvent) => {
  if (!isDragging.value) return
  e.preventDefault()
  const track = sliderTrackRef.value
  if (!track) return

  const rect = track.getBoundingClientRect()
  const clientX = 'touches' in e ? e.touches[0].clientX : e.clientX
  let x = clientX - rect.left

  // 边界限制
  x = Math.max(0, Math.min(x, trackWidth.value - pieceSize.value))
  sliderValue.value = (x / (trackWidth.value - pieceSize.value)) * 100
}

const endDrag = async () => {
  if (!isDragging.value) return
  isDragging.value = false

  // 验证位置
  verifying.value = true
  try {
    // 后端图片原始宽度
    const ORIGINAL_WIDTH = 280
    const displayMaxOffset = trackWidth.value - pieceSize.value
    // 拼图块左边缘在显示坐标中的位置
    const displayLeft = (sliderValue.value / 100) * displayMaxOffset
    // 缩放到原始图片坐标
    const originalLeft = Math.round(displayLeft / trackWidth.value * ORIGINAL_WIDTH)
    const result = await verifySlideCaptcha(captchaKey.value, originalLeft)
    verified.value = true
    emit('verified', result.captchaToken)
  } catch {
    showToast('验证失败，请重试')
    sliderValue.value = 0
    await loadCaptcha()
  } finally {
    verifying.value = false
  }
}

// 全局事件
const onGlobalMove = (e: MouseEvent | TouchEvent) => onDrag(e)
const onGlobalUp = () => { if (isDragging.value) endDrag() }

onMounted(() => {
  loadCaptcha()
  window.addEventListener('mousemove', onGlobalMove)
  window.addEventListener('mouseup', onGlobalUp)
  window.addEventListener('touchmove', onGlobalMove, { passive: false })
  window.addEventListener('touchend', onGlobalUp)
})

onUnmounted(() => {
  window.removeEventListener('mousemove', onGlobalMove)
  window.removeEventListener('mouseup', onGlobalUp)
  window.removeEventListener('touchmove', onGlobalUp)
  window.removeEventListener('touchend', onGlobalUp)
})
</script>

<template>
  <div class="slide-captcha">
    <div class="captcha-panel">
      <!-- 顶部栏 -->
      <div class="captcha-header">
        <span class="captcha-title">请完成安全验证</span>
        <van-icon name="cross" class="captcha-close" @click="$emit('close')" />
      </div>

      <!-- 加载态 -->
      <div v-if="loading" class="captcha-loading">
        <van-icon name="loading" class="spinning" />
        <span>加载验证码...</span>
      </div>

      <!-- 加载错误 -->
      <div v-else-if="loadError" class="captcha-error">
        <van-icon name="warn-o" />
        <span>验证码加载失败</span>
        <button class="retry-btn" @click="loadCaptcha">重试</button>
      </div>

      <!-- 验证码主体 -->
      <template v-else>
        <div class="captcha-image-wrap">
          <img
            ref="bgImgRef"
            :src="bgImage"
            alt="验证码背景"
            class="captcha-bg"
            @load="onBgLoaded"
            @error="onBgError"
          />

          <!-- 拼图块 -->
          <div
            class="captcha-piece"
            :style="{
              width: pieceSize + 'px',
              height: pieceSize + 'px',
              top: pieceY + 'px',
              left: pieceOffset + 'px',
              backgroundImage: `url(${pieceImage})`,
              backgroundSize: `${pieceSize}px ${pieceSize}px`
            }"
          />
        </div>

        <!-- 滑块轨道 -->
        <div
          ref="sliderTrackRef"
          class="slider-track"
          :class="{ 'slider-verified': verified }"
          @mousedown="startDrag"
          @touchstart.prevent="startDrag"
        >
          <div class="slider-fill" :style="{ width: sliderValue + '%' }" />
          <div
            class="slider-thumb"
            :style="{ left: sliderValue + '%' }"
            :class="{ dragging: isDragging, verified: verified }"
          >
            <van-icon v-if="!verified && !verifying" name="arrow" :class="{ 'arrow-pulse': !isDragging }" />
            <van-icon v-else-if="verifying" name="replay" class="spinning" />
            <van-icon v-else name="success" />
          </div>
          <span v-if="!verified && !isDragging" class="slider-hint">拖动滑块完成拼图</span>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.slide-captcha {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.55);
  backdrop-filter: blur(4px);
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.captcha-panel {
  width: 320px;
  max-width: 90vw;
  background: #1a1a2e;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.5);
  overflow: hidden;
  animation: slideUp 0.25s ease;
}

@keyframes slideUp {
  from { transform: translateY(20px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

.captcha-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 18px 12px;
}

.captcha-title {
  font-size: 15px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.85);
}

.captcha-close {
  font-size: 18px;
  color: rgba(255, 255, 255, 0.35);
  cursor: pointer;
  padding: 4px;
  transition: color 0.2s;
}

.captcha-close:hover {
  color: rgba(255, 255, 255, 0.6);
}

/* 加载态 */
.captcha-loading,
.captcha-error {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 120px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.4);
}

.captcha-error {
  flex-direction: column;
  gap: 12px;
  color: rgba(255, 255, 255, 0.5);
}

.captcha-error .van-icon {
  font-size: 28px;
  color: rgba(255, 255, 255, 0.2);
}

.retry-btn {
  padding: 6px 20px;
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.06);
  color: rgba(255, 255, 255, 0.6);
  font-size: 13px;
  cursor: pointer;
  transition: background 0.2s;
}

.retry-btn:hover {
  background: rgba(255, 255, 255, 0.1);
}

/* 图片区 */
.captcha-image-wrap {
  position: relative;
  margin: 0 18px;
  border-radius: 10px;
  overflow: hidden;
}

.captcha-bg {
  display: block;
  width: 100%;
  height: auto;
}

.captcha-piece {
  position: absolute;
  border-radius: 6px;
  background-repeat: no-repeat;
  pointer-events: none;
  box-shadow:
    0 2px 8px rgba(0, 0, 0, 0.35),
    0 0 0 1.5px rgba(255, 255, 255, 0.5);
  z-index: 1;
}

/* 滑块轨道 */
.slider-track {
  position: relative;
  margin: 14px 18px 20px;
  height: 44px;
  background: rgba(255, 255, 255, 0.06);
  border-radius: 22px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  cursor: pointer;
  overflow: hidden;
  transition: border-color 0.3s;
}

.slider-fill {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  background: linear-gradient(90deg, rgba(102, 126, 234, 0.2), rgba(118, 75, 162, 0.2));
  border-radius: 22px;
  transition: width 0.05s;
  pointer-events: none;
}

.slider-hint {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  font-size: 13px;
  color: rgba(255, 255, 255, 0.25);
  white-space: nowrap;
  pointer-events: none;
}

/* 滑块按钮 */
.slider-thumb {
  position: absolute;
  top: -1px;
  width: 46px;
  height: 46px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: grab;
  z-index: 2;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.4);
  transition: box-shadow 0.2s, transform 0.15s;
  transform: translateX(-50%);
  color: #fff;
  font-size: 18px;
}

.slider-thumb.dragging {
  cursor: grabbing;
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.6);
  transform: translateX(-50%) scale(1.06);
}

.slider-thumb.verified {
  background: linear-gradient(135deg, #52c41a, #73d13d);
  box-shadow: 0 2px 8px rgba(82, 196, 26, 0.4);
  cursor: default;
}

/* 验证成功 */
.slider-verified {
  border-color: rgba(82, 196, 26, 0.3) !important;
}

/* 动画 */
.arrow-pulse {
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 0.5; transform: translateX(-1px); }
  50% { opacity: 1; transform: translateX(2px); }
}

.spinning {
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
