<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { CalendarDays, ChevronLeft, ChevronRight, MapPin, Users } from 'lucide-vue-next'
import { apiTimetable } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { useStudyStore } from '@/stores/study'
import type { TimetableItem } from '@/types'
import TimetableGrid from '@/components/TimetableGrid.vue'
import { creditText } from '@/utils/format'

const study = useStudyStore()
const auth = useAuthStore()

const items = ref<TimetableItem[]>([])
const week = ref(study.semester?.currentWeek ?? 8)
const loading = ref(true)

const totalCredits = computed(() => items.value.reduce((sum, item) => {
  const found = study.enrolled.find((e) => e.teachingClassId === item.enrollmentId || e.id === item.enrollmentId)
  return sum + (found?.credits ?? 0)
}, 0))

const uniqueCourses = computed(() => items.value.length)

async function load() {
  loading.value = true
  try {
    if (!study.semester) await study.refreshAll()
    const res = await apiTimetable()
    items.value = res.items
  } finally {
    loading.value = false
  }
}

function changeWeek(delta: number) {
  const total = study.semester?.totalWeeks ?? 18
  week.value = Math.min(total, Math.max(1, week.value + delta))
}

onMounted(load)
</script>

<template>
  <div class="h-full overflow-y-auto">
    <div class="mx-auto max-w-[1180px] px-5 py-5">
      <div class="mb-4 flex flex-wrap items-end justify-between gap-4">
        <div>
          <h2 class="text-[22px] font-semibold tracking-tight text-ink">我的课表</h2>
          <p class="mt-1 text-xs text-sub">
            {{ auth.displayName }} · {{ auth.majorName }} · {{ study.semester?.name }}
          </p>
        </div>
        <div class="flex items-center gap-2">
          <button type="button" class="btn-ghost px-2.5 py-1.5" @click="changeWeek(-1)">
            <ChevronLeft class="h-4 w-4" />
          </button>
          <div class="rounded-lg border border-line bg-white px-3 py-1.5 text-xs font-medium text-ink">
            第 <span class="text-brand-600 tabular-nums">{{ week }}</span> 周
          </div>
          <button type="button" class="btn-ghost px-2.5 py-1.5" @click="changeWeek(1)">
            <ChevronRight class="h-4 w-4" />
          </button>
        </div>
      </div>

      <div class="mb-4 grid grid-cols-3 gap-3">
        <div class="card flex items-center gap-3 p-4">
          <div class="grid h-9 w-9 place-items-center rounded-lg bg-brand-50 text-brand-600">
            <CalendarDays class="h-[18px] w-[18px]" />
          </div>
          <div>
            <p class="text-[11px] text-slate-400">本学期课程</p>
            <p class="text-lg font-semibold leading-tight text-ink tabular-nums">{{ uniqueCourses }} <span class="text-xs font-normal text-slate-400">门</span></p>
          </div>
        </div>
        <div class="card flex items-center gap-3 p-4">
          <div class="grid h-9 w-9 place-items-center rounded-lg bg-emerald-50 text-emerald-600">
            <Users class="h-[18px] w-[18px]" />
          </div>
          <div>
            <p class="text-[11px] text-slate-400">已选学分</p>
            <p class="text-lg font-semibold leading-tight text-ink tabular-nums">
              {{ creditText(study.totalCredits) }} <span class="text-xs font-normal text-slate-400">/ {{ creditText(study.maxCredits) }}</span>
            </p>
          </div>
        </div>
        <div class="card flex items-center gap-3 p-4">
          <div class="grid h-9 w-9 place-items-center rounded-lg bg-amber-50 text-amber-600">
            <MapPin class="h-[18px] w-[18px]" />
          </div>
          <div>
            <p class="text-[11px] text-slate-400">上课校区</p>
            <p class="text-lg font-semibold leading-tight text-ink">{{ study.enrolled[0]?.campus || '军工路' }}</p>
          </div>
        </div>
      </div>

      <div class="card p-3">
        <div v-if="loading" class="space-y-2">
          <div class="skeleton h-[34px] w-full" />
          <div v-for="i in 8" :key="i" class="skeleton h-[52px] w-full" />
        </div>
        <TimetableGrid v-else :items="items" :week="week" />
      </div>

      <p class="mt-3 text-center text-[11px] text-slate-300">
        课表按周次显示，单双周课程会自动隐藏 · 共 {{ (study.semester?.totalWeeks ?? 18) }} 周
      </p>
    </div>
  </div>
</template>
