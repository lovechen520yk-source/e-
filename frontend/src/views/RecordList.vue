<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { showToast, showLoadingToast, closeToast } from 'vant'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import type { WorkRecord } from '@/api/records'
import { getRecords, addRecord, updateRecord } from '@/api/records'
import { getConfig } from '@/api/config'
import type { UserConfig } from '@/api/config'
import { getCurrentYearMonth } from '@/utils/format'
import StatsCard from '@/components/StatsCard.vue'
import type { MonthlySummary } from '@/api/monthly-summary'
import {
  getSummariesByYear,
  getSummaryYears,
  tryGenerateLastMonth,
  exportYearSummary
} from '@/api/monthly-summary'

// ====== 年月状态 ======
const { year: cy, month: cm } = getCurrentYearMonth()
const year = ref(cy)
const month = ref(cm)
const yearMonth = computed(() => `${year.value}年${month.value}月`)
const formatYearMonth = (y: number, m: number) => `${y}-${String(m).padStart(2, '0')}`

const router = useRouter()

/** 跳转到月度详情 */
const goToMonthlyDetail = (yearMonth: string) => {
  router.push(`/records/monthly-detail/${yearMonth}`)
}

// ====== 月度总结状态 ======
const summaries = ref<MonthlySummary[]>([])
const summaryYears = ref<number[]>([])
const activeSummaryYear = ref(new Date().getFullYear())
const summaryLoading = ref(false)

/** 按年加载月度总结 */
const loadSummaries = async () => {
  summaryLoading.value = true
  try {
    summaries.value = await getSummariesByYear(activeSummaryYear.value) || []
  } catch { /* 忽略 */ }
  finally { summaryLoading.value = false }
}

/** 获取所有年份 */
const loadSummaryYears = async () => {
  try {
    const years = await getSummaryYears()
    summaryYears.value = years || [new Date().getFullYear()]
  } catch { /* 忽略 */ }
}

/** 切换年份Tab */
const onSummaryYearChange = () => {
  loadSummaries()
}

/** 导出当前年总结 */
const handleExportYear = () => {
  exportYearSummary(activeSummaryYear.value)
}

/** 每月1日自动生成上月总结 */
const autoGenerateLastMonth = async () => {
  const today = new Date()
  if (today.getDate() !== 1) return
  try {
    await tryGenerateLastMonth()
    // 刷新总结数据和年份
    await loadSummaryYears()
    await loadSummaries()
  } catch { /* 忽略 */ }
}

// ====== 用户配置（信息卡片用） ======
const userConfig = ref<UserConfig>({
  company: '', department: '', position: '',
  salaryMode: 'HOURLY', hourlyRate: 0, fixedMonthlySalary: 0, overtimeHourlyRate: 0
})

// ====== 记录数据 ======
const records = ref<WorkRecord[]>([])
const loading = ref(false)

/** 按日期映射：dateStr → WorkRecord */
const recordMap = computed(() => {
  const map = new Map<string, WorkRecord>()
  for (const r of records.value) {
    map.set(r.workDate, r)
  }
  return map
})

// ====== 统计 ======
const stats = computed(() => {
  const total = records.value.reduce((s, r) => s + r.hours, 0)
  const days = new Set(records.value.filter(r => !r.isRest).map(r => r.workDate)).size
  const projects = new Set(records.value.map(r => r.project)).size
  return { totalHours: Math.round(total * 10) / 10, totalDays: days, totalProjects: projects }
})

// ====== 日历数据 ======
const weekDays = ['日', '一', '二', '三', '四', '五', '六']

interface DayCell {
  day: number
  dateStr: string
  isToday: boolean
  isFuture: boolean
  record: WorkRecord | undefined
  hours: number
}

const daysInMonth = computed(() => dayjs(`${year.value}-${month.value}-01`).daysInMonth())
const firstDayOfWeek = computed(() => dayjs(`${year.value}-${month.value}-01`).day())

