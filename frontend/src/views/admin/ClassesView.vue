<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import { Download, PencilLine, Plus, Search, Trash2, X } from 'lucide-vue-next'
import {
  apiAdminClasses,
  apiCancelClass,
  apiCourses,
  apiCreateClass,
  apiExportEnrollments,
  apiTeachers,
  apiUpdateClass
} from '@/api'
import { useStudyStore } from '@/stores/study'
import type { ClassItem, CourseItem, TimeSlot } from '@/types'
import { creditText, percent, remainLevel, remainClass } from '@/utils/format'

const study = useStudyStore()

const keyword = ref('')
const status = ref<'OPEN' | 'CANCELLED' | ''>('OPEN')
const page = ref(1)
const size = 10
const total = ref(0)
const list = ref<ClassItem[]>([])
const loading = ref(false)

const courses = ref<CourseItem[]>([])
const teachers = ref<Array<{ id: number; name: string; title: string; collegeId: number }>>([])

const modalVisible = ref(false)
const modalMode = ref<'create' | 'edit'>('create')
const saving = ref(false)
const form = reactive({
  id: 0,
  courseId: 0,
  classNo: '01',
  teacherId: 0,
  capacity: 60,
  campus: '军工路',
  classroom: '',
  timeSlots: [] as TimeSlot[]
})

const confirmVisible = ref(false)
const target = ref<ClassItem | null>(null)

const pages = computed(() => Math.max(1, Math.ceil(total.value / size)))

