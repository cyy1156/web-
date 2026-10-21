<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { BarChart, PieChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { Flame, GraduationCap, Layers, Users } from 'lucide-vue-next'
import { apiStats } from '@/api'
import { useStudyStore } from '@/stores/study'
import type { StatsRes } from '@/types'
import StatCard from '@/components/StatCard.vue'
import { percent } from '@/utils/format'

use([BarChart, PieChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const study = useStudyStore()
const stats = ref<StatsRes | null>(null)
const loading = ref(true)

const rates = computed(() => stats.value?.collegeRates ?? [])
const hot = computed(() => stats.value?.hotCourses ?? [])

const barOption = computed(() => ({
  grid: { left: 8, right: 24, top: 8, bottom: 8, containLabel: true },
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  xAxis: { type: 'value', max: 100, splitLine: { lineStyle: { color: '#EEF2F7' } }, axisLabel: { color: '#9CA3AF', fontSize: 11, formatter: '{value}%' } },
  yAxis: {
    type: 'category',
    inverse: true,
    data: rates.value.map((item) => item.collegeName),
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { color: '#6B7280', fontSize: 11 }
  },
  series: [
    {
      type: 'bar',
      barWidth: 12,
      itemStyle: {
        borderRadius: [0, 6, 6, 0],
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 1,
          y2: 0,
          colorStops: [
            { offset: 0, color: '#93C5FD' },
            { offset: 1, color: '#2563EB' }
          ]
        }
      },
      data: rates.value.map((item) => Number(((item.rate as number) * 100).toFixed(1)))
    }
  ]
}))

const pieOption = computed(() => {
  const full = stats.value?.fullClasses ?? 0
  const open = stats.value?.openClasses ?? 0
  return {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, icon: 'circle', itemWidth: 8, itemHeight: 8, textStyle: { color: '#6B7280', fontSize: 11 } },
    series: [
      {
        type: 'pie',
        radius: ['58%', '78%'],
        center: ['50%', '44%'],
        avoidLabelOverlap: true,
        itemStyle: { borderColor: '#fff', borderWidth: 3 },
        label: { show: false },
        data: [
          { value: full, name: '已满班', itemStyle: { color: '#2563EB' } },
          { value: Math.max(0, open - full), name: '有余量', itemStyle: { color: '#E5E7EB' } }
        ]
      }
    ]
  }
})

onMounted(async () => {
  try {
    if (!study.semester) await study.loadSemester()
    stats.value = await apiStats()
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="h-full overflow-y-auto">
    <div class="mx-auto max-w-[1240px] px-5 py-5">
      <div class="mb-4 flex flex-wrap items-end justify-between gap-3">
        <div>
          <h2 class="text-[22px] font-semibold tracking-tight text-ink">选课数据看板</h2>
          <p class="mt-1 text-xs text-sub">{{ study.semester?.name }} · 当前轮次 {{ stats?.activeRoundName || '—' }}</p>
        </div>
        <span class="chip bg-brand-50 px-2.5 py-1 text-brand-700">数据实时统计</span>
      </div>

      <div class="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label="选课总人次" :value="stats?.totalEnrollments ?? 0" unit="人次" hint="已选记录总数" tone="brand" />
        <StatCard label="参与选课学生" :value="stats?.totalStudents ?? 0" unit="人" hint="至少选了一门课" tone="emerald" />
        <StatCard label="人均选课门数" :value="stats?.avgCourses ?? 0" unit="门" hint="按参与学生计算" tone="violet" />
        <StatCard label="已满教学班" :value="stats?.fullClasses ?? 0" unit="个" :hint="`共 ${stats?.openClasses ?? 0} 个开班`" tone="amber" />
      </div>

      <div class="mt-3 grid gap-3 lg:grid-cols-[1.6fr_1fr]">
        <div class="card p-4">
          <div class="mb-2 flex items-center justify-between">
            <p class="flex items-center gap-1.5 text-[13px] font-semibold text-ink">
              <Layers class="h-4 w-4 text-brand-500" />各学院选课率
            </p>
            <span class="text-[11px] text-slate-400">已选 / 容量</span>
          </div>
          <div class="h-[280px]">
            <VChart v-if="rates.length" :option="barOption" autoresize class="h-full w-full" />
            <div v-else class="grid h-full place-items-center text-xs text-slate-300">暂无数据</div>
          </div>
        </div>

        <div class="card p-4">
          <p class="mb-1 flex items-center gap-1.5 text-[13px] font-semibold text-ink">
            <GraduationCap class="h-4 w-4 text-brand-500" />班级余量分布
          </p>
          <div class="h-[280px]">
            <VChart v-if="stats" :option="pieOption" autoresize class="h-full w-full" />
          </div>
        </div>
      </div>

      <div class="card mt-3 overflow-hidden">
        <div class="flex items-center justify-between border-b border-line px-4 py-3">
          <p class="flex items-center gap-1.5 text-[13px] font-semibold text-ink">
            <Flame class="h-4 w-4 text-amber-500" />选课最热 TOP10
          </p>
          <span class="text-[11px] text-slate-400">按已选人数排序</span>
        </div>
        <div class="grid grid-cols-[40px_1fr_140px_100px] gap-2 bg-slate-50/70 px-4 py-2 text-[11px] font-medium text-slate-400">
          <span>#</span>
          <span>课程</span>
          <span>容量占比</span>
          <span class="text-right">已选 / 容量</span>
        </div>
        <div
          v-for="(course, index) in hot"
          :key="String(course.courseName)"
          class="grid grid-cols-[40px_1fr_140px_100px] items-center gap-2 border-t border-line/60 px-4 py-2.5 text-[13px]"
        >
          <span class="text-xs font-semibold tabular-nums" :class="index < 3 ? 'text-amber-600' : 'text-slate-400'">
            {{ index + 1 }}
          </span>
          <span class="truncate text-ink">{{ course.courseName }}</span>
          <div class="h-1.5 overflow-hidden rounded-full bg-slate-100">
            <div
              class="h-full rounded-full bg-gradient-to-r from-brand-400 to-brand-600"
              :style="{ width: `${percent(Number(course.selected), Number(course.capacity))}%` }"
            />
          </div>
          <span class="text-right text-xs tabular-nums text-sub">
            {{ course.selected }} / {{ course.capacity }}
          </span>
        </div>
        <div v-if="!hot.length" class="grid place-items-center py-10 text-xs text-slate-300">暂无数据</div>
      </div>

      <p class="mt-4 flex items-center gap-1.5 text-[11px] text-slate-400">
        <Users class="h-3.5 w-3.5" />统计口径：仅统计 status = SELECTED 的有效选课记录
      </p>
    </div>
  </div>
</template>
