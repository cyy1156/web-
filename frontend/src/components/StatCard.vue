<script setup lang="ts">
defineProps<{
  label: string
  value: string | number
  unit?: string
  hint?: string
  tone?: 'brand' | 'emerald' | 'amber' | 'violet'
}>()

const tones: Record<string, string> = {
  brand: 'from-brand-500/12 to-brand-500/0 text-brand-600',
  emerald: 'from-emerald-500/12 to-emerald-500/0 text-emerald-600',
  amber: 'from-amber-500/12 to-amber-500/0 text-amber-600',
  violet: 'from-violet-500/12 to-violet-500/0 text-violet-600'
}
</script>

<template>
  <div class="card relative overflow-hidden p-5">
    <div class="pointer-events-none absolute inset-x-0 top-0 h-20 bg-gradient-to-b" :class="tones[tone ?? 'brand']" />
    <p class="relative text-[13px] text-sub">{{ label }}</p>
    <div class="relative mt-2 flex items-baseline gap-1">
      <span class="text-[28px] font-semibold leading-none tracking-tight text-ink tabular-nums">{{ value }}</span>
      <span v-if="unit" class="text-xs text-slate-400">{{ unit }}</span>
    </div>
    <p v-if="hint" class="relative mt-2 text-xs text-slate-400">{{ hint }}</p>
  </div>
</template>
