<script setup lang="ts">
import { computed } from 'vue'
import { AlertTriangle, Check, MapPin, Clock3 } from 'lucide-vue-next'
import type { ClassItem } from '@/types'
import { creditText, percent, remainClass, remainLevel, typeTheme } from '@/utils/format'

const props = defineProps<{
  item: ClassItem
  enrolled: boolean
  loading: boolean
  disabled: boolean
}>()

const emit = defineEmits<{ (e: 'select', item: ClassItem): void }>()

const level = computed(() => remainLevel(props.item.remaining, props.item.capacity))
const barWidth = computed(() => percent(props.item.selectedCount, props.item.capacity))
const full = computed(() => props.item.remaining <= 0)
</script>

<template>
  <article
    class="card card-hover group relative cursor-pointer p-4"
    :class="[enrolled ? 'opacity-70' : '', item.conflict && !enrolled ? 'border-red-200' : '']"
    @click="!disabled && !enrolled && !full && emit('select', item)"
  >
    <div class="flex items-start justify-between gap-3">
      <div class="min-w-0">
        <div class="flex flex-wrap items-center gap-2">
          <h3 class="truncate text-[15px] font-semibold text-ink">{{ item.courseName }}</h3>
          <span class="chip" :class="typeTheme(item.courseType)">{{ item.courseTypeText }}</span>
          <span v-if="item.conflict && !enrolled" class="chip bg-red-50 text-red-600">
            <AlertTriangle class="mr-1 h-3 w-3" />冲突
          </span>
          <span v-if="enrolled" class="chip bg-brand-50 text-brand-700">
            <Check class="mr-1 h-3 w-3" />已选
          </span>
        </div>
        <p class="mt-1.5 truncate text-xs text-sub">
          {{ item.teacherName }}<span v-if="item.teacherTitle"> · {{ item.teacherTitle }}</span>
          <span class="mx-1.5 text-slate-300">|</span>{{ item.courseCode }}
        </p>
      </div>
      <div class="shrink-0 text-right">
        <span class="text-lg font-semibold tabular-nums text-brand-600">{{ creditText(item.credits) }}</span>
        <span class="ml-0.5 text-[11px] text-slate-400">学分</span>
      </div>
    </div>

    <div class="mt-3 flex flex-wrap gap-x-4 gap-y-1 text-xs text-sub">
      <span class="inline-flex items-center gap-1">
        <Clock3 class="h-3.5 w-3.5 text-slate-400" />{{ item.timeText }}
      </span>
      <span class="inline-flex items-center gap-1">
        <MapPin class="h-3.5 w-3.5 text-slate-400" />{{ item.campus }} · {{ item.classroom }}
      </span>
    </div>

    <div class="mt-3.5 flex items-center gap-3">
      <div class="h-1.5 flex-1 overflow-hidden rounded-full bg-slate-100">
        <div class="h-full rounded-full transition-all duration-500" :class="remainClass(level)" :style="{ width: `${barWidth}%` }" />
      </div>
      <span class="shrink-0 text-xs tabular-nums" :class="full ? 'text-slate-400' : level === 'tight' ? 'text-amber-600' : 'text-emerald-600'">
        余量 {{ item.remaining }}/{{ item.capacity }}
      </span>
      <button
        type="button"
        class="shrink-0 rounded-lg px-3 py-1.5 text-xs font-medium transition-all"
        :class="
          enrolled
            ? 'bg-brand-50 text-brand-700'
            : full || disabled
              ? 'cursor-not-allowed bg-slate-100 text-slate-400'
              : 'bg-brand-600 text-white hover:bg-brand-700 group-hover:shadow-sm'
        "
        :disabled="enrolled || full || disabled || loading"
      >
        {{ enrolled ? '已选' : full ? '已满' : disabled ? '未开放' : loading ? '提交中' : '选课' }}
      </button>
    </div>
  </article>
</template>
