<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showLoadingToast, closeToast } from 'vant'
import { saveConfig, getConfig, changePassword } from '@/api/config'
import type { UserConfig } from '@/api/config'

const router = useRouter()

const form = ref<UserConfig>({
  username: '',
  name: '',
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

const submitting = ref(false)
const formRef = ref()

// ====== 密码修改状态 ======
const changingPwd = ref(false) // 是否展开密码修改
const originalPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const showOrigPwd = ref(false)
const showNewPwd = ref(false)
const showConfirmPwd = ref(false)

const formRules = {
  name: [{ required: true, message: '请输入姓名' }],
  company: [{ required: true, message: '请输入公司名称' }],
  department: [{ required: true, message: '请输入部门' }],
  position: [{ required: true, message: '请输入岗位' }]
}

/** 展开密码修改 */
const onStartChangePwd = () => {
  changingPwd.value = true
  originalPassword.value = ''
  newPassword.value = ''
  confirmPassword.value = ''
}

/** 取消密码修改 */
const onCancelChangePwd = () => {
  changingPwd.value = false
  originalPassword.value = ''
  newPassword.value = ''
  confirmPassword.value = ''
}

/** 确认修改密码 */
const confirmPwdSubmitting = ref(false)
const onConfirmChangePwd = async () => {
  if (confirmPwdSubmitting.value) return

  if (!originalPassword.value) {
    showToast('请输入原密码')
    return
  }
  if (!newPassword.value || newPassword.value.length < 6) {
    showToast('新密码长度不能少于6位')
    return
  }
  if (newPassword.value !== confirmPassword.value) {
    showToast('两次密码输入不一致')
    return
  }

  confirmPwdSubmitting.value = true
  showLoadingToast({ message: '密码修改中...', forbidClick: true })

  try {
    await changePassword({
      originalPassword: originalPassword.value,
      newPassword: newPassword.value
    })
    closeToast()
    showToast('密码修改成功')
    changingPwd.value = false
  } catch {
    // 错误已在拦截器处理
  } finally {
    confirmPwdSubmitting.value = false
  }
}

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
  reader.onload = () => {
    form.value.avatar = reader.result as string
  }
  reader.readAsDataURL(file)
  target.value = ''
}

const avatarInputRef = ref<HTMLInputElement>()

const triggerAvatarUpload = () => {
  avatarInputRef.value?.click()
}

const onSubmit = async () => {
  if (submitting.value) return

  // 验证基本表单
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  showLoadingToast({ message: '保存中...', forbidClick: true })

  try {
    // 保存用户信息
    await saveConfig(form.value)

    showToast('保存成功')
    router.back()
  } catch (e) {
    console.error('保存失败', e)
  } finally {
    submitting.value = false
    closeToast()
  }
}

onMounted(async () => {
  try {
    const data = await getConfig()
    if (data) {
      form.value = { ...form.value, ...data }
    }
  } catch (e) {
    console.error('加载配置失败', e)
  }
})
</script>

<template>
  <div class="page-container">
    <van-nav-bar
      title="个人信息"
      left-arrow
      @click-left="router.back()"
    />

    <van-form ref="formRef" @submit="onSubmit">
      <!-- 头像 -->
      <van-cell-group inset style="margin-top: 16px;">
        <van-cell title="头像" center>
          <template #value>
            <div class="avatar-upload" @click="triggerAvatarUpload">
              <img
                v-if="form.avatar"
                :src="form.avatar"
                class="avatar-preview"
              />
              <van-icon v-else name="photograph" size="28" color="#ccc" />
              <van-icon name="edit" class="avatar-edit-icon" />
            </div>
          </template>
        </van-cell>
      </van-cell-group>

      <!-- 账户信息 -->
      <van-cell-group inset title="账户信息" style="margin-top: 16px;">
        <!-- 用户名：禁用不可编辑 -->
        <van-field
          v-model="form.username"
          name="username"
          label="用户名"
          disabled
          placeholder="未设置"
        />

        <van-field
          v-model="form.name"
          name="name"
          label="姓名"
          placeholder="请输入姓名"
          :rules="formRules.name"
        />

        <!-- 密码行：掩码 + 修改按钮 -->
        <van-cell center title="密码">
          <template #value>
            <div class="pwd-display">
              <span class="pwd-mask" v-if="!changingPwd">
                ********
              </span>
              <van-button
                v-if="!changingPwd"
                size="small"
                plain
                type="primary"
                color="#FF8A65"
                @click="onStartChangePwd"
              >
                修改密码
              </van-button>
            </div>
          </template>
        </van-cell>

        <!-- 修改密码展开区 -->
        <template v-if="changingPwd">
          <van-cell :border="false" class="pwd-divider">
            <span class="pwd-divider-text">修改密码</span>
          </van-cell>

          <van-field
            v-model="originalPassword"
            name="originalPassword"
            label="原密码"
            :type="showOrigPwd ? 'text' : 'password'"
            placeholder="请输入原密码"
            :right-icon="showOrigPwd ? 'eye-o' : 'closed-eye'"
            @click-right-icon="showOrigPwd = !showOrigPwd"
          />
          <van-field
            v-model="newPassword"
            name="newPassword"
            label="新密码"
            :type="showNewPwd ? 'text' : 'password'"
            placeholder="请输入新密码（至少6位）"
            :right-icon="showNewPwd ? 'eye-o' : 'closed-eye'"
            @click-right-icon="showNewPwd = !showNewPwd"
          />
          <van-field
            v-model="confirmPassword"
            name="confirmPassword"
            label="确认新密码"
            :type="showConfirmPwd ? 'text' : 'password'"
            placeholder="请再次输入新密码"
            :right-icon="showConfirmPwd ? 'eye-o' : 'closed-eye'"
            @click-right-icon="showConfirmPwd = !showConfirmPwd"
          />

          <van-cell class="pwd-actions">
            <template #title>
              <div class="pwd-btn-group">
                <van-button
                  size="small"
                  plain
                  color="#999"
                  @click="onCancelChangePwd"
                >
                  取消修改
                </van-button>
                <van-button
                  size="small"
                  type="primary"
                  color="#FF8A65"
                  :loading="confirmPwdSubmitting"
                  @click="onConfirmChangePwd"
                >
                  确认修改
                </van-button>
              </div>
            </template>
          </van-cell>
        </template>
      </van-cell-group>

      <!-- 工作信息 -->
      <van-cell-group inset title="工作信息" style="margin-top: 16px;">
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
          label="常用项目"
          placeholder="例如：ERP系统、电商平台"
        />
        <van-field
          v-model="form.content"
          name="content"
          label="常用工作内容"
          type="textarea"
          placeholder="例如：需求分析、编码开发、单元测试"
          rows="2"
          autosize
        />
      </van-cell-group>

      <!-- 薪资设置 -->
      <van-cell-group inset title="薪资设置" style="margin-top: 16px;">
        <van-cell title="薪资模式">
          <template #value>
            <van-switch
              v-model="form.salaryMode"
              :active-value="'HOURLY'"
              :inactive-value="'FIXED'"
              size="24"
              active-color="#FF8A65"
            />
          </template>
        </van-cell>
        <van-cell center title="当前模式">
          <template #value>
            <van-tag :type="form.salaryMode === 'HOURLY' ? 'primary' : 'success'">
              {{ form.salaryMode === 'HOURLY' ? '小时薪资' : '固定月薪' }}
            </van-tag>
          </template>
        </van-cell>
        <van-field
          v-if="form.salaryMode === 'HOURLY'"
          v-model.number="form.hourlyRate"
          name="hourlyRate"
          label="小时薪资"
          type="digit"
          placeholder="请输入小时薪资"
          :formatter="(v: string) => v.replace(/[^0-9.]/g, '')"
        >
          <template #prefix>¥</template>
        </van-field>
        <van-field
          v-if="form.salaryMode === 'FIXED'"
          v-model.number="form.fixedMonthlySalary"
          name="fixedMonthlySalary"
          label="固定月薪"
          type="digit"
          placeholder="请输入月薪"
          :formatter="(v: string) => v.replace(/[^0-9.]/g, '')"
        >
          <template #prefix>¥</template>
        </van-field>
        <van-field
          v-model.number="form.overtimeHourlyRate"
          name="overtimeHourlyRate"
          label="加班时薪"
          type="digit"
          placeholder="请输入加班时薪"
          :formatter="(v: string) => v.replace(/[^0-9.]/g, '')"
        >
          <template #prefix>¥</template>
        </van-field>
        <van-field
          v-model.number="form.allowance"
          name="allowance"
          label="月度津贴"
          type="digit"
          placeholder="请输入津贴金额"
          :formatter="(v: string) => v.replace(/[^0-9.]/g, '')"
        >
          <template #prefix>¥</template>
        </van-field>
        <van-field
          v-model.number="form.performanceBonus"
          name="performanceBonus"
          label="月度绩效"
          type="digit"
          placeholder="请输入绩效金额"
          :formatter="(v: string) => v.replace(/[^0-9.]/g, '')"
        >
          <template #prefix>¥</template>
        </van-field>
        <van-field
          v-model.number="form.deduction"
          name="deduction"
          label="扣除金额"
          type="digit"
          placeholder="请输入扣除金额"
          :formatter="(v: string) => v.replace(/[^0-9.]/g, '')"
        >
          <template #prefix>-¥</template>
        </van-field>
      </van-cell-group>

      <div style="margin: 20px 16px;">
        <van-button
          round
          block
          type="primary"
          native-type="submit"
          :loading="submitting"
          color="var(--primary-color)"
        >
          保存
        </van-button>
      </div>
    </van-form>

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
.avatar-upload {
  position: relative;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  overflow: hidden;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.avatar-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-edit-icon {
  position: absolute;
  bottom: 0;
  right: 0;
  font-size: 14px;
  color: #fff;
  background: rgba(0, 0, 0, 0.5);
  border-radius: 50%;
  padding: 3px;
}

/* 密码显示行 */
.pwd-display {
  display: flex;
  align-items: center;
  gap: 10px;
}

.pwd-mask {
  font-size: 16px;
  letter-spacing: 2px;
  color: var(--text-muted);
}

/* 密码修改分隔线 */
.pwd-divider {
  padding-top: 4px;
  padding-bottom: 4px;
}

.pwd-divider-text {
  font-size: 13px;
  font-weight: 600;
  color: var(--primary-color);
}

/* 密码操作按钮组 */
.pwd-actions {
  padding-top: 4px;
  padding-bottom: 4px;
}

.pwd-btn-group {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
