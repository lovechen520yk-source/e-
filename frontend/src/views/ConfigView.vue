<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import { getConfig } from '@/api/config'
import { saveConfig } from '@/api/config'
import { getRecords } from '@/api/records'
import { getSalary } from '@/api/stats'
import type { UserConfig } from '@/api/config'
import type { WorkRecord } from '@/api/records'

const router = useRouter()

const user = ref<UserConfig>({
  username: '',
  name: '',
  password: '',
  avatar: '',
  company: '',
  department: '',
  position: '',
  project: '',
  content: '',
  salaryMode: 'HOURLY',
  hourlyRate: 30,
  fixedMonthlySalary: 8000,
  overtimeHourlyRate: 45,
  allowance: 0,
  performanceBonus: 0,
  deduction: 0
})

/** 当月所有记录 */
const currentMonthRecords = ref<WorkRecord[]>([])

/** 后端计算的薪资 */
const backendSalary = ref<number>(0)
const salaryDescription = ref('')
const salaryLoading = ref(false)

/** 当前年月 YYYY-MM */
const currentYearMonth = () => {
  const d = new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  return `${y}-${m}`
}

/** 本月总工时（不含休息） */
const totalHours = computed(() => {
  return currentMonthRecords.value
    .filter(r => !r.isRest)
    .reduce((sum, r) => sum + (r.hours || 0), 0)
})

/** 预期薪资（使用后端计算结果） */
const expectedSalary = computed(() => {
  return backendSalary.value
})

/** 实际预期薪资 = 预期薪资 + 津贴 + 绩效 - 扣除 */
const actualSalary = computed(() => {
  const a = Number(user.value.allowance) || 0
  const p = Number(user.value.performanceBonus) || 0
  const d = Number(user.value.deduction) || 0
  return expectedSalary.value + a + p - d
})

const avatarInputRef = ref<HTMLInputElement>()

/** 处理头像上传 */
const handleAvatarUpload = (e: Event) => {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return
  if (file.size > 2 * 1024 * 1024) {
    showToast('图片大小不能超过2MB')
    target.value = ''
    return
  }
  const reader = new FileReader()
  reader.onload = async () => {
    const base64 = reader.result as string
    user.value.avatar = base64
    // 自动保存
    try {
      await saveConfig(user.value)
      showToast('头像已更新')
    } catch {
      showToast('上传失败')
    }
  }
  reader.readAsDataURL(file)
  target.value = ''
}

const triggerAvatarUpload = () => {
  avatarInputRef.value?.click()
}

/** 跳转编辑个人信息 */
const goEdit = () => {
  router.push('/config/edit')
}

/** 退出登录 */
const handleLogout = () => {
  showConfirmDialog({
    title: '退出登录',
    message: '确定要退出当前账号吗？',
    confirmButtonColor: '#ff6b6b'
  }).then(() => {
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    router.replace('/login')
  }).catch(() => {
    // 取消退出
  })
}

/** 从后端获取准确薪资 */
const loadSalary = async () => {
  salaryLoading.value = true
  try {
    const ym = currentYearMonth()
    const res: any = await getSalary({ yearMonth: ym })
    if (res && res.salary !== undefined) {
      backendSalary.value = Number(res.salary) || 0
      salaryDescription.value = res.description || ''
    }
  } catch (e) {
    console.error('获取薪资失败', e)
  } finally {
    salaryLoading.value = false
  }
}

onMounted(async () => {
  try {
    const data = await getConfig()
    if (data) {
      user.value = { ...user.value, ...data }
    }
  } catch (e) {
    console.error('加载配置失败', e)
  }

  // 加载当月工时记录
  try {
    const res: any = await getRecords({ yearMonth: currentYearMonth() })
    currentMonthRecords.value = Array.isArray(res) ? res : (res?.data || res?.records || [])
  } catch (e) {
    console.error('加载工时记录失败', e)
  }

  // 从后端获取准确薪资
  await loadSalary()
})
</script>

