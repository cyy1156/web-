package com.usst.course.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.usst.course.common.BizException;
import com.usst.course.common.ErrorCode;
import com.usst.course.dto.Api;
import com.usst.course.dto.EnrolledVO;
import com.usst.course.dto.TimeVO;
import com.usst.course.entity.ClassTime;
import com.usst.course.entity.Course;
import com.usst.course.entity.CourseGrade;
import com.usst.course.entity.Enrollment;
import com.usst.course.entity.Semester;
import com.usst.course.entity.Student;
import com.usst.course.entity.TeachingClass;
import com.usst.course.entity.Teacher;
import com.usst.course.entity.TrainingPlan;
import com.usst.course.mapper.ClassTimeMapper;
import com.usst.course.mapper.CourseGradeMapper;
import com.usst.course.mapper.CourseMapper;
import com.usst.course.mapper.EnrollmentMapper;
import com.usst.course.mapper.StudentMapper;
import com.usst.course.mapper.TeacherMapper;
import com.usst.course.mapper.TeachingClassMapper;
import com.usst.course.mapper.TrainingPlanMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final EnrollmentMapper enrollmentMapper;
    private final TeachingClassMapper classMapper;
    private final CourseMapper courseMapper;
    private final TeacherMapper teacherMapper;
    private final ClassTimeMapper classTimeMapper;
    private final StudentMapper studentMapper;
    private final CourseGradeMapper gradeMapper;
    private final TrainingPlanMapper planMapper;
    private final SemesterService semesterService;
    private final EnrollmentService enrollmentService;

    public List<EnrolledVO> listEnrollments(Long studentId, Long semesterId) {
        Long semId = resolveSemester(semesterId);
        List<Enrollment> list = enrollmentMapper.selectList(Wrappers.<Enrollment>lambdaQuery()
                .eq(Enrollment::getStudentId, studentId)
                .eq(Enrollment::getSemesterId, semId)
                .eq(Enrollment::getStatus, "SELECTED"));
        return toEnrolled(list);
    }

    public Api.TimetableRes timetable(Long studentId, Long semesterId) {
        Long semId = resolveSemester(semesterId);
        List<Enrollment> list = enrollmentMapper.selectList(Wrappers.<Enrollment>lambdaQuery()
                .eq(Enrollment::getStudentId, studentId)
                .eq(Enrollment::getSemesterId, semId)
                .eq(Enrollment::getStatus, "SELECTED"));

        List<Api.TimetableItem> items = new ArrayList<>();
        for (Enrollment e : list) {
            TeachingClass tc = classMapper.selectById(e.getTeachingClassId());
            Course c = courseMapper.selectById(e.getCourseId());
            if (tc == null || c == null || "CANCELLED".equals(tc.getStatus())) {
                continue;
            }
            Teacher t = teacherMapper.selectById(tc.getTeacherId());
            items.add(new Api.TimetableItem(e.getId(), c.getId(), c.getName(),
                    t == null ? "" : t.getName(), tc.getClassroom(), c.getCourseType(),
                    (int) (c.getId() % 8), timeOfClass(tc.getId())));
        }
        Long busy = enrollmentMapper.selectBusyBitmap(studentId, semId);
        return new Api.TimetableRes(items, busy == null ? 0L : busy);
    }

    public Api.CreditStatus creditStatus(Long studentId, Long semesterId) {
        Long semId = resolveSemester(semesterId);
        Student s = studentMapper.selectById(studentId);
        Semester sem = semesterService.current();
        BigDecimal selected = nvl(enrollmentMapper.sumSelectedCredits(studentId, semId));
        BigDecimal passed = nvl(gradeMapper.sumPassedCredits(studentId));
        return new Api.CreditStatus(s == null ? "" : s.getName(),
                s == null ? "" : s.getStudentNo(),
                s == null ? "" : studentMapper.selectMajorName(s.getMajorId()),
                s == null ? "" : s.getClassName(),
                selected, passed,
                enrollmentService.planCredits(s, sem),
                new BigDecimal("15.0"),
                enrollmentService.maxCredits(s, sem));
    }

    public Api.PlanRes plan(Long studentId, Long semesterId) {
        Long semId = resolveSemester(semesterId);
        Student s = studentMapper.selectById(studentId);
        if (s == null) {
            throw BizException.of(ErrorCode.UNAUTHORIZED);
        }
        List<TrainingPlan> plans = planMapper.selectList(Wrappers.<TrainingPlan>lambdaQuery()
                .eq(TrainingPlan::getMajorId, s.getMajorId())
                .eq(TrainingPlan::getGrade, s.getGrade())
                .orderByAsc(TrainingPlan::getTermNo));
        Map<Long, Integer> bestScores = gradeMapper.selectList(Wrappers.<CourseGrade>lambdaQuery()
                        .eq(CourseGrade::getStudentId, studentId)).stream()
                .collect(Collectors.toMap(CourseGrade::getCourseId, CourseGrade::getScore, Math::max));
        List<Enrollment> current = enrollmentMapper.selectList(Wrappers.<Enrollment>lambdaQuery()
                .eq(Enrollment::getStudentId, studentId)
                .eq(Enrollment::getSemesterId, semId)
                .eq(Enrollment::getStatus, "SELECTED"));
        Map<Long, Long> currentCourses = current.stream()
                .collect(Collectors.toMap(Enrollment::getCourseId, Enrollment::getId, (a, b) -> a));

        Map<Integer, List<Api.PlanCourse>> grouped = new LinkedHashMap<>();
        Map<Long, Course> courseCache = new HashMap<>();
        BigDecimal passedCredits = BigDecimal.ZERO;
        BigDecimal requiredCredits = BigDecimal.ZERO;

        for (TrainingPlan p : plans) {
            Course c = courseCache.computeIfAbsent(p.getCourseId(), courseMapper::selectById);
            if (c == null) {
                continue;
            }
            requiredCredits = requiredCredits.add(c.getCredits());
            Integer score = bestScores.get(c.getId());
            String status;
            if (score != null && score >= 60) {
                status = "已完成";
                passedCredits = passedCredits.add(c.getCredits());
            } else if (currentCourses.containsKey(c.getId())) {
                status = "在修";
            } else if (score != null) {
                status = "未通过";
            } else {
                status = "未修";
            }
            grouped.computeIfAbsent(p.getTermNo(), k -> new ArrayList<>())
                    .add(new Api.PlanCourse(c.getId(), c.getName(), c.getCredits(),
                            "COMPULSORY".equals(p.getRequiredType()) ? "必修" : "选修", status, score));
        }

        List<Api.PlanTerm> terms = grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(en -> new Api.PlanTerm(en.getKey(), en.getValue()))
                .toList();
        BigDecimal earning = nvl(enrollmentMapper.sumSelectedCredits(studentId, semId));
        return new Api.PlanRes(terms, passedCredits, earning, requiredCredits);
    }

    private List<EnrolledVO> toEnrolled(List<Enrollment> list) {
        if (list.isEmpty()) {
            return List.of();
        }
        Map<Long, TeachingClass> classes = classMapper
                .selectBatchIds(list.stream().map(Enrollment::getTeachingClassId).toList()).stream()
                .collect(Collectors.toMap(TeachingClass::getId, t -> t));
        Map<Long, Course> courses = courseMapper
                .selectBatchIds(list.stream().map(Enrollment::getCourseId).toList()).stream()
                .collect(Collectors.toMap(Course::getId, c -> c));
        Map<Long, Teacher> teachers = teacherMapper
                .selectBatchIds(classes.values().stream().map(TeachingClass::getTeacherId).toList()).stream()
                .collect(Collectors.toMap(Teacher::getId, t -> t));
        Map<Long, List<TimeVO>> times = classTimeMapper
                .selectList(Wrappers.<ClassTime>lambdaQuery()
                        .in(ClassTime::getTeachingClassId, classes.keySet())).stream()
                .collect(Collectors.groupingBy(ClassTime::getTeachingClassId,
                        Collectors.mapping(t -> new TimeVO(t.getDayOfWeek(), t.getStartSection(),
                                t.getEndSection(), t.getWeekType()), Collectors.toList())));

        long[] masks = new long[list.size()];
        for (int i = 0; i < list.size(); i++) {
            TeachingClass tc = classes.get(list.get(i).getTeachingClassId());
            masks[i] = tc == null || tc.getTimeBitmap() == null ? 0L : tc.getTimeBitmap();
        }

        List<EnrolledVO> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            Enrollment e = list.get(i);
            TeachingClass tc = classes.get(e.getTeachingClassId());
            Course c = courses.get(e.getCourseId());
            if (tc == null || c == null) {
                continue;
            }
            boolean conflicted = false;
            for (int j = 0; j < masks.length; j++) {
                if (i != j && masks[i] != 0 && masks[j] != 0 && (masks[i] & masks[j]) != 0) {
                    conflicted = true;
                    break;
                }
            }
            EnrolledVO vo = new EnrolledVO();
            vo.setId(e.getId());
            vo.setTeachingClassId(tc.getId());
            vo.setCourseId(c.getId());
            vo.setCourseName(c.getName());
            vo.setCredits(c.getCredits());
            vo.setCourseType(c.getCourseType());
            vo.setCourseTypeText(CourseService.typeText(c.getCourseType()));
            Teacher t = teachers.get(tc.getTeacherId());
            vo.setTeacherName(t == null ? "" : t.getName());
            List<TimeVO> ts = new ArrayList<>(times.getOrDefault(tc.getId(), List.of()));
            ts.sort(Comparator.comparing(TimeVO::getDayOfWeek).thenComparing(TimeVO::getStartSection));
            vo.setTimeText(ts.isEmpty() ? "时间待定" : ts.stream().map(TimeVO::text).collect(Collectors.joining(" / ")));
            vo.setCampus(tc.getCampus());
            vo.setClassroom(tc.getClassroom());
            vo.setConflicted(conflicted);
            result.add(vo);
        }
        return result;
    }

    private List<TimeVO> timeOfClass(Long classId) {
        return classTimeMapper.selectList(Wrappers.<ClassTime>lambdaQuery()
                        .eq(ClassTime::getTeachingClassId, classId)).stream()
                .map(t -> new TimeVO(t.getDayOfWeek(), t.getStartSection(), t.getEndSection(), t.getWeekType()))
                .sorted(Comparator.comparing(TimeVO::getDayOfWeek).thenComparing(TimeVO::getStartSection))
                .toList();
    }

    private Long resolveSemester(Long semesterId) {
        return semesterId != null ? semesterId : semesterService.current().getId();
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
