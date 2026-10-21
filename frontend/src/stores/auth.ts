import { defineStore } from 'pinia'
import { apiLogin, apiMe } from '@/api'
import { NAME_KEY, ROLE_KEY, TOKEN_KEY } from '@/api/request'
import type { LoginRes } from '@/types'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    role: (localStorage.getItem(ROLE_KEY) || '') as 'STUDENT' | 'ADMIN' | '',
    name: localStorage.getItem(NAME_KEY) || '',
    profile: null as LoginRes | null
  }),
  getters: {
    isStudent: (state) => state.role === 'STUDENT',
    isAdmin: (state) => state.role === 'ADMIN',
    displayName: (state) => state.profile?.name || state.name || '同学',
    studentNo: (state) => state.profile?.studentNo || '',
    majorName: (state) => state.profile?.majorName || '',
    className: (state) => state.profile?.className || ''
  },
  actions: {
    async login(username: string, password: string) {
      const res = await apiLogin({ username, password })
      this.profile = res
      this.token = res.token
      this.role = res.role
      this.name = res.name
      localStorage.setItem(TOKEN_KEY, res.token)
      localStorage.setItem(ROLE_KEY, res.role)
      localStorage.setItem(NAME_KEY, res.name)
      return res
    },
    /** 刷新页面后补全专业、班级等信息 */
    async hydrate() {
      if (!this.token || this.profile) return
      try {
        const me = await apiMe()
        this.profile = {
          token: this.token,
          role: me.role,
          name: me.name,
          userId: 0,
          studentId: me.studentId,
          studentNo: me.studentNo,
          grade: me.grade,
          majorName: me.majorName,
          className: me.className
        }
        this.name = me.name
        localStorage.setItem(NAME_KEY, me.name)
      } catch {
        // 登录态失效由拦截器处理
      }
    },
    logout() {
      this.token = ''
      this.role = ''
      this.name = ''
      this.profile = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(ROLE_KEY)
      localStorage.removeItem(NAME_KEY)
    }
  }
})
