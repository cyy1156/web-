<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { MessagePlugin } from 'tdesign-vue-next'
import { ArrowRight, GraduationCap, KeyRound, ShieldCheck, Sparkles, UserRound } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()

const username = ref('20240001')
const password = ref('123456')
const loading = ref(false)

const accounts = [
  { label: '学生', user: '20240001', desc: '计算机科学与技术 2401' },
  { label: '管理员', user: 'admin', desc: '教务处' }
]

function fill(user: string) {
  username.value = user
  password.value = '123456'
}

async function submit() {
  if (!username.value.trim()) {
    MessagePlugin.warning('请输入学号或工号')
    return
  }
  if (!password.value) {
    MessagePlugin.warning('请输入密码')
    return
  }
  loading.value = true
  try {
    const res = await auth.login(username.value.trim(), password.value)
    MessagePlugin.success(`欢迎回来，${res.name}`)
    router.push(res.role === 'ADMIN' ? '/admin/dashboard' : '/selection')
  } catch {
    // 错误提示已由请求拦截器统一处理
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="flex min-h-screen bg-canvas">
    <section class="relative hidden w-[56%] overflow-hidden bg-navy lg:block">
      <div class="absolute -left-20 top-10 h-80 w-80 animate-float rounded-full bg-brand-600/30 blur-3xl" />
      <div class="absolute bottom-0 right-0 h-96 w-96 rounded-full bg-cyan-500/20 blur-3xl" />
      <div class="absolute right-24 top-1/3 h-56 w-56 animate-float rounded-full bg-violet-500/20 blur-3xl [animation-delay:2s]" />

      <div
        class="absolute inset-0 opacity-[0.07]"
        style="
          background-image:
            linear-gradient(to right, #ffffff 1px, transparent 1px),
            linear-gradient(to bottom, #ffffff 1px, transparent 1px);
          background-size: 72px 72px;
        "
      />

      <div class="relative flex h-full flex-col justify-between p-12 xl:p-16">
        <div class="flex items-center gap-3">
          <div class="grid h-11 w-11 place-items-center rounded-xl bg-brand-600 shadow-lg shadow-brand-600/40">
            <GraduationCap class="h-6 w-6 text-white" />
          </div>
          <div class="leading-tight">
            <p class="text-base font-semibold text-white">上理选课</p>
            <p class="text-[11px] tracking-wide text-white/40">COURSE SELECTION SYSTEM</p>
          </div>
        </div>

        <div class="max-w-[520px]">
          <span class="inline-flex items-center gap-1.5 rounded-full border border-white/15 bg-white/5 px-3 py-1 text-[11px] text-white/60">
            <Sparkles class="h-3 w-3" />2026-2027 学年第一学期 · 第一轮海选
          </span>
          <h1 class="mt-6 text-[42px] font-semibold leading-[1.15] tracking-tight text-white xl:text-[52px]">
            让每一次选课<br />
            <span class="bg-gradient-to-r from-brand-300 to-cyan-300 bg-clip-text text-transparent">快、准、有据可依</span>
          </h1>
          <p class="mt-5 max-w-[420px] text-sm leading-relaxed text-white/50">
            容量实时可见，冲突提前预判，学分超限即时拦截。三轮选课节奏与考核名单锁定完整复刻真实教务流程。
          </p>

          <div class="mt-10 grid grid-cols-3 gap-4">
            <div v-for="stat in [{ n: '400', t: '教学班' }, { n: '200', t: '课程' }, { n: '2000', t: '学生' }]" :key="stat.t">
              <p class="text-2xl font-semibold text-white tabular-nums">{{ stat.n }}</p>
              <p class="mt-1 text-[11px] text-white/40">{{ stat.t }}</p>
            </div>
          </div>
        </div>

        <p class="text-[11px] leading-relaxed text-white/30">
          教学演示项目，与上海理工大学官方系统无关。<br />
          所有学生姓名、学号与成绩均由脚本生成，不含任何真实个人信息。
        </p>
      </div>
    </section>

    <section class="flex flex-1 items-center justify-center px-6 py-10">
      <div class="w-full max-w-[380px] animate-fade-up">
        <div class="mb-8 lg:hidden">
          <div class="mb-3 grid h-11 w-11 place-items-center rounded-xl bg-brand-600">
            <GraduationCap class="h-6 w-6 text-white" />
          </div>
          <p class="text-lg font-semibold text-ink">上理选课</p>
          <p class="text-xs text-slate-400">教学演示系统</p>
        </div>

        <h2 class="text-[26px] font-semibold tracking-tight text-ink">登录</h2>
        <p class="mt-1.5 text-[13px] text-sub">使用学号或工号进入系统</p>

        <form class="mt-8 space-y-4" @submit.prevent="submit">
          <label class="block">
            <span class="mb-1.5 block text-xs font-medium text-sub">学号 / 工号</span>
            <div class="group relative">
              <UserRound class="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
              <input
                v-model="username"
                class="field pl-9"
                placeholder="请输入学号或工号"
                autocomplete="username"
              />
            </div>
          </label>

          <label class="block">
            <span class="mb-1.5 block text-xs font-medium text-sub">密码</span>
            <div class="group relative">
              <KeyRound class="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
              <input
                v-model="password"
                type="password"
                class="field pl-9"
                placeholder="请输入密码"
                autocomplete="current-password"
              />
            </div>
          </label>

          <button type="submit" class="btn-primary w-full py-2.5" :disabled="loading">
            <span>{{ loading ? '正在登录…' : '进入系统' }}</span>
            <ArrowRight v-if="!loading" class="h-4 w-4" />
          </button>
        </form>

        <div class="mt-7 rounded-xl border border-line bg-white/70 p-3">
          <p class="mb-2 flex items-center gap-1.5 text-[11px] font-medium text-slate-500">
            <ShieldCheck class="h-3.5 w-3.5" />演示账号（点击填充，密码均为 123456）
          </p>
          <div class="grid grid-cols-2 gap-2">
            <button
              v-for="account in accounts"
              :key="account.user"
              type="button"
              class="rounded-lg border border-line bg-white px-2.5 py-2 text-left transition-colors hover:border-brand-200 hover:bg-brand-50"
              @click="fill(account.user)"
            >
              <p class="text-xs font-medium text-ink">{{ account.label }}</p>
              <p class="mt-0.5 truncate text-[10px] text-slate-400">{{ account.user }}</p>
            </button>
          </div>
        </div>

        <p class="mt-8 text-center text-[11px] text-slate-400">
          登录即表示同意本系统仅用于课程设计演示
        </p>
      </div>
    </section>
  </div>
</template>