const todayStr = dayjs().format('YYYY-MM-DD')

const calendarDays = computed(() => {
  const cells: DayCell[] = []
  // 填充前面的空白天数
  for (let i = 0; i < firstDayOfWeek.value; i++) {
    cells.push({ day: 0, dateStr: '', isToday: false, isFuture: false, record: undefined, hours: 0 })
  }
  for (let d = 1; d <= daysInMonth.value; d++) {
    const dateStr = `${year.value}-${String(month.value).padStart(2, '0')}-${String(d).padStart(2, '0')}`
    const record = recordMap.value.get(dateStr)
    cells.push({
      day: d,
      dateStr,
      isToday: dateStr === todayStr,
      isFuture: dayjs(dateStr).isAfter(dayjs(), 'day'),
      record,
      hours: record ? record.hours : 0
    })
  }
  return cells
})

// ====== 弹窗编辑状态 ======
const showPopup = ref(false)
const selectedDate = ref('')
const isEdit = ref(false) // 该日是否有已有记录

const editForm = ref({
  project: '',
  content: '',
  hours: 1,
  remark: '',
  isRest: false
})

const submitting = ref(false)

/** 点击某一天 */
const openDay = (cell: DayCell) => {
  if (cell.isFuture) return // 未来日期不可编辑
  selectedDate.value = cell.dateStr

  if (cell.record) {
    // 编辑已有记录
    isEdit.value = true
    editForm.value = {
      project: cell.record.project || '',
      content: cell.record.content || '',
      hours: cell.record.hours,
      remark: cell.record.remark || '',
      isRest: cell.record.isRest ?? false
    }
  } else {
    // 新建记录
    isEdit.value = false
    // 从用户配置预填
    editForm.value = {
      project: '',
      content: '',
      hours: 1,
      remark: '',
      isRest: false
    }
  }
  showPopup.value = true
}

/** 提交弹窗中的表单 */
const submitPopupForm = async () => {
  if (!editForm.value.isRest) {
    if (!editForm.value.content) { showToast('请输入工作内容'); return }
    if (!editForm.value.project) { showToast('请选择项目'); return }
  }
  if (!editForm.value.isRest && (!editForm.value.hours || editForm.value.hours <= 0)) {
    showToast('请输入有效工时')
    return
  }

  submitting.value = true
  showLoadingToast({ message: '保存中...', forbidClick: true })

  try {
    const payload = {
      workDate: selectedDate.value,
      company: userConfig.value.company || '',
      department: userConfig.value.department || '',
      position: userConfig.value.position || '',
      project: editForm.value.isRest ? '' : editForm.value.project,
      content: editForm.value.isRest ? '休息' : editForm.value.content,
      hours: editForm.value.isRest ? 0 : editForm.value.hours,
      remark: editForm.value.remark,
      isRest: editForm.value.isRest
    }

    if (isEdit.value) {
      const existing = recordMap.value.get(selectedDate.value)
      if (existing?.id) {
        await updateRecord(existing.id, payload as WorkRecord)
        showToast('更新成功')
      }
    } else {
      await addRecord(payload as WorkRecord)
      showToast('新增成功')
    }

    showPopup.value = false
    await loadData() // 刷新日历数据
  } catch (e) {
    console.error('保存失败', e)
  } finally {
    submitting.value = false
    closeToast()
  }
}

/** 删除当前日期的记录 */
const deleteCurrentRecord = async () => {
  const record = recordMap.value.get(selectedDate.value)
  if (!record?.id) return

  // 使用 vant 的 showConfirmDialog
  const { showConfirmDialog } = await import('vant')
  try {
    await showConfirmDialog({
      title: '确认删除',
      message: `确定删除 ${selectedDate.value} 的工时记录吗？`,
      confirmButtonColor: '#F44336'
    })
    const { deleteRecord } = await import('@/api/records')
    await deleteRecord(record.id)
    showToast('删除成功')
    showPopup.value = false
    await loadData()
  } catch {
    // 取消删除
  }
}

