<template>
  <div class="monthly-detail">
    <van-nav-bar
      :title="`${yearMonth.replace('-', '年')}月 工时详情`"
      left-arrow
      @click-left="router.back()"
    />

    <!-- 加载中 -->
    <div v-if="loading" class="loading-container">
      <van-loading size="24px">加载中...</van-loading>
    </div>

    <template v-else>
      <!-- 用户信息卡片 -->
      <div class="info-card">
        <div class="info-card-header">
          <van-icon name="contact" size="18" color="var(--primary-color)" />
          <span class="info-card-title">用户信息</span>
        </div>
        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">姓名</span>
            <span class="info-value">{{ userInfo.name || userInfo.username || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">部门</span>
            <span class="info-value">{{ userInfo.department || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">岗位</span>
            <span class="info-value">{{ userInfo.position || '-' }}</span>
          </div>
        </div>
      </div>

      <!-- 公司信息卡片 -->
      <div class="info-card">
        <div class="info-card-header">
          <van-icon name="shop-o" size="18" color="var(--primary-color)" />
          <span class="info-card-title">公司信息</span>
        </div>
        <div class="info-grid">
          <div class="info-item info-item-full">
            <span class="info-label">公司名称</span>
            <span class="info-value">{{ userInfo.company || '-' }}</span>
          </div>
          <div v-if="userInfo.project" class="info-item info-item-full">
            <span class="info-label">常用项目</span>
            <span class="info-value">{{ userInfo.project }}</span>
          </div>
        </div>
      </div>

      <!-- 月度概览 -->
      <div class="info-card">
        <div class="info-card-header">
          <van-icon name="chart-trending-o" size="18" color="var(--primary-color)" />
          <span class="info-card-title">月度概览</span>
        </div>
        <div class="summary-metrics">
          <div class="metric-item">
            <span class="metric-value">{{ totalHours.toFixed(1) }}</span>
            <span class="metric-label">总工时(h)</span>
          </div>
          <div class="metric-divider" />
          <div class="metric-item">
            <span class="metric-value">{{ records.length }}</span>
            <span class="metric-label">记录条数</span>
          </div>
          <div class="metric-divider" />
          <div class="metric-item">
            <span class="metric-value">{{ workDays }}</span>
            <span class="metric-label">工作天数</span>
          </div>
        </div>
      </div>

      <!-- 工时记录列表 -->
      <div class="records-section">
        <div class="records-header">
          <van-icon name="notes-o" size="18" color="var(--primary-color)" />
          <span class="records-title">工时记录</span>
          <span class="records-count">{{ records.length }} 条</span>
        </div>

        <!-- 空状态 -->
        <div v-if="records.length === 0" class="records-empty">
          <van-icon name="info-o" size="32" color="var(--text-muted)" />
          <p>该月暂无工时记录</p>
        </div>

        <div v-else class="records-list">
          <div
            v-for="(record, index) in records"
            :key="record.id || index"
            class="record-item"
            @click="router.push(`/records/edit/${record.id}`)"
          >
            <div class="record-item-header">
              <span class="record-date">{{ formatDate(record.workDate) }}</span>
              <span class="record-hours">{{ record.hours }}h</span>
            </div>
            <div class="record-meta">
              <span v-if="record.project" class="record-tag project-tag">{{ record.project }}</span>
              <span v-if="record.company" class="record-tag company-tag">{{ record.company }}</span>
              <span v-if="record.department" class="record-tag dept-tag">{{ record.department }}</span>
            </div>
            <div class="record-content">{{ record.content }}</div>
            <div v-if="record.remark" class="record-remark">
              <van-icon name="bullet-point" size="12" />
              {{ record.remark }}
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showLoadingToast, closeToast } from 'vant'
import { getRecords } from '@/api/records'
import { getConfig } from '@/api/config'
import type { WorkRecord } from '@/api/records'
import type { UserConfig } from '@/api/config'

const route = useRoute()
const router = useRouter()

const yearMonth = route.params.yearMonth as string

const loading = ref(true)
const records = ref<WorkRecord[]>([])
const userInfo = ref<UserConfig>({} as UserConfig)

const totalHours = computed(() => records.value.reduce((sum, r) => sum + (r.hours || 0), 0))

const workDays = computed(() => {
  const days = new Set(records.value.filter(r => !r.isRest).map(r => r.workDate))
  return days.size
})

function formatDate(dateStr: string) {
  if (!dateStr) return ''
  const parts = dateStr.split('-')
  if (parts.length === 3) {
    return `${parseInt(parts[1])}/${parseInt(parts[2])}`
  }
  return dateStr
}

async function loadData() {
  loading.value = true
  showLoadingToast({ message: '加载中...', forbidClick: true })

  try {
    const [configRes, recordsRes] = await Promise.all([
      getConfig(),
      getRecords({ yearMonth })
    ])
    userInfo.value = configRes
    records.value = recordsRes || []
  } catch {
    // 错误已在拦截器中处理
  } finally {
    closeToast()
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.monthly-detail {
  min-height: 100vh;
  background: var(--bg-color, #f5f5f5);
  padding-bottom: 20px;
}

.loading-container {
  display: flex;
  justify-content: center;
  padding: 60px 0;
}

/* ====== 信息卡片 ====== */
.info-card {
  background: var(--card-bg, rgba(255,255,255,0.85));
  backdrop-filter: blur(10px);
  border-radius: 12px;
  margin: 12px 12px 0;
  padding: 14px 16px;
}

.info-card-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(0,0,0,0.04);
}

.info-card-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary, #333);
}

.info-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.info-item {
  flex: 1;
  min-width: calc(50% - 4px);
}

.info-item-full {
  min-width: 100%;
}

.info-label {
  display: block;
  font-size: 10px;
  color: var(--text-muted, #999);
  margin-bottom: 2px;
}

.info-value {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary, #333);
}

/* ====== 月度概览 ====== */
.summary-metrics {
  display: flex;
  align-items: center;
  gap: 0;
}

.metric-item {
  flex: 1;
  text-align: center;
}

.metric-value {
  display: block;
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary, #333);
}

.metric-label {
  display: block;
  font-size: 10px;
  color: var(--text-muted, #999);
  margin-top: 2px;
}

.metric-divider {
  width: 1px;
  height: 28px;
  background: rgba(0,0,0,0.06);
}

/* ====== 记录列表 ====== */
.records-section {
  margin: 12px;
}

.records-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
}

.records-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary, #333);
}

.records-count {
  margin-left: auto;
  font-size: 11px;
  color: var(--text-muted, #999);
}

.records-empty {
  text-align: center;
  padding: 40px 0;
  color: var(--text-muted, #999);
  font-size: 13px;
  background: var(--card-bg, rgba(255,255,255,0.85));
  backdrop-filter: blur(10px);
  border-radius: 12px;
}

.records-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.record-item {
  background: var(--card-bg, rgba(255,255,255,0.85));
  backdrop-filter: blur(10px);
  border-radius: 10px;
  padding: 12px 14px;
  transition: opacity 0.2s;
  cursor: pointer;
}

.record-item:active {
  opacity: 0.7;
}

.record-item-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.record-date {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary, #333);
}

.record-hours {
  font-size: 14px;
  font-weight: 700;
  color: var(--primary-color, #FF8A65);
}

.record-meta {
  display: flex;
  gap: 6px;
  margin-bottom: 6px;
  flex-wrap: wrap;
}

.record-tag {
  display: inline-block;
  font-size: 10px;
  padding: 1px 8px;
  border-radius: 10px;
  font-weight: 500;
}

.project-tag {
  background: rgba(255, 138, 101, 0.1);
  color: var(--primary-color, #FF8A65);
}

.company-tag {
  background: rgba(102, 126, 234, 0.1);
  color: #667eea;
}

.dept-tag {
  background: rgba(0, 200, 117, 0.1);
  color: #00c875;
}

.record-content {
  font-size: 12px;
  color: var(--text-primary, #333);
  line-height: 1.5;
}

.record-remark {
  font-size: 11px;
  color: var(--text-muted, #999);
  margin-top: 4px;
  display: flex;
  align-items: flex-start;
  gap: 3px;
}
</style>
