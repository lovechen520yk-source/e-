<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { CountUp } from 'countup.js'

const props = defineProps<{
  title: string
  value: number
  suffix?: string
  color?: string
  decimal?: number
}>()

const emit = defineEmits<{
  (e: 'update:value', val: number): void
}>()

const displayRef = ref<HTMLSpanElement>()

let countUpInstance: CountUp | null = null

const startCountUp = () => {
  if (!displayRef.value) return

  if (countUpInstance) {
    countUpInstance.update(props.value)
  } else {
    countUpInstance = new CountUp(displayRef.value, props.value, {
      duration: 1.2,
      decimalPlaces: props.decimal ?? 1,
      separator: ',',
      suffix: props.suffix ?? ''
    })
    if (!countUpInstance.error) {
      countUpInstance.start()
    }
  }
}

watch(() => props.value, () => {
  startCountUp()
})

onMounted(() => {
  startCountUp()
})
</script>

<template>
  <div class="stats-card" :style="{ '--card-color': color || 'var(--primary-color)' }">
    <div class="stats-card-label">{{ title }}</div>
    <div class="stats-card-value">
      <span ref="displayRef" class="countup-number">0</span>
    </div>
  </div>
</template>

<style scoped>
.stats-card {
  flex: 1;
  background: var(--card-bg);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  padding: 14px 12px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  margin: 0 4px;
}

.stats-card-label {
  font-size: 12px;
  color: var(--text-muted);
  margin-bottom: 6px;
}

.stats-card-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--card-color, var(--primary-color));
}

.countup-number {
  display: inline-block;
}
</style>