<template>
  <div class="config-page">
    <!-- ====== 用户信息栏（渐变背景 + 左右布局） ====== -->
    <div class="user-header">
      <div class="header-bg" />

      <!-- 编辑按钮 -->
      <div class="edit-btn-wrapper">
        <van-icon name="edit" class="edit-btn" @click="goEdit" />
      </div>

      <div class="user-content">
        <!-- 左边：头像 -->
        <div class="avatar-wrapper" @click="triggerAvatarUpload">
          <img
            v-if="user.avatar"
            :src="user.avatar"
            class="avatar-img"
          />
          <div v-else class="avatar-placeholder">
            <van-icon name="photograph" size="32" color="rgba(255,255,255,0.6)" />
          </div>
          <div class="avatar-overlay">
            <van-icon name="camera" size="18" color="#fff" />
          </div>
        </div>

        <!-- 右边：用户名信息 -->
        <div class="user-info">
          <h2 class="user-name">{{ user.name || '未设置姓名' }}</h2>
          <p class="user-subtitle">@{{ user.username || '未设置用户名' }}</p>
        </div>
      </div>
    </div>

    <!-- ====== 薪资概览（计算属性，不做存储） ====== -->
    <div class="salary-card">
      <div class="salary-card-header">
        <van-icon name="balance-o" />
        <span>本月薪资概览</span>
        <span class="salary-card-sub">{{ currentYearMonth() }}</span>
      </div>
      <div class="salary-card-body">
        <div class="salary-metrics">
          <div class="metric-item">
            <span class="metric-value">{{ totalHours.toFixed(1) }}</span>
            <span class="metric-label">总工时(h)</span>
          </div>
          <div class="metric-divider" />
          <div class="metric-item">
            <span class="metric-value">¥{{ expectedSalary.toFixed(0) }}</span>
            <span class="metric-label">预期薪资</span>
          </div>
          <div class="metric-divider" />
          <div class="metric-item">
            <span class="metric-value actual">¥{{ actualSalary.toFixed(0) }}</span>
            <span class="metric-label">实际薪资</span>
          </div>
        </div>
        <div v-if="salaryDescription" class="salary-desc">
          {{ salaryDescription }}
        </div>
      </div>
    </div>

    <!-- ====== 工作信息（只读卡片） ====== -->
    <div class="work-card">
      <div class="work-card-header">
        <van-icon name="info-o" />
        <span>工作信息</span>
      </div>
      <div class="work-card-body">
        <div class="work-row">
          <span class="work-label">公司</span>
          <span class="work-value">{{ user.company || '-' }}</span>
        </div>
        <div class="work-row">
          <span class="work-label">部门</span>
          <span class="work-value">{{ user.department || '-' }}</span>
        </div>
        <div class="work-row">
          <span class="work-label">岗位</span>
          <span class="work-value">{{ user.position || '-' }}</span>
        </div>
        <div class="work-row">
          <span class="work-label">常用项目</span>
          <span class="work-value">{{ user.project || '-' }}</span>
        </div>
        <div class="work-row">
          <span class="work-label">工作内容</span>
          <span class="work-value work-content">{{ user.content || '-' }}</span>
        </div>
      </div>
    </div>

    <!-- ====== 薪资设置 ====== -->
    <div class="card-glass">
      <div class="salary-header">
        <van-icon name="gold-coin-o" />
        <span>薪资设置</span>
      </div>
      <div class="salary-body">
        <div class="salary-row">
          <span class="salary-label">薪资模式</span>
          <van-tag :type="user.salaryMode === 'HOURLY' ? 'primary' : 'success'" size="medium">
            {{ user.salaryMode === 'HOURLY' ? '小时薪资' : '固定月薪' }}
          </van-tag>
        </div>
        <div v-if="user.salaryMode === 'HOURLY'" class="salary-row">
          <span class="salary-label">小时薪资</span>
          <span class="salary-value">¥{{ user.hourlyRate || 0 }}/小时</span>
        </div>
        <div v-if="user.salaryMode === 'FIXED'" class="salary-row">
          <span class="salary-label">固定月薪</span>
          <span class="salary-value">¥{{ user.fixedMonthlySalary || 0 }}/月</span>
        </div>
        <div class="salary-row">
          <span class="salary-label">加班时薪</span>
          <span class="salary-value">¥{{ user.overtimeHourlyRate || 0 }}/小时</span>
        </div>
        <div class="salary-row">
          <span class="salary-label">月度津贴</span>
          <span class="salary-value">¥{{ user.allowance || 0 }}</span>
        </div>
        <div class="salary-row">
          <span class="salary-label">月度绩效</span>
          <span class="salary-value">¥{{ user.performanceBonus || 0 }}</span>
        </div>
        <div class="salary-row">
          <span class="salary-label">扣除</span>
          <span class="salary-value deduction">-¥{{ user.deduction || 0 }}</span>
        </div>
      </div>
    </div>

    <!-- <div style="margin: 16px;">
      <van-button round block type="primary" native-type="submit" color="var(--primary-color)">
        保存配置
      </van-button>
    </div> -->

    <!-- ====== 退出登录 ====== -->
    <div class="logout-section">
      <van-button
        round
        block
        plain
        icon="logout"
        color="#ff6b6b"
        @click="handleLogout"
      >
        退出登录
      </van-button>
    </div>

    <!-- 隐藏的文件输入 -->
    <input
      ref="avatarInputRef"
      type="file"
      accept="image/*"
      style="display: none"
      @change="handleAvatarUpload"
    />
  </div>
</template>

<style scoped>
/* ====== 页面容器 ====== */
.config-page {
  min-height: 100vh;
  background: var(--bg-color);
}

/* ====== 用户头部 ====== */
.user-header {
  position: relative;
  padding: 40px 24px 32px;
  overflow: hidden;
}

.header-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%);
  z-index: 0;
}

