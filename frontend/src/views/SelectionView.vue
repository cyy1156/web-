<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { MessagePlugin } from 'tdesign-vue-next'
import { Info, Loader2, RefreshCw, RotateCcw, Search, TriangleAlert } from 'lucide-vue-next'
import { apiClasses, apiColleges, apiDrop, apiEnroll, apiRemain } from '@/api'
import { useStudyStore } from '@/stores/study'
import type { ClassItem, EnrolledItem } from '@/types'
import CourseCard from '@/components/CourseCard.vue'
import EnrolledPanel from '@/components/EnrolledPanel.vue'
import { rndId } from '@/utils/format'

const study = useStudyStore()
const router = useRouter()

const keyword = ref('')
const courseType = ref<string[]>([])
const collegeId = ref<number | null>(null)
const onlyAvailable = ref(true)
const sortByRemain = ref(false)

const colleges = ref<Array<{ id: number; name: string }>>([])
const list = ref<ClassItem[]>([])
const total = ref(0)
const page = ref(1)
const size = 10
const loading = ref(false)
const finished = ref(false)
const pendingId = ref<number | null>(null)

const confirmVisible = ref(false)
const dropTarget = ref<EnrolledItem | null>(null)
const dropping = ref(false)

const types = [
  { value: 'REQUIRED', label: '必修' },
  { value: 'LIMITED', label: '限选' },
  { value: 'ELECTIVE', label: '任选' }
]

const canOperate = computed(() => study.roundOpen && !study.locked)
const enrolledIds = computed(() => study.enrolledClassIds)
const shown = computed(() => {
  if (!sortByRemain.value) return list.value
  return [...list.value].sort((a, b) => b.remaining - a.remaining)
})

function toggleType(value: string) {
  const index = courseType.value.indexOf(value)
  if (index >= 0) courseType.value.splice(index, 1)
  else courseType.value.push(value)
}

async function load(reset = false) {
  if (loading.value) return
  loading.value = true
  try {
    if (reset) {
      page.value = 1
      finished.value = false
    }
    const res = await apiClasses({
      semesterId: study.semesterId,
      keyword: keyword.value.trim() || undefined,
      courseType: courseType.value.length ? courseType.value.join(',') : undefined,
      collegeId: collegeId.value ?? undefined,
      onlyAvailable: onlyAvailable.value,
      page: page.value,
      size
    })
    total.value = res.total
    list.value = reset ? res.list : [...list.value, ...res.list]
    finished.value = list.value.length >= res.total
  } catch {
    finished.value = true
  } finally {
    loading.value = false
  }
}

function more() {
  if (finished.value || loading.value) return
  page.value += 1
  load()
}

function onScroll(event: Event) {
  const el = event.target as HTMLElement
  if (el.scrollHeight - el.scrollTop - el.clientHeight < 240) more()
}

async function refreshRemain() {
  if (document.hidden || list.value.length === 0) return
  try {
    const res = await apiRemain(list.value.map((item) => item.id))
    const map = new Map(res.map((item) => [item.id, item]))
    list.value = list.value.map((item) => {
      const next = map.get(item.id)
      return next ? { ...item, remaining: next.remaining, selectedCount: next.selectedCount } : item
    })
  } catch {
    // 轮询失败静默处理，下一轮重试
  }
}

async function enroll(item: ClassItem) {
  if (!canOperate.value) {
    MessagePlugin.warning('当前不在选课时间内')
    return
  }
  pendingId.value = item.id
  try {
    await apiEnroll(item.id, rndId('enroll'))
    MessagePlugin.success(`已选上《${item.courseName}》`)
    item.myStatus = 'SELECTED'
    await Promise.all([study.loadEnrolled(), study.loadCredit()])
    await refreshRemain()
  } catch {
    // 提示已由拦截器处理
  } finally {
    pendingId.value = null
  }
}

function askDrop(item: EnrolledItem) {
  dropTarget.value = item
  confirmVisible.value = true
}

async function confirmDrop() {
  if (!dropTarget.value) return
  dropping.value = true
  try {
    await apiDrop(dropTarget.value.id)
    MessagePlugin.success('退课成功，名额已释放')
    confirmVisible.value = false
    await Promise.all([study.loadEnrolled(), study.loadCredit()])
    await load(true)
  } catch {
    confirmVisible.value = false
  } finally {
    dropping.value = false
  }
}

