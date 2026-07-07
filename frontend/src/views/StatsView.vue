<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { init, use, graphic, type EChartsType } from 'echarts/core'
import { PieChart, BarChart, LineChart } from 'echarts/charts'
import { TooltipComponent, GridComponent, LegendComponent, MarkLineComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { getCurrentYearMonth } from '@/utils/format'
import { getRecords } from '@/api/records'
import { getGroupStats } from '@/api/stats'
import { getHealthAdvice } from '@/api/health'
import { getConfig } from '@/api/config'
import type { WorkRecord } from '@/api/records'
import type { GroupStatsItem } from '@/api/stats'
import StatsCard from '@/components/StatsCard.vue'

// 按需注册 ECharts 组件
use([PieChart, BarChart, LineChart, TooltipComponent, GridComponent, LegendComponent, MarkLineComponent, CanvasRenderer])

const router = useRouter()
const { year: currentYear, month: currentMonth } = getCurrentYearMonth()

const year = ref(currentYear)
const month = ref(currentMonth)
const groupBy = ref<'project' | 'company' | 'department' | 'position'>('project')
const loading = ref(false)

const yearMonthStr = ref(`${currentYear}-${String(currentMonth).padStart(2, '0')}`)

// 统计数据
const totalHours = ref(0)
const totalDays = ref(0)
const dailyAvg = ref(0)
const maxDailyHours = ref(0)
const overtimeDays = ref(0)
const groupData = ref<GroupStatsItem[]>([])
const dailyData = ref<{ date: string; hours: number }[]>([])

// AI 健康提醒
const healthAdvice = ref('')
const healthLoading = ref(false)
const healthError = ref('')
const userName = ref('')

// 记录列表（用于计算健康统计数据）
const allRecords = ref<WorkRecord[]>([])

/** 格式化年月 */
const formatYearMonth = (y: number, m: number) => `${y}-${String(m).padStart(2, '0')}`

/** 加载当月记录和分组统计 */
const loadData = async () => {
  loading.value = true
  try {
    const ym = formatYearMonth(year.value, month.value)
    yearMonthStr.value = ym

    // 并行请求
    const [recordsRes, statsRes] = await Promise.all([
      getRecords({ yearMonth: ym }),
      getGroupStats({ groupBy: groupBy.value, yearMonth: ym })
    ])

    const records: WorkRecord[] = recordsRes || []
    allRecords.value = records

    // 计算总工时、工作天数、日均工时
    const workRecords = records.filter(r => !r.isRest)
    const total = workRecords.reduce((s, r) => s + r.hours, 0)
    const days = new Set(workRecords.map((r) => r.workDate)).size
    totalHours.value = Math.round(total * 10) / 10
    totalDays.value = days
    dailyAvg.value = days > 0 ? Math.round((total / days) * 10) / 10 : 0

    // 最大单日工时
    const dailyMap: Record<string, number> = {}
    for (const r of workRecords) {
      dailyMap[r.workDate] = (dailyMap[r.workDate] || 0) + r.hours
    }
    const dailyEntries = Object.entries(dailyMap)
    maxDailyHours.value = dailyEntries.length > 0
      ? Math.round(Math.max(...dailyEntries.map(([, h]) => h)) * 10) / 10
      : 0

    // 加班天数（>8小时）
    overtimeDays.value = dailyEntries.filter(([, h]) => h > 8).length

    // 分组数据
    groupData.value = statsRes || []

    // 每日工时（按日期汇总）
    dailyData.value = dailyEntries
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([date, hours]) => ({ date, hours: Math.round(hours * 10) / 10 }))

    await nextTick()
    renderPieChart()
    renderBarChart()
    renderLineChart()
  } catch (e) {
    console.error('加载统计数据失败', e)
  } finally {
    loading.value = false
  }
}

// 年月切换
const changeMonth = (delta: number) => {
  let newMonth = month.value + delta
  let newYear = year.value
  if (newMonth < 1) { newYear--; newMonth = 12 }
  if (newMonth > 12) { newYear++; newMonth = 1 }
  year.value = newYear
  month.value = newMonth
  // 切月时清空健康建议
  healthAdvice.value = ''
  healthError.value = ''
  loadData()
}

// ========== ECharts ==========

// 饼图
const pieChartRef = ref<HTMLDivElement>()
let pieInstance: EChartsType | null = null