// ====== 数据加载 ======
const loadData = async () => {
  loading.value = true
  try {
    const res = await getRecords({ yearMonth: formatYearMonth(year.value, month.value) })
    records.value = res || []
  } catch (e) {
    console.error('加载记录失败', e)
  } finally {
    loading.value = false
  }
}

const prevMonth = () => {
  if (month.value <= 1) { year.value--; month.value = 12 }
  else month.value--
  loadData()
}

const nextMonth = () => {
  if (month.value >= 12) { year.value++; month.value = 1 }
  else month.value++
  loadData()
}

onMounted(async () => {
  // 并行加载配置和记录
  try {
    const cfg = await getConfig()
    if (cfg) userConfig.value = { ...userConfig.value, ...cfg }
  } catch { /* 忽略 */ }
  await loadData()

  // 加载月度总结
  await Promise.all([loadSummaryYears(), autoGenerateLastMonth()])
  await loadSummaries()
})
</script>

<template>
  <div class="page-container">
    <!-- ====== 顶部栏 ====== -->
    <div class="top-bar">
      <div class="month-selector">
        <van-icon name="arrow-left" @click="prevMonth" />
        <span class="month-title">{{ yearMonth }}</span>
        <van-icon name="arrow" @click="nextMonth" />
      </div>
    </div>

    <!-- ====== 统计摘要 ====== -->
    <div class="stats-row">
      <StatsCard title="总工时" :value="stats.totalHours" suffix="h" />
      <StatsCard title="工作天数" :value="stats.totalDays" suffix="天" color="#4CAF50" :decimal="0" />
      <StatsCard title="项目数" :value="stats.totalProjects" suffix="个" color="#2196F3" :decimal="0" />
    </div>

    <!-- ====== 日历网格 ====== -->
    <div class="calendar-wrapper">
      <!-- 星期头 -->
      <div class="cal-weekdays">
        <span v-for="wd in weekDays" :key="wd" class="cal-weekday">{{ wd }}</span>
      </div>

      <!-- 日期格子 -->
      <div class="cal-grid">
        <div
          v-for="(cell, idx) in calendarDays"
          :key="idx"
          :class="[
            'cal-cell',
            { 'cal-cell--empty': !cell.day },
            { 'cal-cell--today': cell.isToday },
            { 'cal-cell--future': cell.isFuture },
            { 'cal-cell--has-record': !!cell.record },
            { 'cal-cell--clickable': !cell.isFuture && cell.day > 0 }
          ]"
          @click="cell.day > 0 && !cell.isFuture && openDay(cell)"
        >
          <template v-if="cell.day > 0">
            <div v-if="cell.isToday" class="cal-today-ring"></div>
            <span class="cal-day-num">{{ cell.day }}</span>
            <span v-if="cell.record?.isRest" class="cal-hours cal-rest">休息</span>
            <span v-else-if="cell.hours > 0" class="cal-hours">{{ cell.hours }}h</span>
          </template>
        </div>
      </div>
  </div>

  <!-- ====== 月度总结 ====== -->
  <div class="summary-section">
    <div class="summary-header">
      <van-icon name="balance-o" />
      <span class="summary-title">月度总结</span>
      <van-button
        v-if="summaries.length > 0"
        size="small"
        plain
        round
        icon="description"
        color="var(--primary-color)"
        style="margin-left: auto; font-size: 11px; padding: 0 10px; height: 28px;"
        @click="handleExportYear"
      >
        导出 {{ activeSummaryYear }} 年
      </van-button>
    </div>

    <van-tabs
      v-model:active="activeSummaryYear"
      @change="onSummaryYearChange"
      color="var(--primary-color)"
      title-active-color="var(--primary-color)"
      style="margin-top: 4px;"
    >
      <van-tab
        v-for="sy in summaryYears"
        :key="sy"
        :title="sy + '年'"
        :name="sy"
      />
    </van-tabs>

    <div v-if="summaryLoading" class="summary-loading">
      <van-loading size="24px">加载中...</van-loading>
    </div>

    <div v-else-if="summaries.length === 0" class="summary-empty">
      <van-icon name="info-o" size="32" color="var(--text-muted)" />
      <p>暂无月度总结数据</p>
      <p class="summary-empty-hint">每月1日将自动生成上个月的总结</p>
    </div>

    <div v-else class="summary-list">
      <div
        v-for="s in summaries"
        :key="s.yearMonth"
        class="summary-item"
      >
        <div class="summary-item-top">
          <span class="summary-month">{{ s.yearMonth }}</span>
          <span class="summary-count">{{ s.recordCount }} 条记录</span>
        </div>
        <div class="summary-metrics">
          <div class="summary-metric">
            <span class="summary-metric-value">{{ s.totalHours.toFixed(1) }}</span>
            <span class="summary-metric-label">总工时(h)</span>
          </div>
          <div class="summary-metric-divider" />
          <div class="summary-metric">
            <span class="summary-metric-value salary">¥{{ s.totalSalary.toFixed(0) }}</span>
            <span class="summary-metric-label">总薪资</span>
          </div>
        </div>
        <div class="summary-item-footer">
          <van-button
            size="small"
            round
            icon="arrow"
            color="var(--primary-color)"
            class="detail-btn"
            @click="goToMonthlyDetail(s.yearMonth)"
          >
            查看记录详细
          </van-button>
        </div>
      </div>
    </div>
  </div>

  <!-- ====== 编辑弹窗 ====== -->
    <van-popup
      v-model:show="showPopup"
      position="bottom"
      round
      :style="{ maxHeight: '80vh', padding: '20px 16px 30px' }"
    >
      <div class="popup-header">
        <span class="popup-date">{{ selectedDate }}</span>
        <van-icon
          v-if="isEdit"
          name="delete-o"
          color="#F44336"
          size="20"
          class="popup-delete"
          @click="deleteCurrentRecord"
        />
      </div>

      <!-- 个人信息小卡片 -->
      <div class="info-cards">
        <div class="info-card-item" v-if="userConfig.company">
          <van-icon name="location-o" size="16" color="#FF8A65" />
          <div class="info-card-text">
            <span class="info-label">公司</span>
            <span class="info-value">{{ userConfig.company }}</span>
          </div>
        </div>
        <div class="info-card-item" v-if="userConfig.department">
          <van-icon name="friends-o" size="16" color="#4CAF50" />
          <div class="info-card-text">
            <span class="info-label">部门</span>
            <span class="info-value">{{ userConfig.department }}</span>
          </div>
        </div>
        <div class="info-card-item" v-if="userConfig.position">
          <van-icon name="gem-o" size="16" color="#2196F3" />
          <div class="info-card-text">
            <span class="info-label">岗位</span>
            <span class="info-value">{{ userConfig.position }}</span>
          </div>
        </div>
      </div>

      <!-- 编辑表单 -->
      <div class="popup-form">
        <!-- 休息开关 -->
        <van-cell class="popup-rest-row" clickable @click="editForm.isRest = !editForm.isRest">
          <template #title>
            <span class="rest-label">😴 休息</span>
          </template>
          <template #value>
            <van-switch v-model="editForm.isRest" size="22" active-color="#FF8A65" @click.stop />
          </template>
        </van-cell>

        <van-field
          v-model="editForm.project"
          label="项目"
          placeholder="请输入项目名称"
          :clearable="true"
          :disabled="editForm.isRest"
        />
        <van-field
          v-model="editForm.content"
          label="工作内容"
          type="textarea"
          placeholder="请输入工作内容"
          rows="3"
          autosize
          :disabled="editForm.isRest"
        />
        <van-field label="工时">
          <template #input>
            <div v-if="editForm.isRest" class="rest-hours-text">休息中</div>
            <van-stepper v-else v-model="editForm.hours" min="0.5" max="24" step="0.5" />
          </template>
        </van-field>
        <van-field
          v-model="editForm.remark"
          label="备注"
          type="textarea"
          placeholder="可选，填写备注"
          rows="2"
          autosize
        />
      </div>

      <van-button
        round
        block
        type="primary"
        color="var(--primary-color)"
        :loading="submitting"
        style="margin-top: 16px"
        @click="submitPopupForm"
      >
        {{ isEdit ? '更新' : '添加' }}
      </van-button>
    </van-popup>
  </div>
