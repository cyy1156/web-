package com.usst.course.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.usst.course.common.BizException;
import com.usst.course.common.ErrorCode;
import com.usst.course.dto.Api;
import com.usst.course.entity.Course;
import com.usst.course.entity.CoursePrerequisite;
import com.usst.course.entity.CreditRule;
import com.usst.course.entity.Enrollment;
import com.usst.course.entity.EnrollmentLog;
import com.usst.course.entity.Semester;
import com.usst.course.entity.SelectionRound;
import com.usst.course.entity.Student;
import com.usst.course.entity.TeachingClass;
import com.usst.course.entity.TrainingPlan;
import com.usst.course.mapper.CourseGradeMapper;
import com.usst.course.mapper.CourseMapper;
import com.usst.course.mapper.CoursePrerequisiteMapper;
import com.usst.course.mapper.CreditRuleMapper;
import com.usst.course.mapper.EnrollmentLogMapper;
import com.usst.course.mapper.EnrollmentMapper;
import com.usst.course.mapper.StudentMapper;
import com.usst.course.mapper.TeachingClassMapper;
import com.usst.course.mapper.TrainingPlanMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 选课核心。
 *
 * <p>校验顺序固定：轮次窗口 → 重复选课 → 容量 → 学分 → 时间冲突 → 先修 → 跨年级。
 * 容量扣减只有一条条件 UPDATE，影响行数为 0 即视为已满。
 *
 * <p>事务边界用 {@link TransactionTemplate} 显式控制：失败日志必须在事务回滚、
 * 连接归还连接池之后再写，否则高并发下会因为"持锁等连接"造成连接池死锁。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private static final BigDecimal DEFAULT_MAX = new BigDecimal("30.0");
    private static final BigDecimal DEFAULT_PLAN = new BigDecimal("20.0");

    private final EnrollmentMapper enrollmentMapper;
    private final EnrollmentLogMapper logMapper;
    private final EnrollmentLogService logService;
    private final TeachingClassMapper classMapper;
    private final CourseMapper courseMapper;
    private final StudentMapper studentMapper;
    private final SemesterService semesterService;
    private final CreditRuleMapper creditRuleMapper;
    private final CourseGradeMapper gradeMapper;
    private final CoursePrerequisiteMapper prerequisiteMapper;
    private final TrainingPlanMapper planMapper;
    private final TransactionTemplate transactionTemplate;

    public Api.EnrollRes enroll(Long studentId, Long classId, String idemKey) {
        long begin = System.currentTimeMillis();

        EnrollmentLog existed = logService.findByIdemKey(idemKey);
        if (existed != null) {
            if (existed.getResultCode() == 0) {
                return replay(studentId, classId);
            }
            throw new BizException(existed.getResultCode(), existed.getMessage());
        }

        try {
            Api.EnrollRes res = transactionTemplate.execute(status -> doEnroll(studentId, classId, idemKey, begin));
            return res == null ? new Api.EnrollRes(null, BigDecimal.ZERO) : res;
        } catch (BizException ex) {
            logService.recordFailure(studentId, classId, "ENROLL", ex.getCode(), ex.getMessage(),
                    idemKey, System.currentTimeMillis() - begin);
            throw ex;
        }
    }

    public void drop(Long studentId, Long enrollmentId) {
        long begin = System.currentTimeMillis();
        try {
            transactionTemplate.executeWithoutResult(status -> doDrop(studentId, enrollmentId, begin));
        } catch (BizException ex) {
            logService.recordFailure(studentId, null, "DROP", ex.getCode(), ex.getMessage(),
                    null, System.currentTimeMillis() - begin);
            throw ex;
        }
    }

    private Api.EnrollRes doEnroll(Long studentId, Long classId, String idemKey, long begin) {
        Semester semester = semesterService.current();
        if (semester.getLocked() != null && semester.getLocked() == 1) {
            throw BizException.of(ErrorCode.LOCKED);
        }
        SelectionRound round = semesterService.currentRound(semester.getId());
        if (round == null) {
            throw new BizException(ErrorCode.NOT_IN_WINDOW.getCode(), "当前不在选课时间内，请关注教务处通知");
        }

        TeachingClass tc = classMapper.selectById(classId);
        if (tc == null || !"OPEN".equals(tc.getStatus())) {
            throw new BizException(ErrorCode.CLASS_FULL.getCode(), "该教学班已停开");
        }
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw BizException.of(ErrorCode.UNAUTHORIZED);
        }

        // R2 重复选课
        if (enrollmentMapper.countSameCourse(studentId, semester.getId(), tc.getCourseId()) > 0) {
            throw BizException.of(ErrorCode.DUPLICATE, "你已选过该课程，不能重复选择");
        }

        // R3 容量：原子扣减，影响行数为 0 即已满
        if (classMapper.increaseSelected(classId) == 0) {
            throw BizException.of(ErrorCode.CLASS_FULL);
        }

        Course course = courseMapper.selectById(tc.getCourseId());
        if (course == null) {
            throw new BizException(ErrorCode.SERVER_ERROR.getCode(), "课程数据缺失");
        }

        // R4 学分
        BigDecimal selected = nvl(enrollmentMapper.sumSelectedCredits(studentId, semester.getId()));
        BigDecimal max = maxCredits(student, semester);
        if (selected.add(course.getCredits()).compareTo(max) > 0) {
            throw new BizException(ErrorCode.CREDIT_LIMIT.getCode(),
                    "超出学期学分上限（已选 " + trim(selected) + " / " + trim(max)
                            + " 学分，本课程 " + trim(course.getCredits()) + " 学分）");
        }

        // R5 时间冲突（位图运算，O(1)）
        Long busy = enrollmentMapper.selectBusyBitmap(studentId, semester.getId());
        long mask = tc.getTimeBitmap() == null ? 0L : tc.getTimeBitmap();
        if (busy != null && mask != 0 && (busy & mask) != 0) {
            throw new BizException(ErrorCode.TIME_CONFLICT.getCode(),
                    "与《" + conflictCourseName(studentId, semester.getId(), mask) + "》上课时间冲突");
        }

        // R6 先修
        checkPrerequisite(studentId, course);

        // R7 跨年级
        checkCrossGrade(student, course, semester);

        Enrollment e = new Enrollment();
        e.setStudentId(studentId);
        e.setSemesterId(semester.getId());
        e.setTeachingClassId(classId);
        e.setCourseId(tc.getCourseId());
        e.setRoundId(round.getId());
        e.setCredits(course.getCredits());
        e.setStatus("SELECTED");
        e.setCreatedAt(LocalDateTime.now());
        enrollmentMapper.insert(e);

        BigDecimal total = selected.add(course.getCredits());

        EnrollmentLog okLog = new EnrollmentLog();
        okLog.setStudentId(studentId);
        okLog.setTeachingClassId(classId);
        okLog.setAction("ENROLL");
        okLog.setResultCode(0);
        okLog.setMessage("选课成功");
        okLog.setIdempotencyKey(idemKey);
        okLog.setCostMs((int) (System.currentTimeMillis() - begin));
        okLog.setCreatedAt(LocalDateTime.now());
        logMapper.insert(okLog);

        return new Api.EnrollRes(e.getId(), total);
    }

    private void doDrop(Long studentId, Long enrollmentId, long begin) {
        Semester semester = semesterService.current();
        if (semester.getLocked() != null && semester.getLocked() == 1) {
            throw BizException.of(ErrorCode.LOCKED);
        }
        SelectionRound round = semesterService.currentRound(semester.getId());
        if (round == null) {
            throw new BizException(ErrorCode.NOT_IN_WINDOW.getCode(), "当前不在选课时间内，无法退课");
        }
        if (round.getAllowDrop() != null && round.getAllowDrop() == 0) {
            throw new BizException(ErrorCode.NOT_DROPPABLE.getCode(), "本轮选课不允许退课");
        }

        Enrollment e = enrollmentMapper.selectById(enrollmentId);
        if (e == null || !studentId.equals(e.getStudentId())) {
            throw new BizException(ErrorCode.PARAM_INVALID.getCode(), "选课记录不存在");
        }
        if (!"SELECTED".equals(e.getStatus())) {
            throw new BizException(ErrorCode.DUPLICATE.getCode(), "该课程已退选");
        }
        Course course = courseMapper.selectById(e.getCourseId());
        if (course != null && "REQUIRED".equals(course.getCourseType())) {
            throw new BizException(ErrorCode.NOT_DROPPABLE.getCode(), "必修课不可退选");
        }

        e.setStatus("DROPPED");
        e.setDroppedAt(LocalDateTime.now());
        enrollmentMapper.updateById(e);
        classMapper.decreaseSelected(e.getTeachingClassId());

        EnrollmentLog dropLog = new EnrollmentLog();
        dropLog.setStudentId(studentId);
        dropLog.setTeachingClassId(e.getTeachingClassId());
        dropLog.setAction("DROP");
        dropLog.setResultCode(0);
        dropLog.setMessage("退课成功");
        dropLog.setCostMs((int) (System.currentTimeMillis() - begin));
        dropLog.setCreatedAt(LocalDateTime.now());
        logMapper.insert(dropLog);
    }

    private Api.EnrollRes replay(Long studentId, Long classId) {
        Enrollment e = enrollmentMapper.selectOne(Wrappers.<Enrollment>lambdaQuery()
                .eq(Enrollment::getStudentId, studentId)
                .eq(Enrollment::getTeachingClassId, classId)
                .eq(Enrollment::getStatus, "SELECTED").last("LIMIT 1"));
        Semester semester = semesterService.current();
        BigDecimal total = nvl(enrollmentMapper.sumSelectedCredits(studentId, semester.getId()));
        return new Api.EnrollRes(e == null ? null : e.getId(), total);
    }

    private void checkPrerequisite(Long studentId, Course course) {
        List<CoursePrerequisite> pres = prerequisiteMapper.selectList(
                Wrappers.<CoursePrerequisite>lambdaQuery().eq(CoursePrerequisite::getCourseId, course.getId()));
        for (CoursePrerequisite p : pres) {
            Integer score = gradeMapper.bestScore(studentId, p.getPreCourseId());
            if (score == null || score < p.getMinScore()) {
                Course pre = courseMapper.selectById(p.getPreCourseId());
                String preName = pre == null ? "先修课程" : pre.getName();
                throw new BizException(ErrorCode.PREREQUISITE.getCode(),
                        "需先修《" + preName + "》且成绩不低于 " + p.getMinScore() + " 分");
            }
        }
    }

    private void checkCrossGrade(Student student, Course course, Semester semester) {
        int term = currentTerm(student, semester);
        TrainingPlan plan = planMapper.selectOne(Wrappers.<TrainingPlan>lambdaQuery()
                .eq(TrainingPlan::getMajorId, student.getMajorId())
                .eq(TrainingPlan::getCourseId, course.getId())
                .orderByAsc(TrainingPlan::getTermNo).last("LIMIT 1"));
        if (plan != null && plan.getTermNo() != null && plan.getTermNo() > term) {
            throw new BizException(ErrorCode.CROSS_GRADE.getCode(),
                    "该课程为高年级课程（建议第 " + plan.getTermNo() + " 学期修读），低年级不可跨选");
        }
    }

    public int currentTerm(Student student, Semester semester) {
        int grade = student.getGrade() == null ? semester.getYear() : student.getGrade();
        int term = (semester.getYear() - grade) * 2 + semester.getTerm();
        return Math.min(8, Math.max(1, term));
    }

    public BigDecimal maxCredits(Student student, Semester semester) {
        CreditRule rule = creditRuleMapper.selectOne(Wrappers.<CreditRule>lambdaQuery()
                .eq(CreditRule::getMajorId, student.getMajorId())
                .eq(CreditRule::getTermNo, currentTerm(student, semester)).last("LIMIT 1"));
        return rule == null || rule.getMaxCredits() == null ? DEFAULT_MAX : rule.getMaxCredits();
    }

    public BigDecimal planCredits(Student student, Semester semester) {
        CreditRule rule = creditRuleMapper.selectOne(Wrappers.<CreditRule>lambdaQuery()
                .eq(CreditRule::getMajorId, student.getMajorId())
                .eq(CreditRule::getTermNo, currentTerm(student, semester)).last("LIMIT 1"));
        return rule == null || rule.getPlanCredits() == null ? DEFAULT_PLAN : rule.getPlanCredits();
    }

    private String conflictCourseName(Long studentId, Long semesterId, long targetMask) {
        List<Enrollment> list = enrollmentMapper.selectList(Wrappers.<Enrollment>lambdaQuery()
                .eq(Enrollment::getStudentId, studentId)
                .eq(Enrollment::getSemesterId, semesterId)
                .eq(Enrollment::getStatus, "SELECTED"));
        for (Enrollment e : list) {
            TeachingClass tc = classMapper.selectById(e.getTeachingClassId());
            if (tc != null && tc.getTimeBitmap() != null && (tc.getTimeBitmap() & targetMask) != 0) {
                Course c = courseMapper.selectById(e.getCourseId());
                return c == null ? "已选课程" : c.getName();
            }
        }
        return "已选课程";
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static String trim(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }
}
