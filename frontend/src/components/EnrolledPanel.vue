<script setup lang="ts">
import { ArrowRight, CalendarRange, Trash2, TriangleAlert } from 'lucide-vue-next'
import type { CreditStatus, EnrolledItem } from '@/types'
import { creditText } from '@/utils/format'
import CreditRing from './CreditRing.vue'

defineProps<{
  items: EnrolledItem[]
  credit: CreditStatus | null
  totalCredits: number
  maxCredits: number
  canDrop: boolean
}>()

const emit = defineEmits<{
  (e: 'drop', item: EnrolledItem): void
  (e: 'timetable'): void
}>()
</script>

<template>
  <aside class="flex h-full w-[300px] shrink-0 flex-col border-l border-line bg-white">
    <div class="flex items-center justify-between border-b border-line px-4 py-3">
      <div>
        <p class="text-sm font-semibold text-ink">已选课程</p>
        <p class="text-[11px] text-slate-400">共 {{ items.length }} 门</p>
      </div>
      <div class="flex flex-col items-end">
        <span class="text-lg font-semibold leading-none tabular-nums" :class="totalCredits > maxCredits ? 'text-red-600' : 'text-brand-600'">
          {{ creditText(totalCredits) }}
        </span>
        <span class="text-[10px] text-slate-400">/ {{ creditText(maxCredits) }} 学分</span>
      </div>
    </div>

    <div class="flex justify-center border-b border-line py-4">
      <CreditRing :value="totalCredits" :max="maxCredits" :size="104" caption="本学期已选学分" />
    </div>

    <div class="min-h-0 flex-1 overflow-y-auto px-3 py-3">
      <div v-if="items.length === 0" class="mt-16 flex flex-col items-center text-center">
        <div class="grid h-12 w-12 place-items-center rounded-2xl bg-slate-50 text-slate-300">
          <CalendarRange class="h-6 w-6" />
        </div>
        <p class="mt-3 text-xs text-slate-400">还没有选择任何课程</p>
        <p class="mt-1 text-[11px] text-slate-300">在左侧列表点击「选课」加入</p>
      </div>

      <ul v-else class="space-y-2">
        <li
          v-for="item in items"
          :key="item.id"
          class="group relative rounded-lg border p-2.5 transition-colors"
          :class="item.conflicted ? 'border-red-200 bg-red-50/40' : 'border-line bg-white hover:border-brand-200'"
        >
          <span v-if="item.conflicted" class="absolute inset-y-2 left-0 w-[3px] rounded-r bg-red-500" />
          <div class="flex items-start justify-between gap-2">
            <p class="line-clamp-1 text-[13px] font-medium text-ink">{{ item.courseName }}</p>
            <span class="shrink-0 text-[11px] tabular-nums text-slate-500">{{ creditText(item.credits) }} 分</span>
          </div>
          <p class="mt-1 line-clamp-1 text-[11px] text-slate-400">{{ item.timeText }}</p>
          <div class="mt-1.5 flex items-center justify-between">
            <span v-if="item.conflicted" class="inline-flex items-center gap-1 text-[10px] font-medium text-red-600">
              <TriangleAlert class="h-3 w-3" />时间冲突
            </span>
            <span v-else class="text-[10px] text-slate-400">{{ item.teacherName }}</span>
            <button
              type="button"
              class="inline-flex items-center gap-0.5 rounded px-1 py-0.5 text-[10px] text-slate-400 opacity-0 transition-opacity hover:text-red-600 group-hover:opacity-100 disabled:cursor-not-allowed"
              :disabled="!canDrop"
              @click="emit('drop', item)"
            >
              <Trash2 class="h-3 w-3" />退课
            </button>
          </div>
        </li>
      </ul>
    </div>

    <div class="border-t border-line p-3">
      <p v-if="credit" class="mb-2 flex items-center justify-between text-[11px] text-slate-400">
        <span>培养计划建议 {{ creditText(credit.planCredits) }} 学分</span>
        <span>下限 {{ creditText(credit.minCredits) }}</span>
      </p>
      <button type="button" class="btn-primary w-full" @click="emit('timetable')">
        查看我的课表
        <ArrowRight class="h-4 w-4" />
      </button>
    </div>
  </aside>
</template>