const renderPieChart = () => {
  if (!pieChartRef.value || groupData.value.length === 0) return

  if (!pieInstance) {
    pieInstance = init(pieChartRef.value)
  }

  pieInstance.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c}h ({d}%)' },
    series: [{
      type: 'pie',
      radius: ['45%', '70%'],
      center: ['50%', '50%'],
      avoidLabelOverlap: true,
      itemStyle: {
        borderRadius: 6,
        borderColor: '#fff',
        borderWidth: 2
      },
      label: {
        show: true,
        position: 'outside',
        formatter: '{b}\n{c}h'
      },
      emphasis: {
        label: { show: true, fontSize: 14, fontWeight: 'bold' }
      },
      data: groupData.value.map((d) => ({
        name: d.name,
        value: d.totalHours
      }))
    }],
    color: ['#FF8A65', '#4CAF50', '#2196F3', '#FFC107', '#9C27B0', '#E91E63']
  })
}

// 柱状图
const barChartRef = ref<HTMLDivElement>()
let barInstance: EChartsType | null = null

const renderBarChart = () => {
  if (!barChartRef.value || dailyData.value.length === 0) return

  if (!barInstance) {
    barInstance = init(barChartRef.value)
  }

  barInstance.setOption({
    tooltip: { trigger: 'axis', formatter: '{b}<br/>工时: {c}h' },
    grid: { left: 40, right: 16, top: 20, bottom: 30 },
    xAxis: {
      type: 'category',
      data: dailyData.value.map((d) => d.date.slice(5)),
      axisLabel: { fontSize: 10, rotate: 45 }
    },
    yAxis: {
      type: 'value',
      name: '工时(h)',
      nameTextStyle: { fontSize: 10 }
    },
    series: [{
      type: 'bar',
      data: dailyData.value.map((d) => d.hours),
      itemStyle: {
        borderRadius: [4, 4, 0, 0],
        color: (params: any) => {
          const val = params.data
          if (val > 10) return new graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#F44336' }, { offset: 1, color: '#FF8A80' }
          ])
          if (val > 8) return new graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#FF9800' }, { offset: 1, color: '#FFB74D' }
          ])
          return new graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#FF8A65' }, { offset: 1, color: '#FFAB91' }
          ])
        }
      },
      barMaxWidth: 24,
      markLine: {
        silent: true,
        symbol: 'none',
        lineStyle: { type: 'dashed', color: '#4CAF50', width: 1.5 },
        label: { formatter: '8h', fontSize: 10, color: '#4CAF50' },
        data: [{ yAxis: 8 }]
      }
    }]
  })
}

// 折线图 - 工时趋势
const lineChartRef = ref<HTMLDivElement>()
let lineInstance: EChartsType | null = null

const renderLineChart = () => {
  if (!lineChartRef.value || dailyData.value.length === 0) return

  if (!lineInstance) {
    lineInstance = init(lineChartRef.value)
  }

  lineInstance.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: '{b}<br/>工时: {c}h'
    },
    grid: { left: 40, right: 16, top: 30, bottom: 30 },
    xAxis: {
      type: 'category',
      data: dailyData.value.map((d) => d.date.slice(5)),
      axisLabel: { fontSize: 10, rotate: 45 },
      boundaryGap: false
    },
    yAxis: {
      type: 'value',
      name: '工时(h)',
      nameTextStyle: { fontSize: 10 },
      min: 0
    },
    series: [{
      type: 'line',
      data: dailyData.value.map((d) => d.hours),
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: {
        width: 2.5,
        color: new graphic.LinearGradient(1, 0, 0, 0, [
          { offset: 0, color: '#FF8A65' },
          { offset: 1, color: '#FF6E40' }
        ])
      },
      itemStyle: { color: '#FF6E40', borderWidth: 2, borderColor: '#fff' },
      areaStyle: {
        color: new graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(255,138,101,0.35)' },
          { offset: 1, color: 'rgba(255,138,101,0.02)' }
        ])
      },
      markLine: {
        silent: true,
        symbol: 'none',
        lineStyle: { type: 'dashed', color: '#4CAF50', width: 1 },
        label: { formatter: '标准8h', fontSize: 10, color: '#4CAF50', position: 'insideEndTop' },
        data: [{ yAxis: 8 }]
      }
    }]
  })
}

// 窗口大小变化自适应
const handleResize = () => {
  pieInstance?.resize()
  barInstance?.resize()
  lineInstance?.resize()
}

onMounted(async () => {
  loadData()
  window.addEventListener('resize', handleResize)
  // 加载用户名
  try {
    const cfg = await getConfig()
    if (cfg) userName.value = cfg.name || cfg.username || '用户'
  } catch { /* 忽略 */ }
})

// 分组切换时重新请求并渲染饼图
watch(groupBy, async (val) => {
  try {
    const ym = formatYearMonth(year.value, month.value)
    const res = await getGroupStats({ groupBy: val, yearMonth: ym })
    groupData.value = res || []
    renderPieChart()
  } catch (e) {
    console.error('加载分组统计失败', e)
  }
})

