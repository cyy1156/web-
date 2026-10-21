export const WEEK_TEXT = ['一', '二', '三', '四', '五']

export function dayText(day: number) {
  return `周${WEEK_TEXT[day - 1] ?? day}`
}

export function sectionText(start: number, end: number) {
  return start === end ? `第${start}节` : `${start}-${end}节`
}

export function timeText(times: Array<{ dayOfWeek: number; startSection: number; endSection: number }>) {
  if (!times || times.length === 0) {
    return '时间待定'
  }
  return times.map((t) => `${dayText(t.dayOfWeek)} ${sectionText(t.startSection, t.endSection)}`).join(' / ')
}

export function creditText(value: number | string | null | undefined) {
  if (value === null || value === undefined || value === '') {
    return '0'
  }
  return String(Number(value)).replace(/\.0$/, '')
}

/** 秒 -> 02:13:45 */
export function countdown(seconds: number) {
  const s = Math.max(0, Math.floor(seconds))
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  const sec = s % 60
  return [h, m, sec].map((n) => String(n).padStart(2, '0')).join(':')
}

export type RemainLevel = 'plenty' | 'tight' | 'full'

export function remainLevel(remaining: number, capacity: number): RemainLevel {
  if (remaining <= 0) {
    return 'full'
  }
  const ratio = remaining / Math.max(1, capacity)
  return ratio > 0.2 ? 'plenty' : 'tight'
}

export function remainClass(level: RemainLevel) {
  if (level === 'full') return 'bg-slate-300'
  if (level === 'tight') return 'bg-amber-500'
  return 'bg-emerald-500'
}

export function percent(value: number, total: number) {
  if (!total) return 0
  return Math.min(100, Math.round((value / total) * 100))
}

export function typeTheme(type: string) {
  switch (type) {
    case 'REQUIRED':
      return 'bg-brand-50 text-brand-700'
    case 'LIMITED':
      return 'bg-violet-50 text-violet-700'
    case 'ELECTIVE':
      return 'bg-emerald-50 text-emerald-700'
    default:
      return 'bg-slate-100 text-slate-600'
  }
}

export function rndId(prefix = 'u'): string {
  const random =
    typeof crypto !== 'undefined' && crypto.randomUUID
      ? crypto.randomUUID()
      : `${Date.now()}-${Math.random()}`
  return `${prefix}-${random}`
}
