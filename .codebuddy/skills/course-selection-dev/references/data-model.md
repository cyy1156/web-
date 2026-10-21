# 数据模型

数据库：MySQL 8，字符集 `utf8mb4`，排序规则 `utf8mb4_0900_ai_ci`。建表脚本唯一来源：`db/schema.sql`。

## 一、表清单（12 张）

| 表 | 作用 | 关键字段 |
|---|---|---|
| `college` | 学院 | name |
| `major` | 专业 | college_id, name, grade |
| `student` | 学生 | student_no(U), name, grade, major_id, class_name |
| `teacher` | 教师（仅作为教学班字段，无登录） | teacher_no, name, title, college_id |
| `course` | 课程库 | code(U), name, credits, hours, course_type, college_id |
| `course_prerequisite` | 先修关系 | course_id, pre_course_id, min_score |
| `training_plan` | 培养计划 | major_id, grade, term_no, course_id, required_type |
| `semester` | 学期 | year, term, start_date, total_weeks, locked |
| `selection_round` | 选课轮次 | semester_id, round_no, start_time, end_time, allow_drop, status |
| `teaching_class` | 教学班 | semester_id, course_id, teacher_id, capacity, selected_count, time_bitmap, status |
| `class_time` | 上课时间明细（展示用） | teaching_class_id, day_of_week, start_section, end_section, week_type |
| `enrollment` | 选课记录 | student_id, teaching_class_id, course_id, credits, status, status_flag |
| `enrollment_log` | 操作审计 + 幂等 | student_id, teaching_class_id, action, result_code, idempotency_key |

> `sys_user` 不在上表内，用于登录账号：`username(U), password(BCrypt), role, ref_id, status`。

## 二、必须守住的不变量

1. `teaching_class.selected_count` 恒满足 `0 <= selected_count <= capacity`，由 `CHECK` 约束 + 原子 UPDATE 双重保证。
2. `enrollment` 中 `status='SELECTED'` 的记录数，必须等于对应教学班的 `selected_count`。可用 `scripts/check_consistency.py` 对账。
3. 同一学生同一学期同一课程，至多一条 `SELECTED` 记录。
4. `time_bitmap` 与 `class_time` 必须保持一致，写教学班时两者同事务写入。
5. `enrollment_log.idempotency_key` 全局唯一，重放直接读日志表返回。

## 三、关键设计说明

### status_flag 生成列

```sql
status_flag TINYINT GENERATED ALWAYS AS (IF(status = 'SELECTED', 1, NULL)) STORED
```

MySQL 唯一索引对 `NULL` 不生效，因此：

- 选课：`(student_id, course_id, semester_id, 1)` 唯一，防重复选课
- 退课：`status='DROPPED'` 使 `status_flag` 变 NULL，唯一键失效，可再次选课
- 好处：一张表同时实现"防重复"与"可重选"，无需额外的有效标记表

### time_bitmap 位图

- 位索引 = `(dayOfWeek - 1) * 12 + (section - 1)`
- 一门课占用的时间点合并为一个 BIGINT：`mask |= ((1L << count) - 1) << start`
- 冲突判定：`(studentBusy & classMask) != 0`
- 学生已选位图：`SELECT BIT_OR(tc.time_bitmap) FROM enrollment e JOIN teaching_class tc ...`，或后端遍历已选记录用 OR 合并（单学生一学期不超过 12 门，代价可忽略）

## 四、索引清单

| 表 | 索引 | 用途 |
|---|---|---|
| `enrollment` | `uk_stu_class(student_id, teaching_class_id, semester_id, status_flag)` | 防重复选同一班 |
| `enrollment` | `uk_stu_course(student_id, course_id, semester_id, status_flag)` | 防同课程多班 |
| `enrollment` | `idx_stu_sem(student_id, semester_id, status)` | 我的课表 / 学分汇总 |
| `teaching_class` | `uk_sem_course_class(semester_id, course_id, class_no)` | 班号唯一 |
| `teaching_class` | `idx_sem_status(semester_id, status)` | 列表筛选 |
| `class_time` | `idx_class(teaching_class_id)` | 课表关联 |
| `selection_round` | `uk_sem_round(semester_id, round_no)` | 轮次唯一 |
| `course_prerequisite` | `uk_course_pre(course_id, pre_course_id)` | 先修校验 |

## 五、容量扣减的唯一写法

```sql
UPDATE teaching_class
SET selected_count = selected_count + 1
WHERE id = #{classId}
  AND status = 'OPEN'
  AND selected_count < capacity;
```

影响行数为 0 → 抛 `40002` 并回滚，禁止使用"先 SELECT 余量再 UPDATE"的写法。

## 六、种子数据规模

| 实体 | 数量 |
|---|---|
| 学院 / 专业 | 8 / 20 |
| 课程 | 200 |
| 教师 | 120 |
| 教学班 | 400 |
| 学生 | 2000 |
| 选课记录 | 约 8000 |
| 历史成绩 | 6000 |
