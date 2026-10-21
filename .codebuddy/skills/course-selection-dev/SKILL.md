---
name: course-selection-dev
description: 开发与迭代"高校选课系统"（对标上海理工大学教务系统，Spring Boot 3 + Vue 3 单体应用）的工程任务。当任务涉及选课轮次配置、教学班容量原子扣减、学分/时间冲突/先修课校验、退改选、课表渲染、管理端选课统计，或需要修改本项目数据库表、后端接口、前端页面、生成脱敏种子数据时使用。
---

# 高校选课系统开发

本项目是课程设计级的教务选课系统，对标上海理工大学《全日制本科生选课办法》。核心难点集中在"抢课"这一条链路的正确性，界面部分按模板快速铺开。

## 技术栈（不得擅自更换）

- 后端：Spring Boot 3.3 / JDK 21（release）/ MyBatis-Plus 3.5 / MySQL 8 / JWT 拦截器 / Knife4j
- 前端：Vue 3 + TypeScript + Vite 5 / TDesign Vue Next / Tailwind CSS 3.4 / Pinia / vue-echarts
- 明确不引入：Redis、消息队列、微服务、Spring Security 全家桶。需要引入时先向使用者说明理由与工期代价。

## 开工前必读

按顺序阅读，不要跳过：

1. `references/business-rules.md` —— 三轮选课窗口、学分区间、退改选、名单锁定、校验顺序
2. `references/data-model.md` —— 12 张表结构、索引、必须守住的不变量
3. `references/api-spec.md` —— 接口契约与错误码，禁止自造字段名
4. `references/frontend-spec.md` —— 页面布局、交互约定、设计 token

读完再读真实代码确认现状，禁止凭记忆假设字段。

## 硬约束（任何改动不得破坏）

- `0 <= teaching_class.selected_count <= capacity`
- 容量扣减只允许这一条语句，禁止先查后改：
  `UPDATE teaching_class SET selected_count = selected_count + 1 WHERE id = ? AND status = 'OPEN' AND selected_count < capacity`
- 选课校验顺序固定：轮次窗口 → 重复选课 → 容量 → 学分 → 时间冲突 → 先修 → 跨年级
- 写接口必须携带 `Idempotency-Key`，重复请求回放首次结果，不得重复扣减
- 时间冲突只允许位图运算（`time_bitmap`），禁止遍历 `class_time` 逐条比对
- 退课只改 `status = 'DROPPED'`，禁止物理删除 `enrollment` 记录
- 学分上限含重修学分，下限 15.0，上限默认 30.0
- 第三轮结束后 `semester.locked = 1`，所有写接口一律返回 40010
- 仓库内禁止出现真实学生姓名、学号、手机号；页面与 README 必须标注"教学演示项目，非官方系统"

## 标准工作流

1. 判定影响面：表结构 / 后端接口 / 前端页面，三者中改了哪几个
2. 先改 `db/schema.sql`，再改实体与 Mapper，最后改 Service
3. 业务编排只写在 Service 层，Controller 不出现 `if` 分支；异常由 `GlobalExceptionHandler` 统一转换
4. 前端改动先对齐 `references/frontend-spec.md` 的布局与交互约定
5. 校验类改动必须补单测；容量类改动必须跑一次并发脚本验证不超卖
6. 规则变化同步更新 `references/` 下的文档，保持文档与代码一致

## 目录约定

```
backend/src/main/java/com/usst/course/
├── common/      R、ErrorCode、BizException、GlobalExceptionHandler、PageResult
├── security/    JwtUtil、JwtInterceptor、UserContext、@RequireRole
├── module/      auth / course / enrollment / student / admin
└── resources/mapper/*.xml
frontend/src/
├── api/         按模块拆分的请求函数，页面不直接写 axios
├── components/  布局、课表网格、学分环、课程卡
├── stores/      auth / semester / selection
└── views/       login、selection、timetable、plan、admin
```

## 常用命令

```bash
# 初始化数据库（本地 MySQL 8，root 无密码时省略 -p）
mysql -uroot < db/schema.sql
mysql -uroot course_selection < db/seed.sql

# 生成种子数据
python scripts/gen_seed.py --students 2000 --classes 400

# 后端（:8080，接口文档 /doc.html）
cd backend && mvn spring-boot:run

# 前端（:5173，/api 代理到 8080）
cd frontend && npm install && npm run dev
```

## 演示账号

- 学生：`2022001` / `123456`
- 管理员：`admin` / `123456`

## 验收必须能复现的 5 个场景

1. 两人同时抢最后一个名额：一个成功，一个返回 40002
2. 学分超过上限被拦截，提示具体超出多少
3. 与已选课程时间冲突被拦截，提示冲突课程名与节次
4. 先修课未通过被拦截，提示缺少哪门先修课
5. 轮次关闭后学生端选课按钮全部置灰，接口返回 40001
