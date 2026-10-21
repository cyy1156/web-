<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import { CalendarCheck, CircleDot, Lock, Play, Save, Square } from 'lucide-vue-next'
import { apiRounds, apiUpdateRound } from '@/api'
import { useStudyStore } from '@/stores/study'
import type { RoundInfo } from '@/types'

const study = useStudyStore()
const rounds = ref<RoundInfo[]>([])
const loading = ref(true)
const saving = ref<number | null>(null)
const editing = reactive<Record<number, { start: string; end: string }>>({})

const confirmVisible = ref(false)
const target = ref<RoundInfo | null>(null)

const openCount = computed(() => rounds.value.filter((round) => round.status === 'OPEN').length)

function statusTheme(status: string) {
  if (status === 'OPEN') return 'bg-emerald-50 text-emerald-700'
  if (status === 'CLOSED') return 'bg-slate-100 text-slate-500'
  return 'bg-amber-50 text-amber-700'
}

function statusText(status: string) {
  if (status === 'OPEN') return '进行中'
  if (status === 'CLOSED') return '已结束'
  return '未开始'
}

function toInput(value: string) {
  return value ? value.slice(0, 16) : ''
}

function pretty(value: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '—'
}

async function load() {
  loading.value = true
  try {
    if (!study.semester) await study.loadSemester()
    rounds.value = await apiRounds()
    rounds.value.forEach((round) => {
      editing[round.id] = { start: toInput(round.startTime), end: toInput(round.endTime) }
    })
  } finally {
    loading.value = false
  }
}

async function saveTime(round: RoundInfo) {
  const value = editing[round.id]
  if (!value.start || !value.end) {
    MessagePlugin.warning('请填写完整的起止时间')
    return
  }
  if (value.start >= value.end) {
    MessagePlugin.warning('开始时间必须早于结束时间')
    return
  }
  saving.value = round.id
  try {
    await apiUpdateRound(round.id, { startTime: value.start, endTime: value.end })
    MessagePlugin.success('时间已更新')
    await load()
  } finally {
    saving.value = null
  }
}

async function toggleLocked(round: RoundInfo) {
  if (round.status === 'OPEN') {
    target.value = round
    confirmVisible.value = true
    return
  }
  await doOpen(round)
}

async function doOpen(round: RoundInfo) {
  saving.value = round.id
  try {
    const others = rounds.value.filter((item) => item.status === 'OPEN' && item.id !== round.id)
    for (const other of others) {
      await apiUpdateRound(other.id, { status: 'CLOSED' })
    }
    await apiUpdateRound(round.id, { status: 'OPEN' })
    MessagePlugin.success(`已开启「${round.name}」`)
    confirmVisible.value = false
    await Promise.all([load(), study.loadSemester()])
  } finally {
    saving.value = null
  }
}

async function confirmClose() {
  if (!target.value) return
  saving.value = target.value.id
  try {
    await apiUpdateRound(target.value.id, { status: 'CLOSED' })
    MessagePlugin.success('本轮选课已关闭')
    confirmVisible.value = false
    await Promise.all([load(), study.loadSemester()])
  } finally {
    saving.value = null
  }
}

async function toggleDrop(round: RoundInfo) {
  await apiUpdateRound(round.id, { allowDrop: round.allowDrop === 1 ? 0 : 1 })
  round.allowDrop = round.allowDrop === 1 ? 0 : 1
  MessagePlugin.success(round.allowDrop === 1 ? '已允许退课' : '已禁止退课')
}

onMounted(load)
</script>

