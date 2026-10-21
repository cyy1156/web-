#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
选课系统种子数据生成器

用法：
    python scripts/gen_seed.py --students 2000 --classes 400 --output db/seed.sql

产物：一份可直接导入 MySQL 的 SQL 文件。
说明：学生姓名、学号、成绩全部由脚本生成，与任何真实个人信息无关；
      课程名参考高校公开培养计划的常见课程设置，仅用于教学演示。
"""

import argparse
import datetime as dt
import random
from pathlib import Path

# 123456 的 BCrypt 哈希（bcrypt / cost=10），全站演示账号统一密码
PWD_HASH = "$2b$10$RU41qjWiZIuuozdrBodiCuZ5X5dCPD4iEPes4MNyg0YBy.Zpy4L8W"

COLLEGES = [
    ("01", "能源与动力工程学院"),
    ("02", "光电信息与计算机工程学院"),
    ("03", "管理学院"),
    ("04", "机械工程学院"),
    ("05", "外语学院"),
    ("06", "环境与建筑学院"),
    ("07", "理学院"),
    ("08", "出版印刷与艺术设计学院"),
]

MAJORS = {
    "01": ["能源与动力工程", "新能源科学与工程"],
    "02": ["计算机科学与技术", "光电信息科学与工程", "电子信息工程"],
    "03": ["工商管理", "会计学", "信息管理与信息系统"],
    "04": ["机械设计制造及其自动化", "车辆工程"],
    "05": ["英语", "日语"],
    "06": ["环境工程", "土木工程"],
    "07": ["数学与应用数学", "应用物理学"],
    "08": ["视觉传达设计", "编辑出版学", "数字媒体艺术"],
}

COURSE_POOL = {
    "01": ["工程热力学", "传热学", "流体力学", "燃烧学", "制冷原理与设备", "锅炉原理", "汽轮机原理",
           "热力发电厂", "能源与环境", "新能源技术", "风力发电技术", "太阳能利用技术", "内燃机原理",
           "热工测试技术", "泵与风机", "热力系统仿真", "储能原理与技术", "能源系统工程", "动力机械设计",
           "工程计算方法", "节能减排技术", "氢能与燃料电池", "电厂电气设备", "热工过程自动控制", "专业英语(能动)"],
    "02": ["程序设计基础(C语言)", "数据结构", "计算机组成原理", "操作系统", "计算机网络", "数据库原理",
           "软件工程", "算法设计与分析", "面向对象程序设计(Java)", "Web应用开发", "人工智能导论", "机器学习",
           "计算机图形学", "编译原理", "嵌入式系统", "数字逻辑电路", "模拟电子技术", "数字电子技术",
           "信号与系统", "光电检测技术", "应用光学", "激光原理与技术", "光纤通信", "数字图像处理", "信息安全基础"],
    "03": ["管理学原理", "微观经济学", "宏观经济学", "会计学原理", "财务管理", "市场营销学", "运营管理",
           "人力资源管理", "管理信息系统", "统计学", "运筹学", "国际贸易实务", "电子商务概论", "供应链管理",
           "公司战略与风险管理", "审计学", "成本会计", "税法", "商务数据分析", "Python数据分析",
           "组织行为学", "投资学", "项目管理", "消费者行为学", "专业英语(管理)"],
    "04": ["机械制图", "理论力学", "材料力学", "机械原理", "机械设计", "工程材料", "材料成型技术基础",
           "机械制造技术", "液压与气压传动", "机械工程测试技术", "数控技术", "先进制造技术", "汽车构造",
           "汽车理论", "汽车设计", "车辆电子控制技术", "有限元分析", "CAD/CAM技术", "工业机器人",
           "精密测量技术", "模具设计", "机械振动", "热工基础", "智能制造导论", "专业英语(机械)"],
    "05": ["综合英语(1)", "综合英语(2)", "英语听力", "英语口语", "英语写作", "英语阅读", "翻译理论与实践",
           "语言学导论", "英美文学", "跨文化交际", "商务英语", "高级英语", "日语精读(1)", "日语听力",
           "日本概况", "第二外语(德语)", "英语语法", "口译基础", "学术写作", "英语国家社会与文化",
           "同声传译入门", "语料库语言学", "英语词汇学", "报刊选读", "专业英语(外语)"],
    "06": ["环境工程原理", "水污染控制工程", "大气污染控制工程", "固体废物处理与处置", "环境监测",
           "环境微生物学", "环境影响评价", "给排水工程", "土木工程材料", "结构力学", "混凝土结构设计",
           "钢结构设计", "土力学与地基基础", "工程测量", "建筑构造", "建筑设备工程", "施工组织与管理",
           "工程经济学", "建筑CAD", "暖通空调", "建筑节能技术", "环境规划与管理", "清洁生产", "工程地质",
           "专业英语(环境)"],
    "07": ["高等数学A(1)", "高等数学A(2)", "线性代数", "概率论与数理统计", "大学物理(1)", "大学物理(2)",
           "大学物理实验", "数学分析(1)", "数学分析(2)", "高等代数", "常微分方程", "复变函数与积分变换",
           "数值分析", "数学建模", "实变函数", "泛函分析", "概率论", "数理统计", "理论力学(理科)",
           "量子力学", "电动力学", "热力学与统计物理", "光学", "原子物理", "计算物理"],
    "08": ["出版学概论", "编辑学", "数字出版技术", "印刷原理与工艺", "色彩管理", "版式设计", "平面设计基础",
           "字体设计", "包装设计", "视觉传达设计", "插画设计", "数字媒体艺术", "摄影基础", "影视后期制作",
           "交互设计", "用户研究方法", "品牌设计", "广告策划", "书籍装帧设计", "三维建模(3ds Max)",
           "网页设计", "动画原理", "印刷材料学", "出版法规", "专业英语(出版)"],
}

# 先修关系：后修课程 -> (先修课程, 最低分)
PREREQ = [
    ("高等数学A(2)", "高等数学A(1)", 60),
    ("大学物理(2)", "大学物理(1)", 60),
    ("数学分析(2)", "数学分析(1)", 60),
    ("综合英语(2)", "综合英语(1)", 60),
    ("数据结构", "程序设计基础(C语言)", 60),
    ("操作系统", "数据结构", 60),
    ("数据库原理", "数据结构", 60),
    ("机械设计", "机械原理", 60),
    ("量子力学", "大学物理(2)", 60),
    ("混凝土结构设计", "结构力学", 60),
]

SURNAMES = "赵钱孙李周吴郑王冯陈褚卫蒋沈韩杨朱秦尤许何吕施张孔曹严华金魏陶姜戚谢邹喻柏水窦章"
GIVEN = ["子涵", "欣怡", "浩然", "雨轩", "思远", "嘉禾", "宇辰", "梦琪", "俊杰", "雨萌",
         "文博", "诗涵", "泽宇", "语桐", "梓萱", "书航", "一鸣", "若曦", "天佑", "晨曦",
         "明轩", "佳怡", "皓宇", "静怡", "子墨", "可欣", "博文", "嘉懿", "思琪", "云帆"]

TITLES = ["教授", "副教授", "讲师", "副教授", "讲师", "讲师"]

CAPACITIES = [30, 60, 60, 120, 120, 120]
BUILDINGS = ["一教", "二教", "三教", "卓越楼", "光电楼", "管理楼"]
WEEK_TYPES = ["ALL", "ALL", "ALL", "ALL", "ODD", "EVEN"]


def slot_mask(day, start, end):
    """把 (星期, 起始节, 结束节) 转成 60 位位图"""
    width = end - start + 1
    return ((1 << width) - 1) << ((day - 1) * 12 + start - 1)


def bitmap_of(slots):
    m = 0
    for day, start, end in slots:
        m |= slot_mask(day, start, end)
    return m


def sql(s):
    if s is None:
        return "NULL"
    return "'" + str(s).replace("\\", "\\\\").replace("'", "''") + "'"


class Writer:
    def __init__(self):
        self.buf = []
        self.rows = 0

    def insert(self, table, columns, values, batch=200):
        if not values:
            return
        head = f"INSERT INTO {table} ({', '.join(columns)}) VALUES\n"
        for i in range(0, len(values), batch):
            chunk = values[i:i + batch]
            self.buf.append(head + ",\n".join(chunk) + ";\n")
            self.rows += len(chunk)

    def raw(self, text):
        self.buf.append(text + "\n")

    def dump(self, path):
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text("".join(self.buf), encoding="utf-8")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--students", type=int, default=2000)
    ap.add_argument("--classes", type=int, default=400)
    ap.add_argument("--output", default="db/seed.sql")
    ap.add_argument("--seed", type=int, default=42)
    args = ap.parse_args()

    random.seed(args.seed)
    w = Writer()
    w.raw("-- 由 scripts/gen_seed.py 自动生成，请勿手工编辑")
    w.raw("SET NAMES utf8mb4;")
    w.raw("SET FOREIGN_KEY_CHECKS = 1;")
    w.raw("TRUNCATE TABLE enrollment_log;")
    w.raw("TRUNCATE TABLE enrollment;")
    w.raw("TRUNCATE TABLE class_time;")
    w.raw("TRUNCATE TABLE teaching_class;")
    w.raw("TRUNCATE TABLE selection_round;")
    w.raw("TRUNCATE TABLE credit_rule;")
    w.raw("TRUNCATE TABLE course_grade;")
    w.raw("TRUNCATE TABLE training_plan;")
    w.raw("TRUNCATE TABLE course_prerequisite;")
    w.raw("TRUNCATE TABLE course;")
    w.raw("TRUNCATE TABLE teacher;")
    w.raw("TRUNCATE TABLE student;")
    w.raw("TRUNCATE TABLE major;")
    w.raw("TRUNCATE TABLE college;")
    w.raw("TRUNCATE TABLE semester;")
    w.raw("TRUNCATE TABLE sys_user;")

    # ---------------- 学院 / 专业 ----------------
    w.insert("college", ["id", "code", "name"],
             [f"({i + 1}, {sql(c)}, {sql(n)})" for i, (c, n) in enumerate(COLLEGES)])

    major_rows, major_desc = [], []  # (id, college_id, code, name, grade)
    mid = 0
    for ci, (ccode, _) in enumerate(COLLEGES, start=1):
        for name in MAJORS[ccode]:
            mid += 1
            major_rows.append(f"({mid}, {ci}, {sql('M%03d' % mid)}, {sql(name)}, 2024)")
            major_desc.append((mid, ci, name))
    w.insert("major", ["id", "college_id", "code", "name", "grade"], major_rows)
    major_college = {mid_: ci for mid_, ci, _ in major_desc}

    # ---------------- 教师 ----------------
    teacher_rows, teachers_by_college = [], {i: [] for i in range(1, 9)}
    tid = 0
    for ci in range(1, 9):
        for _ in range(15):
            tid += 1
            name = random.choice(SURNAMES) + random.choice(GIVEN)
            teacher_rows.append(
                f"({tid}, {sql('T%04d' % tid)}, {sql(name)}, {sql(random.choice(TITLES))}, {ci})")
            teachers_by_college[ci].append(tid)
    w.insert("teacher", ["id", "teacher_no", "name", "title", "college_id"], teacher_rows)

    # ---------------- 课程 ----------------
    course_rows, course_meta = [], {}  # id -> dict(name, college_id, credits, type, term)
    cid = 0
    for ci, (ccode, _) in enumerate(COLLEGES, start=1):
        for idx, name in enumerate(COURSE_POOL[ccode]):
            cid += 1
            if idx < 14:
                ctype, credits = "REQUIRED", random.choice([3.0, 4.0, 5.0])
            elif idx < 20:
                ctype, credits = "LIMITED", random.choice([2.0, 3.0])
            else:
                ctype, credits = "ELECTIVE", random.choice([1.0, 2.0, 3.0])
            hours = int(credits * 16)
            assess = "EXAM" if ctype == "REQUIRED" else random.choice(["EXAM", "CHECK"])
            code = f"{ccode}{cid:04d}"
            course_rows.append(
                f"({cid}, {sql(code)}, {sql(name)}, {credits}, {hours}, {sql(ctype)}, {sql(assess)}, "
                f"{ci}, {sql('校级精品课程' if idx < 6 else '专业核心课程')})")
            course_meta[cid] = {"name": name, "college_id": ci, "credits": credits,
                                "type": ctype, "term": (idx // 4) + 1}
    w.insert("course", ["id", "code", "name", "credits", "hours", "course_type",
                        "assessment", "college_id", "description"], course_rows)

    name2id = {v["name"]: k for k, v in course_meta.items()}
    pre_rows = []
    for i, (course, pre, score) in enumerate(PREREQ):
        if course in name2id and pre in name2id:
            pre_rows.append(f"({i + 1}, {name2id[course]}, {name2id[pre]}, {score})")
    w.insert("course_prerequisite", ["id", "course_id", "pre_course_id", "min_score"], pre_rows)

    # ---------------- 培养计划 ----------------
    plan_rows, plan_by_major_term = [], {}
    pid = 0
    for mid_, ci, _ in major_desc:
        pool = [c for c, m in course_meta.items() if m["college_id"] == ci]
        for term in range(1, 9):
            picked = random.sample(pool, min(len(pool), random.randint(3, 5)))
            for c in picked:
                pid += 1
                rt = "COMPULSORY" if course_meta[c]["type"] == "REQUIRED" else "ELECTIVE"
                plan_rows.append(f"({pid}, {mid_}, 2024, {term}, {c}, {sql(rt)})")
                plan_by_major_term.setdefault((mid_, term), []).append(c)
    w.insert("training_plan", ["id", "major_id", "grade", "term_no", "course_id", "required_type"], plan_rows)

    # ---------------- 学分规则 ----------------
    credit_rows, crid = [], 0
    for mid_, ci, _ in major_desc:
        for term in range(1, 9):
            courses = plan_by_major_term.get((mid_, term), [])
            plan_credits = max(20.0, sum(course_meta[c]["credits"] for c in courses))
            crid += 1
            # 校规：一般不得高于 30.0 学分，下限 15.0
            credit_rows.append(f"({crid}, {mid_}, {term}, {plan_credits}, 15.0, 30.0)")
    w.insert("credit_rule", ["id", "major_id", "term_no", "plan_credits", "min_credits", "max_credits"],
             credit_rows)

    # ---------------- 学期与轮次（相对当前时间，保证演示时第一轮始终开放） ----------------
    now = dt.datetime.now()
    today = now.replace(hour=0, minute=0, second=0, microsecond=0)
    w.insert("semester", ["id", "year", "term", "name", "start_date", "total_weeks", "current_week", "locked"],
             [f"(1, 2026, 1, {sql('2026-2027 学年第一学期')}, {sql('2026-09-07')}, 18, 8, 0)"])

    r1s, r1e = today - dt.timedelta(days=1) + dt.timedelta(hours=9), today + dt.timedelta(days=7, hours=17)
    r2s, r2e = today + dt.timedelta(days=45, hours=9), today + dt.timedelta(days=50, hours=17)
    r3s, r3e = today + dt.timedelta(days=120, hours=9), today + dt.timedelta(days=125, hours=17)
    fmt = "%Y-%m-%d %H:%M:%S"
    w.insert("selection_round", ["id", "semester_id", "round_no", "name", "start_time", "end_time",
                                 "allow_drop", "status"], [
        f"(1, 1, 1, {sql('第一轮 · 海选')}, {sql(r1s.strftime(fmt))}, {sql(r1e.strftime(fmt))}, 1, 'OPEN')",
        f"(2, 1, 2, {sql('第二轮 · 补选')}, {sql(r2s.strftime(fmt))}, {sql(r2e.strftime(fmt))}, 1, 'PENDING')",
        f"(3, 1, 3, {sql('第三轮 · 退改选')}, {sql(r3s.strftime(fmt))}, {sql(r3e.strftime(fmt))}, 1, 'PENDING')",
    ])

    # ---------------- 教学班 ----------------
    tc_rows, ct_rows, class_meta = [], [], {}
    teacher_busy = {}
    all_courses = sorted(course_meta)
    used_course_class = set()
    tcid = 0
    attempts = 0
    while len(class_meta) < args.classes and attempts < args.classes * 30:
        attempts += 1
        c = random.choice(all_courses)
        meta = course_meta[c]
        class_no = f"{random.randint(1, 4):02d}"
        if (c, class_no) in used_course_class:
            continue
        teacher = random.choice(teachers_by_college[meta["college_id"]])
        busy = teacher_busy.get(teacher, 0)
        slots, mask = [], 0
        ok = True
        for _ in range(random.choice([1, 1, 1, 2])):
            for _try in range(12):
                day = random.randint(1, 5)
                start = random.choice([1, 3, 5, 6, 9, 10])
                end = start + random.choice([1, 1, 2])
                m = slot_mask(day, start, end)
                if (mask & m) == 0 and (busy & m) == 0:
                    slots.append((day, start, end))
                    mask |= m
                    break
            else:
                ok = False
        if not ok or not slots:
            continue
        used_course_class.add((c, class_no))
        teacher_busy[teacher] = busy | mask
        tcid += 1
        capacity = random.choice(CAPACITIES)
        campus = "军工路" if meta["college_id"] in (3, 4, 8) else "浦东"
        classroom = f"{random.choice(BUILDINGS)} {random.randint(1, 5)}{random.randint(0, 9):02d}"
        tc_rows.append(
            f"({tcid}, 1, {c}, {sql(class_no)}, {teacher}, {capacity}, 0, {mask}, "
            f"{sql(campus)}, {sql(classroom)}, 'OPEN')")
        for (day, start, end) in slots:
            ct_rows.append(f"({len(ct_rows) + 1}, {tcid}, {day}, {start}, {end}, "
                           f"{sql(random.choice(WEEK_TYPES))})")
        class_meta[tcid] = {"course_id": c, "capacity": capacity, "mask": mask,
                            "selected": 0, "credit": meta["credits"]}
    w.insert("teaching_class", ["id", "semester_id", "course_id", "class_no", "teacher_id", "capacity",
                                "selected_count", "time_bitmap", "campus", "classroom", "status"], tc_rows)
    w.insert("class_time", ["id", "teaching_class_id", "day_of_week", "start_section", "end_section",
                            "week_type"], ct_rows)

    classes_by_course = {}
    for k, v in class_meta.items():
        classes_by_course.setdefault(v["course_id"], []).append(k)

    # ---------------- 学生 ----------------
    student_rows, student_meta = [], []
    sid = 0
    per_major = max(1, args.students // len(major_desc))
    for mi, (mid_, ci, mname) in enumerate(major_desc):
        for k in range(per_major):
            sid += 1
            grade = 2023 + (k % 4)
            no = f"{grade}{sid:04d}"
            name = random.choice(SURNAMES) + random.choice(GIVEN)
            gender = "M" if random.random() < 0.55 else "F"
            class_name = f"{mname}{grade % 100}{k % 3 + 1:02d}班"
            student_rows.append(f"({sid}, {sql(no)}, {sql(name)}, {sql(gender)}, {grade}, {mid_}, {sql(class_name)})")
            student_meta.append({"id": sid, "no": no, "grade": grade, "major_id": mid_})
    w.insert("student", ["id", "student_no", "name", "gender", "grade", "major_id", "class_name"], student_rows)

    # 演示学生固定为 id=1、学号 20240001、2024 级、计算机科学与技术（major_id = 3）
    demo = student_meta[0]
    demo.update({"grade": 2024, "major_id": 3, "no": "20240001"})
    w.raw(f"UPDATE student SET student_no = '20240001', grade = 2024, major_id = 3, "
          f"class_name = '计算机科学与技术2401班' WHERE id = 1;")

    # ---------------- 账号 ----------------
    user_rows = [f"(1, {sql('admin')}, {sql(PWD_HASH)}, 'ADMIN', 1, 1)"]
    for s in student_meta:
        user_rows.append(f"({s['id'] + 1}, {sql(s['no'])}, {sql(PWD_HASH)}, 'STUDENT', {s['id']}, 1)")
    w.insert("sys_user", ["id", "username", "password", "role", "ref_id", "status"], user_rows)

    # ---------------- 历史成绩（前 200 名学生，供已修学分与先修校验） ----------------
    grade_rows, grid = [], 0
    for s in student_meta[:200]:
        courses = [c for (mid_, term), cs in plan_by_major_term.items()
                   if mid_ == s["major_id"] for c in cs if term <= 4]
        for c in random.sample(courses, min(len(courses), random.randint(8, 14))):
            grid += 1
            score = random.choice([random.randint(60, 95), random.randint(40, 59)])
            grade_rows.append(f"({grid}, {s['id']}, {c}, {score}, {random.randint(1, 4)}, {1 if score >= 60 else 0})")
    # 演示学生：已修 12 门且《程序设计基础(C语言)》未通过，用于演示 40004 先修拦截
    demo_courses = {course_meta[c]["name"]: c for c in range(1, len(course_meta) + 1)}
    demo_grades = [("高等数学A(1)", 88, 1, 1), ("高等数学A(2)", 81, 2, 1), ("线性代数", 90, 2, 1),
                   ("大学物理(1)", 76, 3, 1), ("程序设计基础(C语言)", 45, 1, 0),
                   ("面向对象程序设计(Java)", 84, 3, 1), ("数据库原理", 79, 4, 1),
                   ("计算机网络", 73, 4, 1), ("大学英语(1)", 86, 1, 1), ("大学英语(2)", 82, 2, 1),
                   ("体育(1)", 91, 1, 1), ("体育(2)", 89, 2, 1)]
    for name, score, term, passed in demo_grades:
        c = demo_courses.get(name)
        if c:
            grid += 1
            grade_rows.append(f"({grid}, 1, {c}, {score}, {term}, {passed})")
    w.insert("course_grade", ["id", "student_id", "course_id", "score", "term_no", "passed"], grade_rows)

    # ---------------- 选课记录 ----------------
    def pick_classes(student_id, major_id, term, want):
        """按培养计划为本学生挑选不冲突、有余量的教学班"""
        busy = 0
        chosen, picked_courses = [], set()
        pool = list(plan_by_major_term.get((major_id, term), []))
        if len(pool) < want + 2:
            extra = [c for c in all_courses
                     if course_meta[c]["college_id"] == major_college.get(major_id) and c not in pool]
            random.shuffle(extra)
            pool += extra[:max(0, want + 2 - len(pool))]
        if not pool:
            return chosen, busy
        order = pool[:]
        random.shuffle(order)
        for c in order:
            if len(chosen) >= want:
                break
            if c in picked_courses:
                continue
            cands = [k for k in classes_by_course.get(c, [])
                     if class_meta[k]["selected"] < class_meta[k]["capacity"]]
            if not cands:
                continue
            for k in random.sample(cands, min(len(cands), 3)):
                if (busy & class_meta[k]["mask"]) == 0:
                    class_meta[k]["selected"] += 1
                    busy |= class_meta[k]["mask"]
                    chosen.append(k)
                    picked_courses.add(c)
                    break
        return chosen, busy

    enroll_values, eid = [], 0
    for s in student_meta:
        # 演示学生多选一门，使其学分接近上限，便于现场演示 40005
        want = 7 if s["id"] == 1 else 6
        chosen, _ = pick_classes(s["id"], s["major_id"], 5, want)
        for k in chosen:
            eid += 1
            enroll_values.append(
                f"({eid}, {s['id']}, 1, {k}, {class_meta[k]['course_id']}, 1, "
                f"{class_meta[k]['credit']}, 'SELECTED', NOW(), NULL)")
    w.insert("enrollment", ["id", "student_id", "semester_id", "teaching_class_id", "course_id",
                            "round_id", "credits", "status", "created_at", "dropped_at"],
             enroll_values, batch=500)

    # 回写 selected_count
    updates = [f"UPDATE teaching_class SET selected_count = {v['selected']} WHERE id = {k};"
               for k, v in class_meta.items() if v["selected"] > 0]
    w.raw("\n".join(updates))
    w.raw("-- seed 完成")

    out = Path(args.output)
    w.dump(out)
    total = sum(v["selected"] for v in class_meta.values())
    print(f"[gen_seed] 学院 8 / 专业 {len(major_desc)} / 课程 {len(course_meta)} / "
          f"教学班 {len(class_meta)} / 学生 {len(student_meta)}")
    print(f"[gen_seed] 选课记录 {total} 条 -> {out} ({out.stat().st_size / 1024 / 1024:.2f} MB)")


if __name__ == "__main__":
    main()