</template>

<style scoped>
.top-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px 12px 0;
}

.month-selector {
  display: flex;
  align-items: center;
  gap: 24px;
}

.month-title {
  font-size: 18px;
  font-weight: 600;
  min-width: 110px;
  text-align: center;
}

.month-selector .van-icon {
  font-size: 20px;
  color: var(--text-muted);
  padding: 8px;
  cursor: pointer;
}

.stats-row {
  display: flex;
  gap: 8px;
  margin: 12px;
}

/* ====== 日历样式 ====== */
.calendar-wrapper {
  margin: 0 12px;
  background: var(--card-bg);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  padding: 10px 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.cal-weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  text-align: center;
  margin-bottom: 6px;
}

.cal-weekday {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-muted);
  padding: 2px 0;
}

.cal-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 2px;
}

.cal-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  position: relative;
  min-height: 44px;
  padding: 3px 0;
  transition: all 0.12s;
  overflow: hidden;
}

.cal-cell--empty {
  visibility: hidden;
}

.cal-cell--clickable {
  cursor: pointer;
}

.cal-cell--clickable:active {
  background: rgba(255, 138, 101, 0.15);
  transform: scale(0.94);
}

/* ====== 今日单元格：动态粒子效果 ====== */
.cal-cell--today {
  background: linear-gradient(135deg, #FF6E40, #E64A19);
  background-size: 200% 200%;
  animation: todayGradient 4s ease infinite;
  box-shadow: 0 3px 12px rgba(230, 74, 25, 0.5);
  transform: scale(1.06);
  z-index: 2;
}

/* 渐变动态流动 */
@keyframes todayGradient {
  0%   { background-position: 0% 50%; }
  50%  { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

/* 粒子1：左上浮 */
.cal-cell--today::before {
  content: '';
  position: absolute;
  top: 6px;
  left: 8px;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.6);
  box-shadow: 0 0 6px rgba(255, 255, 255, 0.4);
  animation: particleFloat1 3s ease-in-out infinite;
  pointer-events: none;
}

@keyframes particleFloat1 {
  0%, 100% { transform: translate(0, 0) scale(1); opacity: 0.6; }
  50%      { transform: translate(4px, -4px) scale(1.4); opacity: 0.2; }
}

/* 粒子2：右下浮 */
.cal-cell--today::after {
  content: '';
  position: absolute;
  bottom: 8px;
  right: 8px;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.5);
  box-shadow: 0 0 5px rgba(255, 255, 255, 0.3);
  animation: particleFloat2 3.5s ease-in-out infinite;
  pointer-events: none;
}

@keyframes particleFloat2 {
  0%, 100% { transform: translate(0, 0) scale(1); opacity: 0.5; }
  50%      { transform: translate(-3px, 3px) scale(1.6); opacity: 0.15; }
}

/* 脉冲光环 */
.cal-cell--today .cal-today-ring {
  position: absolute;
  inset: -2px;
  border-radius: 12px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  animation: ringPulse 2s ease-in-out infinite;
  pointer-events: none;
}

@keyframes ringPulse {
  0%, 100% { transform: scale(1); opacity: 0.5; }
  50%      { transform: scale(1.08); opacity: 0; }
}

.cal-cell--today .cal-day-num {
  color: #fff;
  font-size: 18px;
  font-weight: 800;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.3);
  position: relative;
  z-index: 1;
}