/** 获取 AI 健康建议 */
const fetchHealthAdvice = async () => {
  if (totalDays.value === 0) {
    healthError.value = '当前月份暂无工时数据，无法生成健康建议'
    return
  }

  healthLoading.value = true
  healthError.value = ''
  healthAdvice.value = ''

  try {
    const res: any = await getHealthAdvice({
      totalHours: totalHours.value,
      dailyAvg: dailyAvg.value,
      maxDailyHours: maxDailyHours.value,
      workDays: totalDays.value,
      overtimeDays: overtimeDays.value,
      userName: userName.value || '用户',
      yearMonth: yearMonthStr.value
    })
    healthAdvice.value = res?.advice || '暂无建议'
  } catch (e) {
    console.error('获取健康建议失败', e)
    healthError.value = 'AI 健康建议获取失败，请稍后再试'
  } finally {
    healthLoading.value = false
  }
}
</script>

<template>
  <div class="page-container">
    <!-- 年月选择 -->
    <div class="month-selector">
      <van-icon name="arrow-left" @click="changeMonth(-1)" />
      <span class="month-title">{{ year }}年{{ month }}月</span>
      <van-icon name="arrow" @click="changeMonth(1)" />
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <StatsCard title="总工时" :value="totalHours" suffix="h" />
      <StatsCard title="工作天数" :value="totalDays" suffix="天" color="#4CAF50" :decimal="0" />
      <StatsCard title="日均工时" :value="dailyAvg" suffix="h" color="#2196F3" />
    </div>

    <!-- 分组选择 -->
    <div class="section-header">
      <span class="section-title">工时分布</span>
    </div>
    <div class="card-glass" style="padding: 4px 12px 0;">
      <van-tabs v-model:active="groupBy" @change="renderPieChart">
        <van-tab title="按项目" name="project" />
        <van-tab title="按公司" name="company" />
        <van-tab title="按部门" name="department" />
        <van-tab title="按岗位" name="position" />
      </van-tabs>
    </div>

    <!-- 饼图 -->
    <div class="card-glass">
      <div v-if="groupData.length === 0" class="chart-empty">暂无统计数据</div>
      <div v-else ref="pieChartRef" style="height: 260px; width: 100%"></div>
    </div>

    <!-- 柱状图 -->
    <div class="section-header">
      <span class="section-title">每日工时</span>
    </div>
    <div class="card-glass">
      <div v-if="dailyData.length === 0" class="chart-empty">暂无工时数据</div>
      <div v-else ref="barChartRef" style="height: 220px; width: 100%"></div>
    </div>

    <!-- 折线图：工时趋势 -->
    <div class="section-header">
      <span class="section-title">工时趋势</span>
    </div>
    <div class="card-glass">
      <div v-if="dailyData.length === 0" class="chart-empty">暂无趋势数据</div>
      <div v-else ref="lineChartRef" style="height: 220px; width: 100%"></div>
    </div>

    <!-- 工作强度概览 -->
    <div class="section-header">
      <span class="section-title">工作强度</span>
    </div>
    <div class="intensity-card">
      <div class="intensity-grid">
        <div class="intensity-item">
          <div class="intensity-value" :class="{ 'overtime': maxDailyHours > 10 }">
            {{ maxDailyHours.toFixed(1) }}h
          </div>
          <div class="intensity-label">最长单日</div>
        </div>
        <div class="intensity-item">
          <div class="intensity-value" :class="{ 'overtime': overtimeDays > totalDays * 0.5 }">
            {{ overtimeDays }}天
          </div>
          <div class="intensity-label">加班天数</div>
        </div>
        <div class="intensity-item">
          <div class="intensity-value" :class="{ 'overtime': dailyAvg > 9 }">
            {{ dailyAvg.toFixed(1) }}h
          </div>
          <div class="intensity-label">日均工时</div>
        </div>
        <div class="intensity-item">
          <div class="intensity-value normal">
            {{ totalDays > 0 ? Math.round((overtimeDays / totalDays) * 100) : 0 }}%
          </div>
          <div class="intensity-label">加班占比</div>
        </div>
      </div>
      <div v-if="dailyAvg > 10" class="intensity-warning">
        <van-icon name="warning-o" />
        本月日均工时超过10小时，请注意劳逸结合
      </div>
      <div v-else-if="dailyAvg > 9" class="intensity-caution">
        <van-icon name="info-o" />
        本月工作强度偏高，建议适当休息
      </div>
    </div>

    <!-- AI 健康提醒 -->
    <div class="section-header">
      <span class="section-title">AI 健康提醒</span>
      <van-button
        size="small"
        round
        plain
        icon="replay"
        color="var(--primary-color)"
        :loading="healthLoading"
        style="font-size: 11px; padding: 0 10px; height: 28px;"
        @click="fetchHealthAdvice"
      >
        {{ healthAdvice ? '刷新建议' : '获取建议' }}
      </van-button>
    </div>
    <div class="health-card">
      <div v-if="healthLoading" class="health-loading">
        <van-loading size="20px" color="#FF8A65" />
        <span>AI 正在分析您的工时数据...</span>
      </div>
      <div v-else-if="healthError" class="health-error">
        <van-icon name="warning-o" size="20" color="#FF9800" />
        <span>{{ healthError }}</span>
        <van-button size="small" plain round @click="fetchHealthAdvice">重试</van-button>
      </div>
      <div v-else-if="healthAdvice" class="health-content">
        <div class="health-advice-text">{{ healthAdvice }}</div>
        <div class="health-footer">
          <span class="health-tip">每日刷新获取最新建议</span>
          <span class="health-time">{{ new Date().toLocaleDateString('zh-CN') }}</span>
        </div>
      </div>
      <div v-else class="health-empty">
        <div class="health-empty-icon">
          <van-icon name="shield-o" size="32" color="rgba(255,138,101,0.5)" />
        </div>
        <p class="health-empty-text">点击"获取建议"让 AI 为您分析工时健康度</p>
        <p class="health-empty-hint">基于通义千问大模型，根据您的工时数据提供个性化健康建议</p>
        <van-button
          round
          size="small"
          color="var(--primary-color)"
          icon="star-o"
          @click="fetchHealthAdvice"
        >
          立即获取 AI 健康建议
        </van-button>
      </div>
    </div>

    <div style="margin: 16px; text-align: center; padding-bottom: 20px;">
      <van-button
        round
        size="small"
        plain
        icon="description"
        @click="router.push('/export')"
      >
        查看详细报表
      </van-button>
    </div>
  </div>
