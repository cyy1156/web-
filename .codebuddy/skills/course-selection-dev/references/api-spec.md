# 接口契约

## 通用约定

- Base URL：`/api`，前端开发时由 Vite 代理到 `http://localhost:8080`
- 鉴权：`Authorization: Bearer <token>`，白名单 `/api/auth/login`
- 统一响应：

```json
{ "code": 0, "message": "ok", "data": {} }
```

- 业务失败：HTTP 仍返回 200，`code` 为错误码，`message` 为可直接展示的中文文案
- 分页响应：`{ "list": [], "total": 0, "page": 1, "size": 10 }`
- 时间格式：`yyyy-MM-dd HH:mm:ss`

## 一、认证

### POST /api/auth/login

```json
{ "username": "2022001", "password": "123456" }
```

响应：

```json
{
  "code": 0,
  "data": {
    "token": "eyJhbGciOi...",
    "role": "STUDENT",
    "name": "林嘉禾",
    "studentId": 1,
    "studentNo": "2022001",
    "grade": 2022,
    "majorName": "计算机科学与技术"
  }
}
```

### GET /api/auth/me

返回同上（不含 token）。

## 二、学期与轮次

### GET /api/semesters/current

```json
{
  "code": 0,
  "data": {
    "id": 1,
    "year": 2026,
    "term": 1,
    "name": "2026-2027 学年第一学期",
    "startDate": "2026-09-07",
    "totalWeeks": 18,
    "locked": false,
    "serverTime": "2026-10-21 10:30:00",
    "round": {
      "id": 2,
      "roundNo": 1,
      "name": "第一轮 · 海选",
      "status": "OPEN",
      "startTime": "2026-10-19 09:00:00",
      "endTime": "2026-10-26 17:00:00",
      "allowDrop": true,
      "remainingSeconds": 446400
    }
  }
}
```

`remainingSeconds` 由服务端计算，前端据此倒计时插值，杜绝改本地时间绕过。

## 三、课程与教学班

### GET /api/courses

参数：`keyword` `courseType`(REQUIRED/ELECTIVE/LIMITED) `collegeId` `page=1` `size=20`

返回课程分页，元素字段：`id, code, name, credits, hours, courseType, courseTypeText, collegeName, assessment`

### GET /api/teaching-classes

参数：`semesterId`(必填) `courseId` `keyword` `collegeId` `courseType` `onlyAvailable=true` `page` `size`

```json
{
  "code": 0,
  "data": {
    "list": [{
      "id": 101,
      "courseId": 12,
      "courseCode": "MATH11001",
      "courseName": "高等数学A(1)",
      "credits": 4.0,
      "courseType": "REQUIRED",
      "courseTypeText": "必修",
      "teacherName": "张明",
      "teacherTitle": "教授",
      "times": [{ "dayOfWeek": 1, "startSection": 1, "endSection": 2, "weekType": "ALL" }],
      "timeText": "周一 1-2节",
      "campus": "军工路",
      "classroom": "一教 302",
      "capacity": 120,
      "selectedCount": 108,
      "remaining": 12,
      "status": "OPEN",
      "myStatus": "SELECTED",
      "conflict": false
    }],
    "total": 400, "page": 1, "size": 20
  }
}
```

`myStatus`：`SELECTED` / `NONE`；`conflict` 由服务端按当前学生已选位图计算，用于列表预标记。

### POST /api/teaching-classes/remain

```json
{ "ids": [101, 102, 103] }
```

```json
{ "code": 0, "data": [{ "id": 101, "remaining": 11, "selectedCount": 109, "status": "OPEN" }] }
```

前端每 3 秒批量轮询一次，一次请求带全部可见教学班 id。

## 四、选课与退课

### POST /api/enrollments

Header：`Idempotency-Key: <uuid>`

```json
{ "teachingClassId": 101 }
```

成功：

```json
{ "code": 0, "data": { "enrollmentId": 9001, "totalCredits": 18.5, "busyBitmap": "1023" } }
```

失败示例：

```json
{ "code": 40003, "message": "与《大学物理》周三 3-4 节冲突" }
```

### DELETE /api/enrollments/{id}

按 `enrollment.id` 退课，只改状态。必修课退选返回 `40008`。

### GET /api/student/enrollments?semesterId=1

返回已选清单：`id, teachingClassId, courseName, credits, courseTypeText, teacherName, timeText, classroom, conflicted`

### GET /api/student/timetable?semesterId=1

```json
{
  "code": 0,
  "data": {
    "items": [{
      "enrollmentId": 9001, "courseName": "高等数学A(1)", "teacherName": "张明",
      "classroom": "一教 302", "colorKey": 3,
      "times": [{ "dayOfWeek": 1, "startSection": 1, "endSection": 2, "weekType": "ALL" }]
    }],
    "busyBitmap": "1023"
  }
}
```

### GET /api/student/credit-status?semesterId=1

```json
{ "code": 0, "data": { "selectedCredits": 18.5, "passedCredits": 96.0, "planCredits": 24.0, "minCredits": 15.0, "maxCredits": 30.0 } }
```

## 五、管理端

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/admin/rounds?semesterId=1` | 三轮列表 |
| PUT | `/api/admin/rounds/{id}` | `{status, startTime, endTime, allowDrop}`，操作后清理前端缓存 |
| GET | `/api/admin/classes` | 分页，同教学班查询参数 + `status` |
| POST | `/api/admin/classes` | 新增教学班，`timeSlots: [{dayOfWeek,startSection,endSection,weekType}]` |
| PUT | `/api/admin/classes/{id}` | 编辑，容量不得小于已选人数 |
| POST | `/api/admin/classes/{id}/cancel` | 停开 |
| GET | `/api/admin/stats/overview?semesterId=1` | 看板数据 |
| GET | `/api/admin/enrollments/export?teachingClassId=101` | 返回 xlsx 流 |

看板响应：

```json
{
  "code": 0,
  "data": {
    "totalEnrollments": 8213,
    "avgCourses": 4.1,
    "fullClasses": 37,
    "activeRoundName": "第一轮 · 海选",
    "collegeRates": [{ "collegeName": "计算机学院", "rate": 0.92, "total": 60, "enrolled": 55 }],
    "hotCourses": [{ "courseName": "高等数学A(1)", "selected": 118, "capacity": 120 }]
  }
}
```

## 六、权限

| 路径前缀 | 允许角色 |
|---|---|
| `/api/auth/login` | 匿名 |
| `/api/student/**`、`/api/enrollments/**` | STUDENT |
| `/api/semesters/current`、`/api/courses`、`/api/teaching-classes` | STUDENT、ADMIN |
| `/api/admin/**` | ADMIN |

权限校验由 `JwtInterceptor` + `@RequireRole` 完成，越权返回 `40300`。