.cal-cell--today .cal-hours {
  color: rgba(255, 255, 255, 0.95);
  font-size: 10px;
  font-weight: 700;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
  position: relative;
  z-index: 1;
}

.cal-cell--future {
  opacity: 0.35;
  pointer-events: none;
}

.cal-cell--has-record .cal-day-num {
  color: var(--primary-dark);
  font-weight: 600;
}

.cal-cell--has-record::after {
  content: '';
  position: absolute;
  bottom: 4px;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: var(--primary-color);
}

.cal-day-num {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  line-height: 1.3;
}

.cal-hours {
  font-size: 10px;
  color: var(--primary-color);
  font-weight: 600;
  line-height: 1;
}

/* ====== 弹窗样式 ====== */
.popup-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.popup-date {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
}

.popup-delete {
  padding: 4px;
  cursor: pointer;
}

/* 个人信息小卡片 */
.info-cards {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.info-card-item {
  display: flex;
  align-items: center;
  gap: 6px;
  background: rgba(255, 138, 101, 0.08);
  border: 1px solid rgba(255, 138, 101, 0.2);
  border-radius: 8px;
  padding: 6px 10px;
  flex: 1;
  min-width: 80px;
}

.info-card-text {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}

.info-label {
  font-size: 10px;
  color: var(--text-muted);
}

.info-value {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-primary);
}