</template>

<style scoped>
.month-selector {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
  padding: 12px 0;
}

.month-title {
  font-size: 17px;
  font-weight: 600;
  min-width: 100px;
  text-align: center;
}

.month-selector .van-icon {
  font-size: 20px;
  color: var(--text-muted);
  padding: 8px;
}

.stats-row {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  padding: 0 12px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  margin: 16px 0 8px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.chart-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: var(--text-muted);
  font-size: 14px;
}

/* ====== 工作强度卡片 ====== */
.intensity-card {
  margin: 0 12px;
  background: var(--card-bg);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  padding: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.intensity-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}

.intensity-item {
  text-align: center;
}

.intensity-value {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: 0.5px;
}

.intensity-value.overtime {
  color: #F44336;
}

.intensity-value.normal {
  color: #4CAF50;
}

.intensity-label {
  font-size: 11px;
  color: var(--text-muted);
  margin-top: 4px;
}

.intensity-warning {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 12px;
  padding: 8px 12px;
  background: rgba(244, 67, 54, 0.08);
  border: 1px solid rgba(244, 67, 54, 0.15);
  border-radius: 8px;
  font-size: 12px;
  color: #E53935;
}

.intensity-caution {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 12px;
  padding: 8px 12px;
  background: rgba(255, 152, 0, 0.08);
  border: 1px solid rgba(255, 152, 0, 0.15);
  border-radius: 8px;
  font-size: 12px;
  color: #F57C00;
}

/* ====== AI 健康提醒卡片 ====== */
.health-card {
  margin: 0 12px;
  background: linear-gradient(135deg, #fef9f6 0%, #fff5f0 50%, #fef0ea 100%);
  border-radius: 14px;
  border: 1px solid rgba(255, 138, 101, 0.15);
  padding: 16px;
  box-shadow: 0 4px 16px rgba(255, 138, 101, 0.08);
  min-height: 100px;
}

.health-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 20px 0;
  font-size: 13px;
  color: var(--text-muted);
}

.health-error {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  font-size: 13px;
  color: #FF9800;
}

.health-content {
  animation: fadeIn 0.5s ease;
}

.health-advice-text {
  font-size: 14px;
  line-height: 1.8;
  color: var(--text-primary);
  white-space: pre-wrap;
  word-break: break-word;
}

.health-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 14px;
  padding-top: 10px;
  border-top: 1px solid rgba(255, 138, 101, 0.1);
}

.health-tip {
  font-size: 11px;
  color: var(--text-muted);
}

.health-time {
  font-size: 11px;
  color: var(--text-muted);
}

.health-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 0;
  gap: 8px;
}

.health-empty-icon {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: rgba(255, 138, 101, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 4px;
}

.health-empty-text {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 500;
  margin: 0;
}

.health-empty-hint {
  font-size: 12px;
  color: var(--text-muted);
  margin: 0 0 4px;
  text-align: center;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
