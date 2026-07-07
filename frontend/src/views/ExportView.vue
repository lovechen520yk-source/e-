<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showLoadingToast, closeToast, showConfirmDialog } from 'vant'
import { exportExcel, downloadBackup, importBackup } from '@/api/export'
import { getCurrentYearMonth } from '@/utils/format'

const router = useRouter()
const { year: currentYear, month: currentMonth } = getCurrentYearMonth()

const year = ref(currentYear)
const month = ref(currentMonth)
const fileInputRef = ref<HTMLInputElement>()

const yearMonth = `${year.value}年${month.value}月`

// 导出 Excel
const handleExportExcel = async () => {
  showLoadingToast({ message: '导出中...', forbidClick: true })
  try {
    const ym = `${year.value}-${String(month.value).padStart(2, '0')}`
    const blob = await exportExcel({ yearMonth: ym })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `工时报表_${year.value}_${String(month.value).padStart(2, '0')}.xlsx`
    a.click()
    URL.revokeObjectURL(url)
    showToast('导出成功')
  } catch (e) {
    console.error('导出失败', e)
    showToast('导出失败')
  } finally {
    closeToast()
  }
}

// 下载备份
const handleDownloadBackup = async () => {
  showLoadingToast({ message: '备份下载中...', forbidClick: true })
  try {
    const blob = await downloadBackup()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `工时数据备份_${new Date().toISOString().slice(0, 10)}.json`
    a.click()
    URL.revokeObjectURL(url)
    showToast('备份下载成功')
  } catch (e) {
    console.error('备份下载失败', e)
    showToast('备份下载失败')
  } finally {
    closeToast()
  }
}

// 选择导入文件
const triggerFileInput = () => {
  fileInputRef.value?.click()
}

// 导入备份
const handleFileChange = async (e: Event) => {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  // 验证文件类型
  if (!file.name.endsWith('.json')) {
    showToast('请选择 JSON 备份文件')
    return
  }

  showConfirmDialog({
    title: '导入确认',
    message: '导入将覆盖现有数据，是否继续？',
    confirmButtonColor: 'var(--primary-color)'
  }).then(async () => {
    showLoadingToast({ message: '导入中...', forbidClick: true })
    try {
      await importBackup(file)
      showToast('导入成功')
    } catch (e) {
      console.error('导入失败', e)
      showToast('导入失败，请检查文件格式')
    } finally {
      closeToast()
      target.value = ''
    }
  }).catch(() => {
    target.value = ''
  })
}
</script>

<template>
  <div class="page-container">
    <van-nav-bar
      title="数据导出"
      left-arrow
      @click-left="router.back()"
    />

    <!-- 年月选择 -->
    <div class="card-glass">
      <div class="section-label">选择导出月份</div>
      <div class="month-selector">
        <van-icon name="arrow-left" @click="month <= 1 ? (year--, month=12) : month--" />
        <span class="month-title">{{ yearMonth }}</span>
        <van-icon name="arrow" @click="month >= 12 ? (year++, month=1) : month++" />
      </div>
    </div>

    <!-- 导出 Excel -->
    <div class="card-glass">
      <div class="action-row">
        <div class="action-info">
          <van-icon name="description" class="action-icon" color="#FF8A65" />
          <div>
            <div class="action-title">导出 Excel 报表</div>
            <div class="action-desc">生成选定月份的工时统计报表</div>
          </div>
        </div>
        <van-button
          round
          size="small"
          type="primary"
          color="var(--primary-color)"
          @click="handleExportExcel"
        >
          导出
        </van-button>
      </div>
    </div>

    <!-- 备份下载 -->
    <div class="card-glass">
      <div class="action-row">
        <div class="action-info">
          <van-icon name="downlod" class="action-icon" color="#4CAF50" />
          <div>
            <div class="action-title">下载数据备份</div>
            <div class="action-desc">导出全部数据为 JSON 格式备份文件</div>
          </div>
        </div>
        <van-button
          round
          size="small"
          type="success"
          @click="handleDownloadBackup"
        >
          下载
        </van-button>
      </div>
    </div>

    <!-- 导入备份 -->
    <div class="card-glass">
      <div class="action-row">
        <div class="action-info">
          <van-icon name="updload" class="action-icon" color="#2196F3" />
          <div>
            <div class="action-title">导入数据备份</div>
            <div class="action-desc">从 JSON 备份文件恢复数据</div>
          </div>
        </div>
        <van-button
          round
          size="small"
          type="primary"
          plain
          @click="triggerFileInput"
        >
          导入
        </van-button>
      </div>
    </div>

    <!-- 隐藏的文件输入 -->
    <input
      ref="fileInputRef"
      type="file"
      accept=".json"
      style="display: none"
      @change="handleFileChange"
    />
  </div>
</template>

<style scoped>
.month-selector {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
  padding: 8px 0;
}

.month-title {
  font-size: 16px;
  font-weight: 600;
  min-width: 100px;
  text-align: center;
}

.month-selector .van-icon {
  font-size: 18px;
  color: var(--text-muted);
  padding: 8px;
}

.section-label {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 4px;
  text-align: center;
}

.action-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.action-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.action-icon {
  font-size: 32px;
}

.action-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 2px;
}

.action-desc {
  font-size: 12px;
  color: var(--text-muted);
}
</style>
