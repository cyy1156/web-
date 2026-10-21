<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ChevronDown, GraduationCap, Target, TrendingUp } from 'lucide-vue-next'
import { apiPlan } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { useStudyStore } from '@/stores/study'
import type { PlanRes } from '@/types'
import { creditText, percent } from '@/utils/format'

const study = useStudyStore()
const auth = useAuthStore()

const plan = ref<PlanRes | null>(null)
const loading = ref(true)
const openTerms = ref<number[]>([])

const progress = computed(() => percent(Number(plan.value?.passedCredits ?? 0), Number(plan.value?.requiredCredits ?? 1)))

function statusTheme(status: string) {
  switch (status) {
    case '已完成':
      return 'bg-emerald-50 text-emerald-700'
    case '在修':
      return 'bg-brand-50 text-brand-700'
    case '未通过':
      return 'bg-red-50 text-red-600'
    default:
      return 'bg-slate-100 text-slate-500'
  }
}

function toggle(termNo: number) {
  const index = openTerms.value.indexOf(termNo)
  if (index >= 0) openTerms.value.splice(index, 1)
  else openTerms.value.push(termNo)
}

onMounted(async () => {
  try {
    if (!study.semester) await study.refreshAll()
    plan.value = await apiPlan()
    const first = plan.value.terms.find((term) => term.courses.some((course) => course.status === '在修'))
    openTerms.value = [first?.termNo ?? plan.value.terms[0]?.termNo ?? 1]
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="h-full overflow-y-auto">
    <div class="mx-auto max-w-[1100px] px-5 py-5">
      <div class="mb-4">
        <h2 class="text-[22px] font-semibold tracking-tight text-ink">培养计划</h2>
        <p class="mt-1 text-xs text-sub">
          {{ auth.majorName }} · {{ auth.name || auth.displayName }} · 按建议修读学期展示
        </p>
      </div>

      <div class="mb-4 grid gap-3 md:grid-cols-3">
        <div class="card p-4">
          <div class="flex items-center gap-2 text-[11px] text-slate-400">
            <GraduationCap class="h-3.5 w-3.5" />已修学分
          </div>
          <p class="mt-2 text-2xl font-semibold tabular-nums text-ink">{{ creditText(plan?.passedCredits ?? 0) }}</p>
        </div>
        <div class="card p-4">
          <div class="flex items-center gap-2 text-[11px] text-slate-400">
            <TrendingUp class="h-3.5 w-3.5" />本学期在修
          </div>
          <p class="mt-2 text-2xl font-semibold tabular-nums text-brand-600">{{ creditText(plan?.earningCredits ?? 0) }}</p>
        </div>
        <div class="card p-4">
          <div class="flex items-center gap-2 text-[11px] text-slate-400">
            <Target class="h-3.5 w-3.5" />培养计划要求
          </div>
          <p class="mt-2 flex items-baseline gap-1">
            <span class="text-2xl font-semibold tabular-nums text-ink">{{ creditText(plan?.requiredCredits ?? 0) }}</span>
            <span class="text-xs text-slate-400">学分</span>
          </p>
          <div class="mt-2 h-1.5 overflow-hidden rounded-full bg-slate-100">
            <div class="h-full rounded-full bg-brand-500 transition-all duration-500" :style="{ width: `${progress}%` }" />
          </div>
          <p class="mt-1 text-[10px] text-slate-400">完成度 {{ progress }}%</p>
        </div>
      </div>

      <div v-if="loading" class="space-y-2">
        <div v-for="i in 4" :key="i" class="skeleton h-14 w-full" />
      </div>

      <div v-else class="space-y-2.5">
        <section v-for="term in plan?.terms ?? []" :key="term.termNo" class="card overflow-hidden">
          <button
            type="button"
            class="flex w-full items-center justify-between px-4 py-3 text-left transition-colors hover:bg-slate-50/70"
            @click="toggle(term.termNo)"
          >
            <div class="flex items-center gap-3">
              <span class="grid h-7 w-7 place-items-center rounded-lg bg-brand-50 text-[11px] font-semibold text-brand-700">
                {{ term.termNo }}
              </span>
              <div>
                <p class="text-[13px] font-medium text-ink">第 {{ term.termNo }} 学期</p>
                <p class="text-[11px] text-slate-400">
                  {{ term.courses.length }} 门 ·
                  {{ creditText(term.courses.reduce((sum, course) => sum + Number(course.credits), 0)) }} 学分
                </p>
              </div>
            </div>
            <ChevronDown
              class="h-4 w-4 text-slate-400 transition-transform"
              :class="openTerms.includes(term.termNo) ? 'rotate-180' : ''"
            />
          </button>

          <div v-if="openTerms.includes(term.termNo)" class="border-t border-line">
            <div class="grid grid-cols-[1fr_80px_70px_80px_70px] gap-2 bg-slate-50/70 px-4 py-2 text-[11px] font-medium text-slate-400">
              <span>课程名称</span>
              <span>性质</span>
              <span class="text-right">学分</span>
              <span class="text-center">状态</span>
              <span class="text-right">成绩</span>
            </div>
            <div
              v-for="course in term.courses"
              :key="course.courseId"
              class="grid grid-cols-[1fr_80px_70px_80px_70px] items-center gap-2 border-t border-line/60 px-4 py-2.5 text-[13px]"
              :class="course.status === '未通过' ? 'bg-red-50/40' : ''"
            >
              <span class="truncate text-ink">{{ course.courseName }}</span>
              <span class="text-xs text-sub">{{ course.requiredType }}</span>
              <span class="text-right tabular-nums text-sub">{{ creditText(course.credits) }}</span>
              <span class="text-center">
                <span class="chip" :class="statusTheme(course.status)">{{ course.status }}</span>
              </span>
              <span
                class="text-right text-xs tabular-nums"
                :class="course.score === null ? 'text-slate-300' : course.score >= 60 ? 'text-ink' : 'font-medium text-red-600'"
              >
                {{ course.score === null ? '—' : course.score }}
              </span>
            </div>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>
