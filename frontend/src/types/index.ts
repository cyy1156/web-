export interface LoginRes {
  token: string
  role: 'STUDENT' | 'ADMIN'
  name: string
  userId: number
  studentId: number | null
  studentNo: string | null
  grade: number | null
  majorName: string | null
  className: string | null
}

export interface RoundInfo {
  id: number
  roundNo: number
  name: string
  status: 'PENDING' | 'OPEN' | 'CLOSED'
  startTime: string
  endTime: string
  allowDrop: number
  remainingSeconds: number
}

export interface SemesterInfo {
  id: number
  year: number
  term: number
  name: string
  startDate: string
  totalWeeks: number
  currentWeek: number
  locked: number
  serverTime: string
  round: RoundInfo | null
}

export interface TimeSlot {
  dayOfWeek: number
  startSection: number
  endSection: number
  weekType: string
}

export interface ClassItem {
  id: number
  courseId: number
  classNo: string
  courseCode: string
  courseName: string
  credits: number
  courseType: string
  courseTypeText: string
  teacherName: string
  teacherTitle: string
  times: TimeSlot[]
  timeText: string
  campus: string
  classroom: string
  capacity: number
  selectedCount: number
  remaining: number
  status: string
  myStatus: 'SELECTED' | 'NONE'
  conflict: boolean
}

export interface CourseItem {
  id: number
  code: string
  name: string
  credits: number
  hours: number
  courseType: string
  courseTypeText: string
  assessment: string
  collegeId: number
  collegeName: string
  description: string
}

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  size: number
}

export interface EnrolledItem {
  id: number
  teachingClassId: number
  courseId: number
  courseName: string
  credits: number
  courseType: string
  courseTypeText: string
  teacherName: string
  timeText: string
  campus: string
  classroom: string
  conflicted: boolean
}

export interface CreditStatus {
  name: string
  studentNo: string
  majorName: string
  className: string
  selectedCredits: number
  passedCredits: number
  planCredits: number
  minCredits: number
  maxCredits: number
}

export interface TimetableItem {
  enrollmentId: number
  courseId: number
  courseName: string
  teacherName: string
  classroom: string
  courseType: string
  colorKey: number
  times: TimeSlot[]
}

export interface PlanCourse {
  courseId: number
  courseName: string
  credits: number
  requiredType: string
  status: '已完成' | '在修' | '未修' | '未通过'
  score: number | null
}

export interface PlanTerm {
  termNo: number
  courses: PlanCourse[]
}

export interface PlanRes {
  terms: PlanTerm[]
  passedCredits: number
  earningCredits: number
  requiredCredits: number
}

export interface StatsRes {
  totalEnrollments: number
  totalStudents: number
  avgCourses: number
  fullClasses: number
  openClasses: number
  activeRoundName: string
  collegeRates: Array<Record<string, number | string>>
  hotCourses: Array<Record<string, number | string>>
}

export interface RemainItem {
  id: number
  remaining: number
  selectedCount: number
  status: string
}
