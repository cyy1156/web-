<script setup lang="ts">
import { computed } from 'vue'
import type { TimetableItem } from '@/types'
import { WEEK_TEXT } from '@/utils/format'

const props = defineProps<{ items: TimetableItem[]; week: number }>()

const PALETTE = [
  'bg-brand-50 text-brand-800 border-brand-200',
  'bg-emerald-50 text-emerald-800 border-emerald-200',
  'bg-amber-50 text-amber-800 border-amber-200',
  'bg-violet-50 text-violet-800 border-violet-200',
  'bg-rose-50 text-rose-800 border-rose-200',
  'bg-cyan-50 text-cyan-800 border-cyan-200',
  'bg-lime-50 text-lime-800 border-lime-200',
  'bg-indigo-50 text-indigo-800 border-indigo-200'
]

interface Block {
  key: string
  day: number
  start: number
  end: number
  span: number
  name: string
  room: string
  teacher: string
  color: string
  width: number
  offset: number
}

const blocks = computed<Block[]>(() => {
  const raw: Block[] = []
  props.items.forEach((item) => {
    item.times.forEach((slot, index) => {
      if (slot.weekType === 'ODD' && props.week % 2 === 0) return
      if (slot.weekType === 'EVEN' && props.week % 2 === 1) return
      raw.push({
        key: `${item.enrollmentId}-${index}`,
        day: slot.dayOfWeek,
        start: slot.startSection,
        end: slot.endSection,
        span: slot.endSection - slot.startSection + 1,
        name: item.courseName,
        room: item.classroom || '',
        teacher: item.teacherName,
        color: PALETTE[item.colorKey % PALETTE.length],
        width: 100,
        offset: 0
      })
    })
  })

  for (let day = 1; day <= 5; day += 1) {
    const group = raw.filter((block) => block.day === day).sort((a, b) => a.start - b.start)
    group.forEach((block) => {
      const overlaps = group.filter((other) => other.start <= block.end && block.start <= other.end)
      if (overlaps.length > 1) {
        const index = overlaps.findIndex((other) => other.key === block.key)
        block.width = 100 / overlaps.length
        block.offset = index * block.width
      }
    })
  }
  return raw
})
</script>

<template>
  <div class="overflow-auto">
    <div
      class="grid min-w-[720px] gap-px rounded-xl bg-line/60 p-px"
      style="grid-template-columns: 56px repeat(5, minmax(0, 1fr)); grid-template-rows: 34px repeat(12, 58px)"
    >
      <div :style="{ gridColumn: 1, gridRow: 1 }" class="rounded-tl-lg bg-white" />
      <div
        v-for="(day, index) in WEEK_TEXT"
        :key="`head-${day}`"
        :style="{ gridColumn: index + 2, gridRow: 1 }"
        class="flex items-center justify-center bg-white text-xs font-medium text-sub"
      >
        周{{ day }}
      </div>

      <div
        v-for="section in 12"
        :key="`label-${section}`"
        :style="{ gridColumn: 1, gridRow: section + 1 }"
        class="flex items-center justify-center bg-white text-[11px] font-medium text-slate-400"
        :class="section === 4 ? 'border-b border-dashed border-slate-200' : ''"
      >
        {{ section }}
      </div>

      <template v-for="section in 12" :key="`cells-${section}`">
        <div
          v-for="day in 5"
          :key="`cell-${section}-${day}`"
          :style="{ gridColumn: day + 1, gridRow: section + 1 }"
          class="bg-white"
          :class="section === 4 ? 'border-b border-dashed border-slate-200' : ''"
        />
      </template>

      <div
        v-for="block in blocks"
        :key="block.key"
        class="z-10 my-0.5 overflow-hidden rounded-lg border px-2 py-1.5 text-[11px] leading-tight shadow-sm transition-transform hover:z-20 hover:scale-[1.02]"
        :class="block.color"
        :style="{
          gridColumn: block.day + 1,
          gridRow: `${block.start + 1} / span ${block.span}`,
          width: `calc(${block.width}% - 4px)`,
          marginLeft: `calc(${block.offset}% + 2px)`
        }"
      >
        <p class="truncate font-semibold">{{ block.name }}</p>
        <p class="mt-0.5 truncate opacity-70">{{ block.room }}</p>
        <p class="truncate opacity-60">{{ block.teacher }}</p>
      </div>
    </div>
  </div>
</template>
