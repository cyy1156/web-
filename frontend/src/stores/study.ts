import { defineStore } from 'pinia'
import { apiCredit, apiEnrolled, apiSemester } from '@/api'
import type { CreditStatus, EnrolledItem, SemesterInfo } from '@/types'

export const useStudyStore = defineStore('study', {
  state: () => ({
    semester: null as SemesterInfo | null,
    enrolled: [] as EnrolledItem[],
    credit: null as CreditStatus | null,
    loading: false
  }),
  getters: {
    semesterId: (state) => state.semester?.id,
    roundName: (state) => state.semester?.round?.name || '未开放选课',
    roundOpen: (state) => state.semester?.round?.status === 'OPEN',
    locked: (state) => state.semester?.locked === 1,
    totalCredits: (state) => state.credit?.selectedCredits ?? 0,
    maxCredits: (state) => state.credit?.maxCredits ?? 30,
    minCredits: (state) => state.credit?.minCredits ?? 15,
    enrolledClassIds: (state) => new Set(state.enrolled.map((item) => item.teachingClassId))
  },
  actions: {
    async loadSemester() {
      this.semester = await apiSemester()
      return this.semester
    },
    async loadEnrolled() {
      this.enrolled = await apiEnrolled()
      return this.enrolled
    },
    async loadCredit() {
      this.credit = await apiCredit()
      return this.credit
    },
    async refreshAll() {
      this.loading = true
      try {
        await Promise.all([this.loadSemester(), this.loadEnrolled(), this.loadCredit()])
      } finally {
        this.loading = false
      }
    }
  }
})