.header-bg::after {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 20% 80%, rgba(255,255,255,0.15) 0%, transparent 50%),
    radial-gradient(circle at 80% 20%, rgba(255,255,255,0.1) 0%, transparent 50%);
}

.edit-btn-wrapper {
  position: absolute;
  top: 12px;
  right: 16px;
  z-index: 2;
}

.edit-btn {
  font-size: 22px;
  color: rgba(255,255,255,0.85);
  padding: 6px;
  background: rgba(255,255,255,0.15);
  border-radius: 50%;
  backdrop-filter: blur(4px);
}

/* 用户内容 - 左右布局 */
.user-content {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 20px;
}

/* 头像 */
.avatar-wrapper {
  position: relative;
  width: 72px;
  height: 72px;
  border-radius: 50%;
  flex-shrink: 0;
  cursor: pointer;
  border: 3px solid rgba(255,255,255,0.5);
  box-shadow: 0 4px 20px rgba(0,0,0,0.15);
}

.avatar-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: rgba(255,255,255,0.2);
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: rgba(0,0,0,0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.25s;
}

.avatar-wrapper:hover .avatar-overlay,
.avatar-wrapper:active .avatar-overlay {
  opacity: 1;
}

/* 用户信息 */
.user-info {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: 22px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 4px;
  text-shadow: 0 2px 8px rgba(0,0,0,0.15);
}

.user-subtitle {
  font-size: 14px;
  color: rgba(255,255,255,0.75);
  margin: 0;
  font-weight: 400;
}

/* ====== 工作信息卡片 ====== */
.work-card {
  margin: 4px 16px 16px;
  padding: 20px;
  background: rgba(255,255,255,0.92);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-radius: 16px;
  border: 1px solid rgba(255,255,255,0.5);
  box-shadow: 0 8px 32px rgba(0,0,0,0.08);
  position: relative;
  z-index: 2;
}

.work-card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  padding-bottom: 14px;
  border-bottom: 1px solid rgba(0,0,0,0.05);
  margin-bottom: 14px;
}

.work-card-header .van-icon {
  font-size: 18px;
  color: var(--primary-color);
}

.work-card-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.work-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.work-label {
  flex-shrink: 0;
  width: 68px;
  font-size: 13px;
  color: var(--text-muted);
  font-weight: 500;
  letter-spacing: 0.5px;
}

.work-value {
  flex: 1;
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 500;
  word-break: break-word;
}

.work-content {
  line-height: 1.6;
  color: var(--text-secondary);
}

/* ====== 薪资设置 ====== */
.salary-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  padding-bottom: 14px;
  border-bottom: 1px solid rgba(0,0,0,0.05);
  margin-bottom: 14px;
}

.salary-header .van-icon {
  font-size: 18px;
  color: var(--primary-color);
}

.salary-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.salary-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.salary-label {
  font-size: 14px;
  color: var(--text-secondary);
}

.salary-value {
  font-size: 15px;
  font-weight: 600;
  color: var(--primary-color);
}

.salary-value.deduction {
  color: #ff6b6b;
}

/* ====== 薪资概览卡片 ====== */
.salary-card {
  margin: 12px 16px 16px;
  padding: 14px 16px;
  background: linear-gradient(135deg, #fefcfb 0%, #fdf8f5 100%);
  border-radius: 14px;
  box-shadow: 0 4px 20px rgba(255, 138, 101, 0.08);
  position: relative;
  overflow: hidden;
  border: 1px solid rgba(255, 138, 101, 0.08);
}

.salary-card::before {
  content: '';
  position: absolute;
  top: -40%;
  right: -15%;
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255,138,101,0.08), transparent 70%);
  pointer-events: none;
}

.salary-card-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.salary-card-header .van-icon {
  font-size: 15px;
  color: #FF8A65;
}

.salary-card-sub {
  margin-left: auto;
  font-size: 10px;
  font-weight: 400;
  color: var(--text-muted);
}

.salary-card-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.salary-metrics {
  display: flex;
  align-items: center;
  justify-content: space-around;
  padding: 2px 0;
}

.metric-item {
  text-align: center;
}

.metric-value {
  display: block;
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: 0.5px;
}

.metric-value.actual {
  color: #FF8A65;
}

.metric-label {
  display: block;
  font-size: 10px;
  color: var(--text-muted);
  margin-top: 2px;
  font-weight: 500;
}

.metric-divider {
  width: 1px;
  height: 28px;
  background: rgba(0,0,0,0.08);
}

.salary-desc {
  font-size: 11px;
  color: var(--text-muted);
  text-align: center;
  padding-top: 6px;
  border-top: 1px solid rgba(0,0,0,0.04);
  margin-top: 6px;
  line-height: 1.5;
}

/* ====== 退出按钮 ====== */
.logout-section {
  padding: 24px 16px 40px;
}

.logout-section .van-button {
  border-color: rgba(255, 107, 107, 0.35);
  font-size: 15px;
  letter-spacing: 1px;
}
</style>
