# 前端规范

## 一、路由

| 路径 | 页面 | 角色 | 说明 |
|---|---|---|---|
| `/login` | 登录 | 匿名 | 左右分屏 |
| `/selection` | 选课大厅 | STUDENT | 核心页，三栏 |
| `/timetable` | 我的课表 | STUDENT | CSS Grid |
| `/plan` | 培养计划 | STUDENT | 按学期折叠 |
| `/admin/dashboard` | 数据看板 | ADMIN | 4 卡 + 图表 |
| `/admin/rounds` | 轮次配置 | ADMIN | 三卡横排 |
| `/admin/classes` | 教学班管理 | ADMIN | 密集表格 + 抽屉表单 |

路由守卫：无 token 跳 `/login`；角色不匹配跳自身首页（学生 `/selection`，管理员 `/admin/dashboard`）。

## 二、设计 token

```css
--color-primary: #2563EB;
--color-primary-hover: #1D4ED8;
--color-primary-light: #EFF6FF;
--color-bg: #F5F7FA;
--color-surface: #FFFFFF;
--color-text: #111827;
--color-text-sub: #6B7280;
--color-border: #E5E7EB;
--color-success: #16A34A;
--color-warning: #F59E0B;
--color-danger: #DC2626;
--radius-card: 12px;
--radius-btn: 8px;
--shadow-card: 0 1px 2px rgba(16,24,40,.06), 0 1px 3px rgba(16,24,40,.1);
--shadow-hover: 0 8px 24px rgba(37,99,235,.12);
font-family: "PingFang SC", "HarmonyOS Sans SC", "Microsoft YaHei", system-ui, sans-serif;
```

字号：页面标题 28/600，区块标题 18/600，正文 14/400，辅助 12/400。

## 三、布局

- 左侧导航 220px（折叠 64px），深色底 `#0F1B33`，选中项左侧 3px 主色条
- 顶栏 60px 白底，含学期选择、轮次标签、倒计时、学分环、用户菜单
- 内容区 `padding: 20px`，最大宽度不限制，表格横向撑满
- 选课大厅为特殊布局：`height: calc(100vh - 60px)`，三栏各自滚动

## 四、选课大厅交互约定

| 交互 | 规则 |
|---|---|
| 余量轮询 | 3 秒一次，只请求当前可见（含预加载下一页）的教学班 id，`document.hidden` 时暂停，连续失败 3 次退避到 10 秒 |
| 余量颜色 | 余量/容量 > 20% 绿色、5%–20% 橙色、0 灰色并禁用按钮 |
| 冲突预判 | 加入选课车（前端暂存）前用本地 `busyBitmap` 位运算比对，冲突项标红并显示冲突课程名 |
| 防重复提交 | 按钮 `loading` + `crypto.randomUUID()` 作为 `Idempotency-Key`，成功后清除，失败保留 3 秒后释放 |
| 学分实时 | 右侧环形进度实时计算，超上限变红并禁用"确认选课" |
| 错误提示 | 统一 Toast 顶部居中，直接展示后端中文 `message`，禁止展示数字错误码 |
| 倒计时 | 以 `remainingSeconds` 为初值，前端 `setInterval` 递减；归零时置灰按钮并刷新一次学期信息 |
| 已选卡片 | 降饱和 + 右上角"已选"角标，点击可退课 |
| 滚动加载 | 距底部 200px 触发下一页，最多保留 300 条 DOM |

## 五、课表网格

```css
grid-template-columns: 56px repeat(5, 1fr);
grid-template-rows: 40px repeat(12, 56px);
```

- 课程块绝对定位在网格容器内：`top = (startSection-1)*56`，`height = (endSection-startSection+1)*56`
- 颜色由 `courseId % 8` 映射到 8 组低饱和色板，同课程同色
- 冲突时两块各占 50% 宽度，`border: 1.5px dashed var(--color-danger)`
- 顶部周次切换 1–18 周，`week_type` 为 ODD/EVEN 的课程按当前周奇数偶数显隐
- 空单元格浅灰虚线，hover 显示"未排课"

## 六、组件清单

| 组件 | 职责 |
|---|---|
| `AppLayout.vue` | 侧边栏 + 顶栏 + `<router-view>`，按角色渲染菜单 |
| `CourseCard.vue` | 课程卡：名称、性质标签、学分、教师、时间、余量条、选课按钮 |
| `CreditRing.vue` | 学分环形进度，`selectedCredits / maxCredits`，超限变红 |
| `TimetableGrid.vue` | 课表网格，接收 items 与 week |
| `EmptyState.vue` | 空状态插画 + 文案 |
| `StatCard.vue` | 看板指标卡，含迷你趋势线 |

## 七、状态

| store | 字段 |
|---|---|
| `useAuthStore` | token、role、profile，持久化到 localStorage |
| `useSemesterStore` | semester、round、serverOffset、locked |
| `useSelectionStore` | enrolledList、busyBitmap、totalCredits、cart |

## 八、禁止事项

- 页面内直接调用 axios，必须走 `src/api/*`
- 展示后端数字错误码
- 使用 `div` 模拟按钮/输入框
- 引入新的 UI 库或动画库
- 单个 `.vue` 文件超过 300 行，超出则拆子组件