async function load() {
  loading.value = true
  try {
    if (!study.semester) await study.loadSemester()
    const res = await apiAdminClasses({
      semesterId: study.semesterId,
      keyword: keyword.value.trim() || undefined,
      status: status.value || undefined,
      page: page.value,
      size
    })
    list.value = res.list
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function openCreate() {
  modalMode.value = 'create'
  form.id = 0
  form.courseId = courses.value[0]?.id ?? 0
  form.teacherId = teachers.value[0]?.id ?? 0
  form.classNo = '01'
  form.capacity = 60
  form.campus = '军工路'
  form.classroom = ''
  form.timeSlots = [{ dayOfWeek: 1, startSection: 1, endSection: 2, weekType: 'ALL' }]
  modalVisible.value = true
}

function openEdit(item: ClassItem) {
  modalMode.value = 'edit'
  form.id = item.id
  form.courseId = item.courseId
  form.teacherId = 0
  form.classNo = item.classNo || '01'
  form.capacity = item.capacity
  form.campus = item.campus
  form.classroom = item.classroom
  form.timeSlots = item.times.map((time) => ({ ...time }))
  modalVisible.value = true
}

async function save() {
  if (!form.courseId) {
    MessagePlugin.warning('请选择课程')
    return
  }
  if (modalMode.value === 'create' && !form.teacherId) {
    MessagePlugin.warning('请选择授课教师')
    return
  }
  if (!form.timeSlots.length) {
    MessagePlugin.warning('请至少安排一个上课时间段')
    return
  }
  const invalid = form.timeSlots.some((slot) => slot.endSection < slot.startSection)
  if (invalid) {
    MessagePlugin.warning('结束节次不能早于起始节次')
    return
  }
  saving.value = true
  try {
    const payload = {
      courseId: form.courseId,
      classNo: form.classNo || '01',
      teacherId: form.teacherId || undefined,
      capacity: Number(form.capacity),
      campus: form.campus,
      classroom: form.classroom,
      timeSlots: form.timeSlots
    }
    if (modalMode.value === 'create') {
      await apiCreateClass(payload)
      MessagePlugin.success('教学班已创建')
    } else {
      await apiUpdateClass(form.id, payload)
      MessagePlugin.success('教学班已更新')
    }
    modalVisible.value = false
    await load()
  } catch {
    // 拦截器已提示
  } finally {
    saving.value = false
  }
}

async function confirmCancel() {
  if (!target.value) return
  try {
    await apiCancelClass(target.value.id)
    MessagePlugin.success('已停开该教学班')
    confirmVisible.value = false
    await load()
  } catch {
    confirmVisible.value = false
  }
}

async function exportList(item: ClassItem) {
  const resp = await apiExportEnrollments(item.id)
  const blob = new Blob([resp.data], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `选课名单-${item.courseName}-${item.id}.csv`
  link.click()
  URL.revokeObjectURL(url)
  MessagePlugin.success('名单已导出')
}

function addSlot() {
  form.timeSlots.push({ dayOfWeek: 1, startSection: 1, endSection: 2, weekType: 'ALL' })
}

function removeSlot(index: number) {
  form.timeSlots.splice(index, 1)
}

let debounce: number | undefined
watch(keyword, () => {
  if (debounce) window.clearTimeout(debounce)
  debounce = window.setTimeout(() => {
    page.value = 1
    load()
  }, 350)
})
watch(status, () => {
  page.value = 1
  load()
})

onMounted(async () => {
  const [courseRes, teacherRes] = await Promise.all([
    apiCourses({ page: 1, size: 200 }),
    apiTeachers()
  ])
  courses.value = courseRes.list
  teachers.value = teacherRes
  await load()
})
</script>

<template>
  <div class="h-full overflow-y-auto">
    <div class="mx-auto max-w-[1320px] px-5 py-5">
      <div class="mb-4 flex flex-wrap items-end justify-between gap-3">
        <div>
          <h2 class="text-[22px] font-semibold tracking-tight text-ink">教学班管理</h2>
          <p class="mt-1 text-xs text-sub">共 {{ total }} 个教学班 · 容量不得小于已选人数</p>
        </div>
        <div class="flex items-center gap-2">
          <div class="relative">
            <Search class="pointer-events-none absolute left-2.5 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-slate-400" />
            <input v-model="keyword" class="field w-56 pl-8 text-[13px]" placeholder="课程名 / 课程代码" />
          </div>
          <select v-model="status" class="field w-28 text-[13px]">
            <option value="OPEN">开课中</option>
            <option value="CANCELLED">已停开</option>
            <option value="">全部</option>
          </select>
          <button type="button" class="btn-primary" @click="openCreate">
            <Plus class="h-4 w-4" />新增教学班
          </button>
        </div>
      </div>

      <div class="card overflow-hidden">
        <div class="overflow-x-auto">
          <div class="min-w-[1120px]">
            <div class="grid grid-cols-[1.6fr_56px_84px_1.3fr_104px_64px_96px_72px_176px] gap-2 bg-slate-50/80 px-4 py-2.5 text-[11px] font-medium text-slate-400">
              <span>课程</span>
              <span>班号</span>
              <span>教师</span>
              <span>上课时间</span>
              <span>地点</span>
              <span class="text-right">容量</span>
              <span>余量</span>
              <span class="text-center">状态</span>
              <span class="text-right">操作</span>
            </div>

            <div v-if="loading" class="space-y-1 p-3">
              <div v-for="i in 6" :key="i" class="skeleton h-9 w-full" />
            </div>

            <div
              v-if="!loading"
              v-for="item in list"
              :key="item.id"
              class="grid grid-cols-[1.6fr_56px_84px_1.3fr_104px_64px_96px_72px_176px] items-center gap-2 border-t border-line/60 px-4 py-2.5 text-[13px] hover:bg-slate-50/60"
            >
              <div class="min-w-0">
                <p class="truncate font-medium text-ink">{{ item.courseName }}</p>
                <p class="truncate text-[10px] text-slate-400">{{ item.courseCode }} · {{ creditText(item.credits) }} 学分</p>
              </div>
              <span class="text-xs text-sub">{{ item.classNo || '01' }}</span>
              <span class="truncate text-xs text-sub">{{ item.teacherName }}</span>
              <span class="truncate text-xs text-sub">{{ item.timeText }}</span>
              <span class="truncate text-xs text-sub">{{ item.classroom }}</span>
              <span class="text-right text-xs tabular-nums text-sub">{{ item.capacity }}</span>
              <div class="pr-2">
                <div class="mb-1 flex items-center justify-between text-[10px]">
                  <span :class="item.remaining === 0 ? 'text-slate-400' : 'text-slate-500'">
                    {{ item.remaining }}
                  </span>
                  <span class="text-slate-300">{{ percent(item.selectedCount, item.capacity) }}%</span>
                </div>
                <div class="h-1 overflow-hidden rounded-full bg-slate-100">
                  <div
                    class="h-full rounded-full"
                    :class="remainClass(remainLevel(item.remaining, item.capacity))"
                    :style="{ width: `${percent(item.selectedCount, item.capacity)}%` }"
                  />
                </div>
              </div>
              <span class="text-center">
                <span class="chip" :class="item.status === 'OPEN' ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-500'">
                  {{ item.status === 'OPEN' ? '开课' : '停开' }}
                </span>
              </span>
              <div class="flex items-center justify-end gap-0.5 whitespace-nowrap">
                <button type="button" class="btn-text" @click="exportList(item)">
                  <Download class="h-3 w-3" />导出
                </button>
                <button type="button" class="btn-text" @click="openEdit(item)">
                  <PencilLine class="h-3 w-3" />编辑
                </button>
                <button
                  type="button"
                  class="inline-flex items-center gap-0.5 rounded-md px-1.5 py-1 text-xs text-slate-400 transition-colors hover:bg-red-50 hover:text-red-600"
                  :disabled="item.status !== 'OPEN'"
                  @click="target = item; confirmVisible = true"
                >
                  <Trash2 class="h-3 w-3" />停开
                </button>
              </div>
            </div>

            <div v-if="!loading && !list.length" class="grid place-items-center py-14 text-xs text-slate-300">
              没有符合条件的教学班
            </div>
          </div>
        </div>

        <div class="flex items-center justify-between border-t border-line px-4 py-2.5">
          <span class="text-[11px] text-slate-400">第 {{ page }} / {{ pages }} 页</span>
          <div class="flex items-center gap-2">
            <button type="button" class="btn-ghost px-2.5 py-1 text-xs" :disabled="page <= 1" @click="page -= 1; load()">
              上一页
            </button>
            <button type="button" class="btn-ghost px-2.5 py-1 text-xs" :disabled="page >= pages" @click="page += 1; load()">
              下一页
            </button>
          </div>
        </div>
      </div>

      <div
        v-if="modalVisible"
        class="fixed inset-0 z-50 flex items-center justify-center bg-navy/40 px-4 py-6 backdrop-blur-sm"
        @click.self="modalVisible = false"
      >
        <div class="max-h-full w-full max-w-[560px] animate-fade-up overflow-y-auto rounded-2xl bg-white p-5 shadow-float">
          <div class="mb-4 flex items-center justify-between">
            <h3 class="text-base font-semibold text-ink">{{ modalMode === 'create' ? '新增教学班' : '编辑教学班' }}</h3>
            <button type="button" class="text-slate-400 hover:text-ink" @click="modalVisible = false">
              <X class="h-4 w-4" />
            </button>
          </div>

          <div class="grid gap-3 sm:grid-cols-2">
            <label class="block sm:col-span-2">
              <span class="mb-1 block text-[11px] text-slate-400">课程</span>
              <select v-model.number="form.courseId" class="field text-[13px]">
                <option v-for="course in courses" :key="course.id" :value="course.id">
                  {{ course.name }}（{{ course.code }} · {{ creditText(course.credits) }} 学分）
                </option>
              </select>
            </label>
            <label class="block">
              <span class="mb-1 block text-[11px] text-slate-400">班号</span>
              <input v-model="form.classNo" class="field text-[13px]" placeholder="01" />
            </label>
            <label class="block">
              <span class="mb-1 block text-[11px] text-slate-400">容量</span>
              <input v-model.number="form.capacity" type="number" min="1" class="field text-[13px]" />
            </label>
            <label v-if="modalMode === 'create'" class="block sm:col-span-2">
              <span class="mb-1 block text-[11px] text-slate-400">授课教师</span>
              <select v-model.number="form.teacherId" class="field text-[13px]">
                <option v-for="teacher in teachers" :key="teacher.id" :value="teacher.id">
                  {{ teacher.name }} · {{ teacher.title }}
                </option>
              </select>
            </label>
            <label class="block">
              <span class="mb-1 block text-[11px] text-slate-400">校区</span>
              <input v-model="form.campus" class="field text-[13px]" />
            </label>
            <label class="block">
              <span class="mb-1 block text-[11px] text-slate-400">教室</span>
              <input v-model="form.classroom" class="field text-[13px]" placeholder="一教 302" />
            </label>
          </div>

          <div class="mt-4">
            <div class="mb-2 flex items-center justify-between">
              <span class="text-[11px] text-slate-400">上课时间</span>
              <button type="button" class="btn-text" @click="addSlot"><Plus class="h-3 w-3" />添加时间段</button>
            </div>
            <div class="space-y-2">
              <div v-for="(slot, index) in form.timeSlots" :key="index" class="flex items-center gap-1.5">
                <select v-model.number="slot.dayOfWeek" class="field flex-1 py-1.5 text-xs">
                  <option v-for="day in 5" :key="day" :value="day">周{{ '一二三四五'[day - 1] }}</option>
                </select>
                <select v-model.number="slot.startSection" class="field w-20 py-1.5 text-xs">
                  <option v-for="n in 12" :key="n" :value="n">{{ n }}</option>
                </select>
                <span class="text-xs text-slate-300">-</span>
                <select v-model.number="slot.endSection" class="field w-20 py-1.5 text-xs">
                  <option v-for="n in 12" :key="n" :value="n">{{ n }}</option>
                </select>
                <select v-model="slot.weekType" class="field w-20 py-1.5 text-xs">
                  <option value="ALL">全周</option>
                  <option value="ODD">单周</option>
                  <option value="EVEN">双周</option>
                </select>
                <button type="button" class="text-slate-300 hover:text-red-500" @click="removeSlot(index)">
                  <X class="h-3.5 w-3.5" />
                </button>
              </div>
            </div>
          </div>

          <div class="mt-5 flex justify-end gap-2">
            <button type="button" class="btn-ghost" @click="modalVisible = false">取消</button>
            <button type="button" class="btn-primary" :disabled="saving" @click="save">
              {{ saving ? '保存中…' : '保存' }}
            </button>
          </div>
        </div>
      </div>

      <div
        v-if="confirmVisible"
        class="fixed inset-0 z-50 flex items-center justify-center bg-navy/40 px-4 backdrop-blur-sm"
        @click.self="confirmVisible = false"
      >
        <div class="w-full max-w-[360px] animate-fade-up rounded-2xl bg-white p-5 shadow-float">
          <h3 class="text-base font-semibold text-ink">停开「{{ target?.courseName }}」？</h3>
          <p class="mt-2 text-[13px] leading-relaxed text-sub">
            停开后该教学班不再出现在学生选课列表中，已选学生记录保留，需人工调整到其他教学班。
          </p>
          <div class="mt-5 flex justify-end gap-2">
            <button type="button" class="btn-ghost" @click="confirmVisible = false">取消</button>
            <button
              type="button"
              class="rounded-lg bg-red-600 px-3.5 py-2 text-sm font-medium text-white hover:bg-red-700"
              @click="confirmCancel"
            >
              确认停开
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