/* 表单 */
.popup-form {
  background: #f8f8f8;
  border-radius: 10px;
  padding: 4px 0;
}

.popup-form .van-field {
  background: transparent;
}

/* 休息开关行 */
.popup-rest-row {
  background: transparent;
  border-bottom: 1px solid #eee;
}

.rest-label {
  font-size: 14px;
  font-weight: 500;
}

/* 休息状态下工时文字 */
.rest-hours-text {
  color: var(--text-muted);
  font-size: 13px;
  font-weight: 400;
  padding: 4px 0;
}

/* 日历上的休息标记 */
.cal-hours.cal-rest {
  color: #bbb;
}

/* ====== 月度总结 ====== */
.summary-section {
  margin: 12px;
  background: var(--card-bg);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  padding: 14px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.summary-header {
  display: flex;
  align-items: center;
  gap: 6px;
}

.summary-header .van-icon {
  font-size: 16px;
  color: var(--primary-color);
}

.summary-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.summary-loading {
  display: flex;
  justify-content: center;
  padding: 24px 0;
}

.summary-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 0 12px;
  color: var(--text-muted);
  font-size: 13px;
  gap: 4px;
}

.summary-empty-hint {
  font-size: 11px;
  color: var(--text-muted);
  opacity: 0.7;
}

.summary-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 8px;
}

.summary-item {
  background: linear-gradient(135deg, #fefcfb 0%, #fdf8f5 100%);
  border: 1px solid rgba(255, 138, 101, 0.1);
  border-radius: 12px;
  padding: 12px 14px;
}

.summary-item-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.summary-month {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.summary-count {
  font-size: 11px;
  color: var(--text-muted);
}

.summary-metrics {
  display: flex;
  align-items: center;
  gap: 0;
}

.summary-metric {
  flex: 1;
  text-align: center;
}

.summary-metric-value {
  display: block;
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
}

.summary-metric-value.salary {
  color: var(--primary-color);
}

.summary-metric-label {
  display: block;
  font-size: 10px;
  color: var(--text-muted);
  margin-top: 2px;
}

.summary-metric-divider {
  width: 1px;
  height: 28px;
  background: rgba(0, 0, 0, 0.06);
}

.summary-item-footer {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
}

.detail-btn {
  height: 28px !important;
  padding: 0 12px !important;
  font-size: 11px !important;
}

/* Tabs 样式覆盖 */
.summary-section .van-tabs__wrap {
  margin-bottom: 0;
}

.summary-section .van-tab {
  font-size: 13px;
}
</style>