<template>
  <div class="h-full overflow-y-auto">
    <div class="mx-auto max-w-[1180px] px-5 py-5">
      <div class="mb-4 flex flex-wrap items-end justify-between gap-3">
        <div>
          <h2 class="text-[22px] font-semibold tracking-tight text-ink">选课轮次配置</h2>
          <p class="mt-1 text-xs text-sub">同一时间只允许一个轮次处于进行中，关闭后学生端按钮立即置灰</p>
        </div>
        <span class="chip" :class="openCount ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-500'">
          <CircleDot class="mr-1 h-3 w-3" />{{ openCount ? `${openCount} 个轮次进行中` : '当前无进行中轮次' }}
        </span>
      </div>

      <div v-if="loading" class="grid gap-3 lg:grid-cols-3">
        <div v-for="i in 3" :key="i" class="skeleton h-64 w-full" />
      </div>

      <div v-else class="grid gap-3 lg:grid-cols-3">
        <section
          v-for="round in rounds"
          :key="round.id"
          class="card relative overflow-hidden p-4"
          :class="round.status === 'OPEN' ? 'ring-1 ring-brand-200' : ''"
        >
          <div v-if="round.status === 'OPEN'" class="absolute inset-x-0 top-0 h-1 bg-gradient-to-r from-brand-400 via-brand-600 to-cyan-400" />

          <div class="flex items-start justify-between">
            <div class="flex items-center gap-2.5">
              <span class="grid h-8 w-8 place-items-center rounded-lg bg-brand-50 text-xs font-semibold text-brand-700">
                R{{ round.roundNo }}
              </span>
              <div>
                <p class="text-[14px] font-semibold text-ink">{{ round.name }}</p>
                <p class="text-[11px] text-slate-400">
                  {{ round.status === 'OPEN' ? '学生可提交选课' : round.status === 'CLOSED' ? '已停止受理' : '等待开启' }}
                </p>
              </div>
            </div>
            <span class="chip" :class="statusTheme(round.status)">{{ statusText(round.status) }}</span>
          </div>

          <div class="mt-4 space-y-2.5">
            <label class="block">
              <span class="mb-1 block text-[11px] text-slate-400">开始时间</span>
              <input v-model="editing[round.id].start" type="datetime-local" class="field text-[13px]" />
            </label>
            <label class="block">
              <span class="mb-1 block text-[11px] text-slate-400">结束时间</span>
              <input v-model="editing[round.id].end" type="datetime-local" class="field text-[13px]" />
            </label>
          </div>

          <div class="mt-3 flex items-center justify-between rounded-lg bg-slate-50 px-2.5 py-2">
            <span class="text-[11px] text-sub">允许退课</span>
            <button
              type="button"
              class="relative h-5 w-9 rounded-full transition-colors"
              :class="round.allowDrop === 1 ? 'bg-brand-600' : 'bg-slate-200'"
              @click="toggleDrop(round)"
            >
              <span
                class="absolute top-0.5 h-4 w-4 rounded-full bg-white shadow transition-all"
                :class="round.allowDrop === 1 ? 'left-[18px]' : 'left-0.5'"
              />
            </button>
          </div>

          <p class="mt-2 text-[10px] text-slate-400">当前设置：{{ pretty(round.startTime) }} 至 {{ pretty(round.endTime) }}</p>

          <div class="mt-3 flex gap-2">
            <button type="button" class="btn-ghost flex-1 py-1.5 text-xs" :disabled="saving === round.id" @click="saveTime(round)">
              <Save class="h-3.5 w-3.5" />保存时间
            </button>
            <button
              type="button"
              class="flex-1 rounded-lg px-3 py-1.5 text-xs font-medium transition-colors"
              :class="
                round.status === 'OPEN'
                  ? 'bg-slate-800 text-white hover:bg-slate-900'
                  : 'bg-brand-600 text-white hover:bg-brand-700'
              "
              :disabled="saving === round.id"
              @click="toggleLocked(round)"
            >
              <span class="inline-flex items-center gap-1">
                <Square v-if="round.status === 'OPEN'" class="h-3 w-3" />
                <Play v-else class="h-3 w-3" />
                {{ round.status === 'OPEN' ? '关闭本轮' : '开启本轮' }}
              </span>
            </button>
          </div>
        </section>
      </div>

      <div class="card mt-4 flex items-start gap-3 p-4">
        <div class="grid h-8 w-8 shrink-0 place-items-center rounded-lg bg-amber-50 text-amber-600">
          <CalendarCheck class="h-4 w-4" />
        </div>
        <div class="text-[12px] leading-relaxed text-sub">
          <p class="font-medium text-ink">规则说明</p>
          <p class="mt-1">
            第一轮海选时间为每学期第 12 周，第二轮补选为第 15 周，第三轮退改选在开课第 3 周进行。第三轮结束后应锁定名单，
            锁定后所有选退课接口返回 40010。
          </p>
          <p class="mt-1 text-slate-400">演示环境时间窗由 seed 脚本按生成日相对设置，可直接在此修改。</p>
        </div>
      </div>

      <div
        v-if="confirmVisible"
        class="fixed inset-0 z-50 flex items-center justify-center bg-navy/40 px-4 backdrop-blur-sm"
        @click.self="confirmVisible = false"
      >
        <div class="w-full max-w-[360px] animate-fade-up rounded-2xl bg-white p-5 shadow-float">
          <div class="mb-2 grid h-9 w-9 place-items-center rounded-lg bg-amber-50 text-amber-600">
            <Lock class="h-4 w-4" />
          </div>
          <h3 class="text-base font-semibold text-ink">关闭「{{ target?.name }}」？</h3>
          <p class="mt-2 text-[13px] leading-relaxed text-sub">
            关闭后学生端选课按钮会立即置灰，接口返回 40001。已选记录不受影响。
          </p>
          <div class="mt-5 flex justify-end gap-2">
            <button type="button" class="btn-ghost" @click="confirmVisible = false">取消</button>
            <button
              type="button"
              class="inline-flex items-center gap-1.5 rounded-lg bg-slate-800 px-3.5 py-2 text-sm font-medium text-white hover:bg-slate-900"
              @click="confirmClose"
            >
              确认关闭
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
