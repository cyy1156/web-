-- =====================================================================
-- 高校选课系统 建表脚本（对标上海理工大学教务选课办法）
-- MySQL 8.4 / utf8mb4 / InnoDB
-- 使用：mysql -uroot course_selection < db/schema.sql
-- =====================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS enrollment_log;
DROP TABLE IF EXISTS enrollment;
DROP TABLE IF EXISTS class_time;
DROP TABLE IF EXISTS teaching_class;
DROP TABLE IF EXISTS selection_round;
DROP TABLE IF EXISTS credit_rule;
DROP TABLE IF EXISTS course_grade;
DROP TABLE IF EXISTS training_plan;
DROP TABLE IF EXISTS course_prerequisite;
DROP TABLE IF EXISTS course;
DROP TABLE IF EXISTS teacher;
DROP TABLE IF EXISTS student;
DROP TABLE IF EXISTS major;
DROP TABLE IF EXISTS college;
DROP TABLE IF EXISTS semester;
DROP TABLE IF EXISTS sys_user;

-- ---------------------------------------------------------------------
-- 基础数据
-- ---------------------------------------------------------------------
CREATE TABLE college (
  id    BIGINT PRIMARY KEY AUTO_INCREMENT,
  code  VARCHAR(16) NOT NULL,
  name  VARCHAR(64) NOT NULL,
  UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学院';

CREATE TABLE major (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  college_id BIGINT NOT NULL,
  code       VARCHAR(16) NOT NULL,
  name       VARCHAR(64) NOT NULL,
  grade      INT NOT NULL,
  KEY idx_college (college_id),
  UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='专业';

CREATE TABLE student (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  student_no VARCHAR(16) NOT NULL,
  name       VARCHAR(32) NOT NULL,
  gender     CHAR(1) NOT NULL DEFAULT 'M',
  grade      INT NOT NULL,
  major_id   BIGINT NOT NULL,
  class_name VARCHAR(32) NOT NULL,
  UNIQUE KEY uk_no (student_no),
  KEY idx_major (major_id),
  KEY idx_grade (grade)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生';

CREATE TABLE teacher (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  teacher_no VARCHAR(16) NOT NULL,
  name       VARCHAR(32) NOT NULL,
  title      VARCHAR(16) NOT NULL DEFAULT '讲师',
  college_id BIGINT NOT NULL,
  UNIQUE KEY uk_no (teacher_no),
  KEY idx_college (college_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师（仅作为教学班属性，无登录）';

CREATE TABLE course (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  code        VARCHAR(16) NOT NULL,
  name        VARCHAR(64) NOT NULL,
  credits     DECIMAL(3,1) NOT NULL COMMENT '学分',
  hours       INT NOT NULL COMMENT '学时',
  course_type VARCHAR(16) NOT NULL COMMENT 'REQUIRED 必修 / LIMITED 限选 / ELECTIVE 任选',
  assessment  VARCHAR(16) NOT NULL DEFAULT 'EXAM' COMMENT 'EXAM 考试 / CHECK 考查',
  college_id  BIGINT NOT NULL,
  description VARCHAR(255) DEFAULT NULL,
  UNIQUE KEY uk_code (code),
  KEY idx_college (college_id),
  KEY idx_type (course_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程库';

CREATE TABLE course_prerequisite (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  course_id   BIGINT NOT NULL,
  pre_course_id BIGINT NOT NULL,
  min_score   INT NOT NULL DEFAULT 60,
  UNIQUE KEY uk_course_pre (course_id, pre_course_id),
  KEY idx_pre (pre_course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='先修课程关系';

CREATE TABLE training_plan (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  major_id      BIGINT NOT NULL,
  grade         INT NOT NULL,
  term_no       TINYINT NOT NULL COMMENT '建议修读学期 1-8',
  course_id     BIGINT NOT NULL,
  required_type VARCHAR(16) NOT NULL DEFAULT 'COMPULSORY' COMMENT 'COMPULSORY / ELECTIVE',
  KEY idx_major_term (major_id, term_no),
  UNIQUE KEY uk_plan (major_id, grade, term_no, course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='培养计划';

CREATE TABLE course_grade (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  student_id BIGINT NOT NULL,
  course_id  BIGINT NOT NULL,
  score      INT NOT NULL,
  term_no    TINYINT NOT NULL,
  passed     TINYINT NOT NULL DEFAULT 1,
  KEY idx_stu (student_id),
  KEY idx_stu_course (student_id, course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='历史成绩（先修校验与已修学分）';

CREATE TABLE credit_rule (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  major_id     BIGINT NOT NULL,
  term_no      TINYINT NOT NULL,
  plan_credits DECIMAL(4,1) NOT NULL COMMENT '培养计划建议学分',
  min_credits  DECIMAL(4,1) NOT NULL DEFAULT 15.0,
  max_credits  DECIMAL(4,1) NOT NULL DEFAULT 30.0,
  UNIQUE KEY uk_major_term (major_id, term_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学分上下限规则';

-- ---------------------------------------------------------------------
-- 学期与轮次
-- ---------------------------------------------------------------------
CREATE TABLE semester (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  year        INT NOT NULL,
  term        TINYINT NOT NULL,
  name        VARCHAR(64) NOT NULL,
  start_date  DATE NOT NULL,
  total_weeks INT NOT NULL DEFAULT 18,
  current_week INT NOT NULL DEFAULT 1,
  locked      TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_year_term (year, term)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学期';

CREATE TABLE selection_round (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  semester_id BIGINT NOT NULL,
  round_no   TINYINT NOT NULL COMMENT '1 海选 / 2 补选 / 3 退改选',
  name       VARCHAR(32) NOT NULL,
  start_time DATETIME NOT NULL,
  end_time   DATETIME NOT NULL,
  allow_drop TINYINT NOT NULL DEFAULT 1,
  status     VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/OPEN/CLOSED',
  UNIQUE KEY uk_sem_round (semester_id, round_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='选课轮次';

-- ---------------------------------------------------------------------
-- 教学班：超卖防线
-- ---------------------------------------------------------------------
CREATE TABLE teaching_class (
  id             BIGINT PRIMARY KEY AUTO_INCREMENT,
  semester_id    BIGINT NOT NULL,
  course_id      BIGINT NOT NULL,
  class_no       VARCHAR(16) NOT NULL,
  teacher_id     BIGINT NOT NULL,
  capacity       INT NOT NULL,
  selected_count INT NOT NULL DEFAULT 0,
  time_bitmap    BIGINT NOT NULL DEFAULT 0 COMMENT '周1-5 × 12节 = 60bit',
  campus         VARCHAR(32) DEFAULT '军工路',
  classroom      VARCHAR(64) DEFAULT NULL,
  status         VARCHAR(16) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/CANCELLED',
  UNIQUE KEY uk_sem_course_class (semester_id, course_id, class_no),
  KEY idx_sem_status (semester_id, status),
  KEY idx_teacher (teacher_id),
  CONSTRAINT ck_capacity CHECK (selected_count >= 0 AND selected_count <= capacity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教学班';

CREATE TABLE class_time (
  id                BIGINT PRIMARY KEY AUTO_INCREMENT,
  teaching_class_id BIGINT NOT NULL,
  day_of_week       TINYINT NOT NULL COMMENT '1-5 周一到周五',
  start_section     TINYINT NOT NULL COMMENT '1-12 节',
  end_section       TINYINT NOT NULL,
  week_type         VARCHAR(8) NOT NULL DEFAULT 'ALL' COMMENT 'ALL/ODD/EVEN',
  KEY idx_class (teaching_class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='上课时间明细';

-- ---------------------------------------------------------------------
-- 选课记录：status_flag 生成列实现"防重复 + 可重选"
-- ---------------------------------------------------------------------
CREATE TABLE enrollment (
  id                BIGINT PRIMARY KEY AUTO_INCREMENT,
  student_id        BIGINT NOT NULL,
  semester_id       BIGINT NOT NULL,
  teaching_class_id BIGINT NOT NULL,
  course_id         BIGINT NOT NULL,
  round_id          BIGINT NOT NULL,
  credits           DECIMAL(3,1) NOT NULL,
  status            VARCHAR(16) NOT NULL DEFAULT 'SELECTED' COMMENT 'SELECTED/DROPPED',
  status_flag       TINYINT GENERATED ALWAYS AS (IF(status = 'SELECTED', 1, NULL)) STORED,
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  dropped_at        DATETIME NULL,
  UNIQUE KEY uk_stu_class (student_id, teaching_class_id, semester_id, status_flag),
  UNIQUE KEY uk_stu_course (student_id, course_id, semester_id, status_flag),
  KEY idx_stu_sem (student_id, semester_id, status),
  KEY idx_class_status (teaching_class_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='选课记录';

CREATE TABLE enrollment_log (
  id                BIGINT PRIMARY KEY AUTO_INCREMENT,
  student_id        BIGINT NOT NULL,
  teaching_class_id BIGINT NOT NULL,
  action            VARCHAR(16) NOT NULL COMMENT 'ENROLL/DROP',
  result_code       INT NOT NULL,
  message           VARCHAR(255) DEFAULT NULL,
  idempotency_key   VARCHAR(64) DEFAULT NULL,
  cost_ms           INT DEFAULT NULL,
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_idem (idempotency_key),
  KEY idx_stu_time (student_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='选课操作审计 + 幂等';

-- ---------------------------------------------------------------------
-- 登录账号
-- ---------------------------------------------------------------------
CREATE TABLE sys_user (
  id       BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(32) NOT NULL,
  password VARCHAR(72) NOT NULL COMMENT 'BCrypt',
  role     VARCHAR(16) NOT NULL COMMENT 'STUDENT/ADMIN',
  ref_id   BIGINT DEFAULT NULL COMMENT '学生id或管理员id',
  status   TINYINT NOT NULL DEFAULT 1,
  UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录账号';

SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------
-- 常用视图：带余量的教学班（管理端与统计复用）
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW v_teaching_class AS
SELECT tc.*, c.code AS course_code, c.name AS course_name, c.credits, c.course_type,
       t.name AS teacher_name, t.title AS teacher_title,
       (tc.capacity - tc.selected_count) AS remaining
FROM teaching_class tc
JOIN course c ON c.id = tc.course_id
JOIN teacher t ON t.id = tc.teacher_id;
