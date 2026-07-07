<script setup lang="ts">
import type { WorkRecord } from '@/api/records'
import { formatDate, formatHours } from '@/utils/format'

const props = defineProps<{
  record: WorkRecord
  selectMode?: boolean
  selected?: boolean
}>()

const emit = defineEmits<{
  (e: 'click'): void
  (e: 'delete'): void
  (e: 'toggle-select'): void
}>()

const handleClick = () => {
  if (props.selectMode) {
    emit('toggle-select')
  } else {
    emit('click')
  }
}
</script>

<template>
  <div
    :class="['record-item', 'list-item', 'animate-fade-in', { 'is-selected': selected }]"
    @click="handleClick"
  >
    <!-- 多选复选框 -->
    <div v-if="selectMode" class="select-checkbox" @click.stop="emit('toggle-select')">
      <van-icon
        :name="selected ? 'success' : 'circle'"
        :class="['check-icon', { checked: selected }]"
      />
    </div>

    <div class="record-content-wrap">
      <div class="record-header">
        <span class="record-date">{{ formatDate(record.workDate, 'MM/DD') }}</span>
        <span class="record-weekday">{{ ['日', '一', '二', '三', '四', '五', '六'][new Date(record.workDate).getDay()] }}</span>
        <span class="record-hours">{{ formatHours(record.hours) }}</span>
      </div>
      <div class="record-body">
        <div class="record-content">{{ record.content }}</div>
        <div class="record-tags">
          <span class="tag tag-primary">{{ record.project }}</span>
          <span class="tag tag-primary">{{ record.company }}</span>
        </div>
      </div>
      <div class="record-footer">
        <span>{{ record.department }}</span>
        <span class="record-position">{{ record.position }}</span>
      </div>
    </div>

    <!-- 单条删除（多选模式下隐藏） -->
    <van-icon
      v-if="!selectMode"
      name="delete"
      class="record-delete"
      @click.stop="emit('delete')"
    />
  </div>
</template>

<style scoped>
.record-item {
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 14px 16px;
  transition: background-color 0.2s;
}

.record-item.is-selected {
  background-color: rgba(255, 138, 101, 0.06);
}

/* 多选复选框 */
.select-checkbox {
  flex-shrink: 0;
  padding-top: 2px;
}

.check-icon {
  font-size: 20px;
  color: var(--text-muted);
  opacity: 0.4;
  transition: all 0.2s;
}

.check-icon.checked {
  color: var(--primary-color);
  opacity: 1;
}

.record-content-wrap {
  flex: 1;
  min-width: 0;
  padding-right: 24px;
}

.record-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.record-date {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.record-weekday {
  font-size: 12px;
  color: var(--text-muted);
}

.record-hours {
  margin-left: auto;
  font-size: 14px;
  font-weight: 600;
  color: var(--primary-color);
}

.record-body {
  margin-bottom: 8px;
}

.record-content {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.6;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.record-tags {
  display: flex;
  gap: 6px;
}

.record-footer {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: var(--text-muted);
}

.record-position {
  color: var(--text-muted);
}

.record-delete {
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 18px;
  color: var(--text-muted);
  opacity: 0.4;
}

.record-delete:active {
  color: var(--danger);
  opacity: 1;
}
</style>
