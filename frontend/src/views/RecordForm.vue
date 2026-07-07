<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast, showLoadingToast, closeToast } from 'vant'
import dayjs from 'dayjs'
import { addRecord, updateRecord, getRecord } from '@/api/records'
import type { WorkRecord } from '@/api/records'

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => !!route.params.id)
const recordId = computed(() => Number(route.params.id))
const pageTitle = computed(() => (isEdit.value ? '编辑记录' : '新增记录'))
const showCalendar = ref(false)

const form = ref({
  workDate: dayjs().format('YYYY-MM-DD'),
  company: '',
  department: '',
  position: '',
  content: '',
  hours: 1,
  project: '',
  remark: ''
})

const submitting = ref(false)

// 表单验证规则
const formRules = {
  workDate: [{ required: true, message: '请选择日期' }],
  company: [{ required: true, message: '请输入公司名称' }],
  department: [{ required: true, message: '请输入部门' }],
  position: [{ required: true, message: '请输入岗位' }],
  content: [{ required: true, message: '请输入工作内容' }],
  hours: [
    { required: true, message: '请输入工时' },
    { validator: (val: number) => val > 0 && val <= 24, message: '工时必须在0-24之间' }
  ],
  project: [{ required: true, message: '请输入项目名称' }]
}

const formRef = ref()

const onCalendarConfirm = (d: Date) => {
  form.value.workDate = dayjs(d).format('YYYY-MM-DD')
  showCalendar.value = false
}

const onSubmit = async () => {
  if (submitting.value) return

  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  showLoadingToast({ message: '提交中...', forbidClick: true })

  try {
    if (isEdit.value) {
      await updateRecord(recordId.value, form.value as WorkRecord)
      showToast('更新成功')
    } else {
      await addRecord(form.value as WorkRecord)
      showToast('新增成功')
    }
    router.back()
  } catch (e) {
    console.error('提交失败', e)
  } finally {
    submitting.value = false
    closeToast()
  }
}

// 加载编辑数据
onMounted(async () => {
  if (isEdit.value) {
    try {
      const data = await getRecord(recordId.value)
      form.value = { ...form.value, ...data }
    } catch (e) {
      console.error('加载记录失败', e)
      showToast('加载失败')
    }
  }
})
</script>

<template>
  <div class="page-container">
    <van-nav-bar
      :title="pageTitle"
      left-arrow
      @click-left="router.back()"
    />

    <van-form ref="formRef" @submit="onSubmit">
      <van-cell-group inset>
        <van-field
          v-model="form.workDate"
          name="workDate"
          label="日期"
          placeholder="选择日期"
          is-link
          readonly
          :rules="formRules.workDate"
          @click="showCalendar = true"
        />
        <van-calendar
          v-model:show="showCalendar"
          @confirm="onCalendarConfirm"
        />

        <van-field
          v-model="form.company"
          name="company"
          label="公司"
          placeholder="请输入公司名称"
          :rules="formRules.company"
        />
        <van-field
          v-model="form.department"
          name="department"
          label="部门"
          placeholder="请输入部门"
          :rules="formRules.department"
        />
        <van-field
          v-model="form.position"
          name="position"
          label="岗位"
          placeholder="请输入岗位"
          :rules="formRules.position"
        />
        <van-field
          v-model="form.project"
          name="project"
          label="项目"
          placeholder="请输入项目名称"
          :rules="formRules.project"
        />
        <van-field
          v-model="form.content"
          name="content"
          label="工作内容"
          type="textarea"
          placeholder="请输入工作内容"
          rows="3"
          autosize
          :rules="formRules.content"
        />
        <van-field
          v-model.number="form.hours"
          name="hours"
          label="工时"
          type="digit"
          placeholder="请输入工时"
          :rules="formRules.hours"
        >
          <template #button>
            <van-stepper v-model="form.hours" min="0.5" max="24" step="0.5" />
          </template>
        </van-field>
        <van-field
          v-model="form.remark"
          name="remark"
          label="备注"
          type="textarea"
          placeholder="可选，填写备注信息"
          rows="2"
          autosize
        />
      </van-cell-group>

      <div style="margin: 16px">
        <van-button
          round
          block
          type="primary"
          native-type="submit"
          :loading="submitting"
          color="var(--primary-color)"
        >
          {{ isEdit ? '更新记录' : '提交记录' }}
        </van-button>
      </div>
    </van-form>
  </div>
</template>