function resetFilters() {
  keyword.value = ''
  courseType.value = []
  collegeId.value = null
  onlyAvailable.value = true
  load(true)
}

let debounce: number | undefined
watch(keyword, () => {
  if (debounce) window.clearTimeout(debounce)
  debounce = window.setTimeout(() => load(true), 350)
})
watch([courseType, collegeId, onlyAvailable], () => load(true), { deep: true })

let poll: number | undefined
onMounted(async () => {
  if (!study.semester) await study.refreshAll()
  colleges.value = await apiColleges()
  await load(true)
  poll = window.setInterval(refreshRemain, 3000)
})
onUnmounted(() => {
  if (poll) window.clearInterval(poll)
  if (debounce) window.clearTimeout(debounce)
})
</script>

<template>
  <div class="flex h-full">
    <aside class="hidden w-[228px] shrink-0 flex-col overflow-y-auto border-r border-line bg-white lg:flex">
      <div class="flex items-center justify-between px-4 pb-2 pt-4">
        <p class="text-[13px] font-semibold text-ink">筛选条件</p>
        <button type="button" class="btn-text" @click="resetFilters">
          <RotateCcw class="h-3 w-3" />重置
        </button>
      </div>

      <div class="px-4 py-3">
        <p class="mb-2 text-[11px] font-medium text-slate-400">课程性质</p>
        <div class="flex flex-wrap gap-1.5">
          <button
            v-for="type in types"
            :key="type.value"
            type="button"
            class="rounded-lg border px-2.5 py-1.5 text-xs transition-colors"
            :class="
              courseType.includes(type.value)
                ? 'border-brand-500 bg-brand-50 font-medium text-brand-700'
                : 'border-line bg-white text-sub hover:border-brand-200'
            "
            @click="toggleType(type.value)"
          >
            {{ type.label }}
          </button>
        </div>
      </div>

      <div class="px-4 py-3">
        <p class="mb-2 text-[11px] font-medium text-slate-400">开课学院</p>
        <div class="relative">
          <select v-model="collegeId" class="field appearance-none pr-7 text-[13px]">
            <option :value="null">全部学院</option>
            <option v-for="college in colleges" :key="college.id" :value="college.id">
              {{ college.name }}
            </option>
          </select>
          <svg class="pointer-events-none absolute right-2.5 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-slate-400" viewBox="0 0 20 20" fill="currentColor">
            <path d="M5.5 7.5 10 12l4.5-4.5" stroke="currentColor" stroke-width="1.6" fill="none" stroke-linecap="round" />
          </svg>
        </div>
      </div>

      <div class="px-4 py-3">
        <p class="mb-2 text-[11px] font-medium text-slate-400">关键字</p>
        <div class="relative">
          <Search class="pointer-events-none absolute left-2.5 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-slate-400" />
          <input v-model="keyword" class="field pl-8 text-[13px]" placeholder="课程名 / 课程代码" />
        </div>
      </div>

      <div class="mt-1 flex items-center justify-between border-t border-line px-4 py-3">
        <span class="text-xs text-sub">仅看有余量</span>
        <button
          type="button"
          class="relative h-5 w-9 rounded-full transition-colors"
          :class="onlyAvailable ? 'bg-brand-600' : 'bg-slate-200'"
          @click="onlyAvailable = !onlyAvailable"
        >
          <span
            class="absolute top-0.5 h-4 w-4 rounded-full bg-white shadow transition-all"
            :class="onlyAvailable ? 'left-[18px]' : 'left-0.5'"
          />
        </button>
      </div>

      <div class="mt-auto px-4 pb-4 pt-2">
        <div class="rounded-xl bg-brand-50/70 p-3 text-[11px] leading-relaxed text-brand-800/80">
          <p class="mb-1 flex items-center gap-1 font-medium text-brand-700"><Info class="h-3.5 w-3.5" />选课提示</p>
          <p>先到先得、额满为止。时间冲突与学分超限会在提交时实时拦截。</p>
        </div>
      </div>
    </aside>

    <section class="flex min-w-0 flex-1 flex-col">
      <div
        v-if="!canOperate"
        class="flex items-center gap-2 border-b border-amber-200 bg-amber-50 px-5 py-2.5 text-xs text-amber-800"
      >
        <TriangleAlert class="h-4 w-4 shrink-0" />
        <span>{{ study.locked ? '本学期选课名单已锁定，仅可查看。' : '当前不在选课时间内，选课按钮已置灰。' }}</span>
      </div>

      <div class="flex items-center justify-between border-b border-line bg-white/60 px-5 py-2.5">
        <p class="text-xs text-sub">
          共 <span class="font-semibold text-ink tabular-nums">{{ total }}</span> 个教学班
          <span class="ml-2 text-slate-400">每 3 秒刷新余量</span>
        </p>
        <div class="flex items-center gap-2">
          <button
            type="button"
            class="rounded-lg border px-2.5 py-1 text-[11px] transition-colors"
            :class="sortByRemain ? 'border-brand-400 bg-brand-50 text-brand-700' : 'border-line text-sub hover:border-brand-200'"
            @click="sortByRemain = !sortByRemain"
          >
            余量优先
          </button>
          <button
            type="button"
            class="grid h-7 w-7 place-items-center rounded-lg border border-line text-slate-400 transition-colors hover:text-brand-600"
            title="刷新"
            @click="load(true)"
          >
            <RefreshCw class="h-3.5 w-3.5" :class="loading ? 'animate-spin' : ''" />
          </button>
        </div>
      </div>

      <div class="min-h-0 flex-1 overflow-y-auto px-5 py-4" @scroll="onScroll">
        <div v-if="loading && list.length === 0" class="grid gap-3 xl:grid-cols-2">
          <div v-for="i in 6" :key="i" class="card space-y-3 p-4">
            <div class="skeleton h-4 w-2/5" />
            <div class="skeleton h-3 w-3/5" />
            <div class="skeleton h-1.5 w-full" />
          </div>
        </div>

        <div v-else-if="list.length === 0" class="flex h-full flex-col items-center justify-center text-center">
          <div class="grid h-14 w-14 place-items-center rounded-2xl bg-white shadow-card">
            <Search class="h-6 w-6 text-slate-300" />
          </div>
          <p class="mt-4 text-sm text-sub">没有匹配的教学班</p>
          <p class="mt-1 text-xs text-slate-400">试着放宽筛选条件，或关闭「仅看有余量」</p>
          <button type="button" class="btn-ghost mt-4" @click="resetFilters">清空筛选</button>
        </div>

        <div v-else class="grid gap-3 xl:grid-cols-2">
          <CourseCard
            v-for="item in shown"
            :key="item.id"
            :item="item"
            :enrolled="enrolledIds.has(item.id)"
            :loading="pendingId === item.id"
            :disabled="!canOperate"
            @select="enroll"
          />
        </div>

        <div v-if="list.length > 0" class="py-5 text-center">
          <button v-if="!finished" type="button" class="btn-ghost" :disabled="loading" @click="more">
            <Loader2 v-if="loading" class="h-4 w-4 animate-spin" />
            {{ loading ? '加载中' : '加载更多' }}
          </button>
          <p v-else class="text-[11px] text-slate-300">已经到底了 · 共 {{ total }} 个教学班</p>
        </div>
      </div>
    </section>

    <EnrolledPanel
      :items="study.enrolled"
      :credit="study.credit"
      :total-credits="study.totalCredits"
      :max-credits="study.maxCredits"
      :can-drop="canOperate"
      @drop="askDrop"
      @timetable="router.push('/timetable')"
    />

    <div
      v-if="confirmVisible"
      class="fixed inset-0 z-50 flex items-center justify-center bg-navy/40 px-4 backdrop-blur-sm"
      @click.self="confirmVisible = false"
    >
      <div class="w-full max-w-[360px] animate-fade-up rounded-2xl bg-white p-5 shadow-float">
        <h3 class="text-base font-semibold text-ink">确认退课</h3>
        <p class="mt-2 text-[13px] leading-relaxed text-sub">
          确认退掉《{{ dropTarget?.courseName }}》吗？退课后该名额立即释放，可能被其他同学选走。
        </p>
        <div class="mt-5 flex justify-end gap-2">
          <button type="button" class="btn-ghost" @click="confirmVisible = false">再想想</button>
          <button
            type="button"
            class="inline-flex items-center gap-1.5 rounded-lg bg-red-600 px-3.5 py-2 text-sm font-medium text-white transition-colors hover:bg-red-700 disabled:bg-red-300"
            :disabled="dropping"
            @click="confirmDrop"
          >
            {{ dropping ? '退课中…' : '确认退课' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
