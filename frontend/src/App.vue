<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const active = ref(0)

const tabRoutes = [
  { name: '记录', icon: 'bars', path: '/records' },
  { name: '统计', icon: 'chart-trending-o', path: '/stats' },
  { name: '我的', icon: 'setting-o', path: '/config' }
]

// 是否显示底部 TabBar（登录/注册页面隐藏）
const showTabbar = computed(() => {
  return !route.meta.noAuth
})

// 根据当前路由同步 tab 高亮
watch(
  () => route.path,
  (path) => {
    const idx = tabRoutes.findIndex((r) => path.startsWith(r.path))
    if (idx >= 0) active.value = idx
  },
  { immediate: true }
)

const onTabChange = (idx: number) => {
  router.push(tabRoutes[idx].path)
}
</script>

<template>
  <div class="app-container">
    <router-view />
    <van-tabbar
      v-if="showTabbar"
      v-model="active"
      active-color="#FF8A65"
      inactive-color="#999"
      @change="onTabChange"
      border
      safe-area-inset-bottom
    >
      <van-tabbar-item
        v-for="tab in tabRoutes"
        :key="tab.path"
        :icon="tab.icon"
      >
        {{ tab.name }}
      </van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<style scoped>
.app-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-container :deep(.van-tabbar) {
  z-index: 100;
}
</style>
