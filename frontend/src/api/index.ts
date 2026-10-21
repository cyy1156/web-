import { http } from './request'
import type {
  ClassItem,
  CourseItem,
  CreditStatus,
  EnrolledItem,
  LoginRes,
  PageResult,
  PlanRes,
  RemainItem,
  RoundInfo,
  SemesterInfo,
  StatsRes,
  TimetableItem
} from '@/types'

export interface LoginPayload {
  username: string
  password: string
}

export const apiLogin = (payload: LoginPayload) => http.post<LoginRes>('/auth/login', payload)

export const apiMe = () => http.get<LoginRes>('/auth/me')

export const apiSemester = () => http.get<SemesterInfo>('/semesters/current')

export const apiColleges = () => http.get<Array<{ id: number; name: string; code: string }>>('/colleges')

export const apiTeachers = (collegeId?: number) =>
  http.get<Array<{ id: number; name: string; title: string; collegeId: number }>>(
    '/teachers',
    collegeId ? { collegeId } : undefined
  )

export const apiCourses = (params: Record<string, unknown>) =>
  http.get<PageResult<CourseItem>>('/courses', params)

export const apiClasses = (params: Record<string, unknown>) =>
  http.get<PageResult<ClassItem>>('/teaching-classes', params)

export const apiRemain = (ids: number[]) =>
  http.post<RemainItem[]>('/teaching-classes/remain', { ids })

export const apiEnroll = (teachingClassId: number, idempotencyKey: string) =>
  http.post<{ enrollmentId: number; totalCredits: number }>(
    '/enrollments',
    { teachingClassId },
    { 'Idempotency-Key': idempotencyKey }
  )

export const apiDrop = (enrollmentId: number) => http.del<void>(`/enrollments/${enrollmentId}`)

export const apiEnrolled = (semesterId?: number) =>
  http.get<EnrolledItem[]>('/student/enrollments', semesterId ? { semesterId } : undefined)

export const apiTimetable = (semesterId?: number) =>
  http.get<{ items: TimetableItem[]; busyBitmap: number }>(
    '/student/timetable',
    semesterId ? { semesterId } : undefined
  )

export const apiCredit = (semesterId?: number) =>
  http.get<CreditStatus>('/student/credit-status', semesterId ? { semesterId } : undefined)

export const apiPlan = (semesterId?: number) =>
  http.get<PlanRes>('/student/plan', semesterId ? { semesterId } : undefined)

/* ---------------- 管理端 ---------------- */

export const apiRounds = (semesterId?: number) =>
  http.get<RoundInfo[]>('/admin/rounds', semesterId ? { semesterId } : undefined)

export const apiUpdateRound = (
  id: number,
  payload: { status?: string; startTime?: string; endTime?: string; allowDrop?: number }
) => http.put<void>(`/admin/rounds/${id}`, payload)

export const apiAdminClasses = (params: Record<string, unknown>) =>
  http.get<PageResult<ClassItem>>('/admin/classes', params)

export const apiCreateClass = (payload: Record<string, unknown>) =>
  http.post<number>('/admin/classes', payload)

export const apiUpdateClass = (id: number, payload: Record<string, unknown>) =>
  http.put<void>(`/admin/classes/${id}`, payload)

export const apiCancelClass = (id: number) => http.post<void>(`/admin/classes/${id}/cancel`)

export const apiStats = (semesterId?: number) =>
  http.get<StatsRes>('/admin/stats/overview', semesterId ? { semesterId } : undefined)

export const apiExportEnrollments = (teachingClassId: number) =>
  http.download('/admin/enrollments/export', { teachingClassId })
