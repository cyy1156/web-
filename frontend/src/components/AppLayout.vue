<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { RouterView, useRoute, useRouter } from 'vue-router'
import {
  BookOpen,
  CalendarDays,
  CalendarRange,
  ChevronLeft,
  ChevronRight,
  GraduationCap,
  LayoutDashboard,
  ListChecks,
  LogOut,
  Table2,
  Timer
} from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import { useStudyStore } from '@/stores/study'
import { countdown, creditText } from '@/utils/format'
import CreditRing from './CreditRing.vue'

const auth = useAuthStore()
const study = useStudyStore()
const route = useRoute()
const router = useRouter()

const collapsed = ref(false)
const left = ref(0)
let timer: number | undefined

const menus = computed(() =>
  auth.isAdmin
    ? [
        { to: '/admin/dashboard', label: '数据看板', icon: LayoutDashboard },
        { to: '/admin/rounds', label: '轮次配置', icon: CalendarRange },
        { to: '/admin/classes', label: '教学班管理', icon: Table2 }
      ]
    : [
        { to: '/selection', label: '选课大厅', icon: BookOpen },
        { to: '/timetable', label: '我的课表', icon: CalendarDays },
        { to: '/plan', label: '培养计划', icon: ListChecks }
      ]
)

watch(
  () => study.semester?.round?.remainingSeconds,
  (value) => {
    left.value = Number(value ?? 0)
  },
  { immediate: true }
)

onMounted(async () => {
  await auth.hydrate()
  if (auth.isStudent) {
    await study.refreshAll()
  } else {
    await study.loadSemester()
  }
  timer = window.setInterval(() => {
    if (left.value > 0) left.value -= 1
  }, 1000)
})

onUnmounted(() => {
  if (timer) window.clearInterval(timer)
})

function logout() {
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <div class="flex h-screen overflow-hidden bg-canvas">
    <aside
      class="relative flex shrink-0 flex-col bg-navy text-white transition-all duration-200"
      :class="collapsed ? 'w-[68px]' : 'w-[220px]'"
    >
      <div class="flex h-[60px] items-center gap-2.5 px-4">
        <div class="grid h-9 w-9 shrink-0 place-items-center rounded-xl bg-brand-600 shadow-lg shadow-brand-600/30">
          <GraduationCap class="h-5 w-5" />
        </div>
        <div v-if="!collapsed" class="min-w-0 leading-tight">
          <p class="truncate text-sm font-semibold">上理选课</p>
          <p class="truncate text-[10px] text-white/40">教学演示系统</p>
        </div>
      </div>

      <nav class="mt-4 flex flex-1 flex-col gap-1 px-3">
        <router-link
          v-for="menu in menus"
          :key="menu.to"
          :to="menu.to"
          class="group relative flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm text-white/65 transition-colors hover:bg-white/8 hover:text-white"
          :class="route.path === menu.to ? 'bg-white/10 font-medium text-white' : ''"
        >
          <span
            v-if="route.path === menu.to"
            class="absolute left-0 top-1/2 h-5 w-[3px] -translate-y-1/2 rounded-r bg-brand-500"
          />
          <component :is="menu.icon" class="h-[18px] w-[18px] shrink-0" />
          <span v-if="!collapsed" class="truncate">{{ menu.label }}</span>
        </router-link>
      </nav>

      <div v-if="!collapsed" class="mx-3 mb-3 rounded-xl bg-white/5 p-3 text-[11px] leading-relaxed text-white/45">
        <p class="font-medium text-white/70">教学演示项目</p>
        <p class="mt-1">与上海理工大学官方系统无关，数据均为脚本生成。</p>
      </div>

      <button
        type="button"
        class="mx-3 mb-4 flex items-center justify-center gap-1 rounded-lg py-2 text-xs text-white/40 transition-colors hover:bg-white/8 hover:text-white/80"
        @click="collapsed = !collapsed"
      >
        <ChevronRight v-if="collapsed" class="h-4 w-4" />
        <ChevronLeft v-else class="h-4 w-4" />
        <span v-if="!collapsed">收起菜单</span>
      </button>
    </aside>

    <div class="flex min-w-0 flex-1 flex-col">
      <header class="flex h-[60px] shrink-0 items-center gap-4 border-b border-line bg-white/90 px-5 backdrop-blur">
        <div class="min-w-0">
          <h1 class="truncate text-[15px] font-semibold text-ink">{{ route.meta.title }}</h1>
          <p class="truncate text-[11px] text-slate-400">{{ study.semester?.name || '加载中…' }}</p>
        </div>

        <div class="ml-auto flex items-center gap-3">
          <span
            class="hidden items-center gap-1.5 rounded-lg px-2.5 py-1.5 text-xs font-medium md:inline-flex"
            :class="study.roundOpen ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-500'"
          >
            <span class="relative flex h-1.5 w-1.5">
              <span
                v-if="study.roundOpen"
                class="absolute inline-flex h-full w-full animate-ping rounded-full bg-emerald-400 opacity-75"
              />
              <span class="relative inline-flex h-1.5 w-1.5 rounded-full" :class="study.roundOpen ? 'bg-emerald-500' : 'bg-slate-400'" />
            </span>
            {{ study.roundName }}
          </span>

          <div v-if="study.roundOpen" class="hidden items-center gap-1.5 rounded-lg bg-brand-50 px-2.5 py-1.5 text-xs font-medium text-brand-700 lg:flex">
            <Timer class="h-3.5 w-3.5" />
            <span class="tabular-nums">{{ countdown(left) }}</span>
          </div>

          <div v-if="auth.isStudent" class="hidden items-center gap-2 border-l border-line pl-3 xl:flex">
            <div class="text-right text-[11px] leading-tight text-slate-400">
              <p>已选学分</p>
              <p class="text-sm font-semibold text-ink tabular-nums">
                {{ creditText(study.totalCredits) }}
                <span class="text-[11px] font-normal text-slate-400">/ {{ creditText(study.maxCredits) }}</span>
              </p>
            </div>
            <CreditRing :value="study.totalCredits" :max="study.maxCredits" :size="38" :stroke="5" :show-text="false" />
          </div>

          <div class="flex items-center gap-2 border-l border-line pl-3">
            <div class="grid h-8 w-8 place-items-center rounded-full bg-brand-50 text-xs font-semibold text-brand-700">
              {{ auth.displayName.slice(0, 1) }}
            </div>
            <div class="hidden leading-tight sm:block">
              <p class="text-xs font-medium text-ink">{{ auth.displayName }}</p>
              <p class="text-[10px] text-slate-400">{{ auth.isAdmin ? '教务处 · 选课管理' : auth.studentNo || '学生' }}</p>
            </div>
            <button
              type="button"
              class="ml-1 grid h-8 w-8 place-items-center rounded-lg text-slate-400 transition-colors hover:bg-slate-100 hover:text-ink"
              title="退出登录"
              @click="logout"
            >
              <LogOut class="h-4 w-4" />
            </button>
          </div>
        </div>
      </header>

      <main class="min-h-0 flex-1 overflow-hidden">
        <RouterView v-slot="{ Component }">
          <transition name="page" mode="out-in">
            <component :is="Component" />
          </transition>
        </RouterView>
      </main>
    </div>
  </div>
</template>
