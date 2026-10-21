<script setup lang="ts">
import { computed } from 'vue'
import { creditText } from '@/utils/format'

const props = withDefaults(
  defineProps<{
    value: number
    max: number
    size?: number
    stroke?: number
    showText?: boolean
    caption?: string
  }>(),
  { size: 96, stroke: 9, showText: true, caption: '已选学分' }
)

const radius = computed(() => (props.size - props.stroke) / 2)
const circumference = computed(() => 2 * Math.PI * radius.value)
const ratio = computed(() => (props.max > 0 ? Math.min(1, props.value / props.max) : 0))
const offset = computed(() => circumference.value * (1 - ratio.value))
const over = computed(() => props.value > props.max)
const color = computed(() => (over.value ? '#DC2626' : '#2563EB'))
</script>

<template>
  <div class="relative inline-flex items-center justify-center" :style="{ width: `${size}px`, height: `${size}px` }">
    <svg :width="size" :height="size" class="-rotate-90">
      <circle :cx="size / 2" :cy="size / 2" :r="radius" fill="none" stroke="#EEF2F7" :stroke-width="stroke" />
      <circle
        :cx="size / 2"
        :cy="size / 2"
        :r="radius"
        fill="none"
        :stroke="color"
        :stroke-width="stroke"
        stroke-linecap="round"
        :stroke-dasharray="circumference"
        :stroke-dashoffset="offset"
        class="transition-all duration-500"
      />
    </svg>
    <div v-if="showText" class="absolute inset-0 flex flex-col items-center justify-center leading-none">
      <span class="font-semibold tabular-nums" :class="over ? 'text-red-600' : 'text-ink'" :style="{ fontSize: `${size * 0.24}px` }">
        {{ creditText(value) }}
      </span>
      <span class="mt-1 text-[10px] text-slate-400">/ {{ creditText(max) }}</span>
    </div>
    <span v-if="caption && size >= 90" class="absolute -bottom-4 text-[11px] text-slate-400">{{ caption }}</span>
  </div>
</template>
