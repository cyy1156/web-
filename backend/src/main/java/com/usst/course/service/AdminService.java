package com.usst.course.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.usst.course.common.BizException;
import com.usst.course.common.ErrorCode;
import com.usst.course.dto.Api;
import com.usst.course.dto.StatsVO;
import com.usst.course.entity.ClassTime;
import com.usst.course.entity.Course;
import com.usst.course.entity.Enrollment;
import com.usst.course.entity.Semester;
import com.usst.course.entity.SelectionRound;
import com.usst.course.entity.Student;
import com.usst.course.entity.TeachingClass;
import com.usst.course.mapper.ClassTimeMapper;
import com.usst.course.mapper.CourseMapper;
import com.usst.course.mapper.EnrollmentMapper;
import com.usst.course.mapper.SelectionRoundMapper;
import com.usst.course.mapper.StatsMapper;
import com.usst.course.mapper.StudentMapper;
import com.usst.course.mapper.TeachingClassMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SelectionRoundMapper roundMapper;
    private final TeachingClassMapper classMapper;
    private final ClassTimeMapper classTimeMapper;
    private final CourseMapper courseMapper;
    private final StudentMapper studentMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final StatsMapper statsMapper;
    private final SemesterService semesterService;

    public List<Api.RoundInfo> rounds(Long semesterId) {
        Long semId = semesterId != null ? semesterId : semesterService.current().getId();
        return roundMapper.selectList(Wrappers.<SelectionRound>lambdaQuery()
                        .eq(SelectionRound::getSemesterId, semId)
                        .orderByAsc(SelectionRound::getRoundNo)).stream()
                .map(semesterService::toRoundInfo).toList();
    }

    public void updateRound(Long id, Api.RoundUpdateReq req) {
        SelectionRound round = roundMapper.selectById(id);
        if (round == null) {
            throw new BizException(ErrorCode.PARAM_INVALID.getCode(), "轮次不存在");
        }
        if (req.status() != null) {
            round.setStatus(req.status());
        }
        if (req.startTime() != null) {
            round.setStartTime(req.startTime());
        }
        if (req.endTime() != null) {
            round.setEndTime(req.endTime());
        }
        if (req.allowDrop() != null) {
            round.setAllowDrop(req.allowDrop());
        }
        if (round.getStartTime() != null && round.getEndTime() != null
                && round.getStartTime().isAfter(round.getEndTime())) {
            throw new BizException(ErrorCode.PARAM_INVALID.getCode(), "开始时间不能晚于结束时间");
        }
        roundMapper.updateById(round);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createClass(Api.ClassSaveReq req) {
        validate(req);
        Semester sem = semesterService.current();
        TeachingClass tc = new TeachingClass();
        tc.setSemesterId(sem.getId());
        applyRequest(tc, req);
        classMapper.insert(tc);
        saveTimes(tc.getId(), req);
        return tc.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateClass(Long id, Api.ClassSaveReq req) {
        TeachingClass tc = classMapper.selectById(id);
        if (tc == null) {
            throw new BizException(ErrorCode.PARAM_INVALID.getCode(), "教学班不存在");
        }
        if (req.capacity() != null && req.capacity() < tc.getSelectedCount()) {
            throw new BizException(ErrorCode.PARAM_INVALID.getCode(),
                    "容量不能小于已选人数（" + tc.getSelectedCount() + "）");
        }
        applyRequest(tc, req);
        classMapper.updateById(tc);
        if (req.timeSlots() != null && !req.timeSlots().isEmpty()) {
            classTimeMapper.delete(Wrappers.<ClassTime>lambdaQuery().eq(ClassTime::getTeachingClassId, id));
            saveTimes(id, req);
        }
    }

    public void cancelClass(Long id) {
        TeachingClass tc = classMapper.selectById(id);
        if (tc == null) {
            throw new BizException(ErrorCode.PARAM_INVALID.getCode(), "教学班不存在");
        }
        tc.setStatus("CANCELLED");
        classMapper.updateById(tc);
    }

    public StatsVO stats(Long semesterId) {
        Semester sem = semesterService.current();
        Long semId = semesterId != null ? semesterId : sem.getId();
        StatsVO vo = new StatsVO();
        vo.setTotalEnrollments(enrollmentMapper.countBySemester(semId));
        vo.setTotalStudents(enrollmentMapper.countStudents(semId));
        Double avg = statsMapper.avgCourses(semId);
        vo.setAvgCourses(avg == null ? 0 : BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP).doubleValue());
        vo.setFullClasses(classMapper.countFullClasses(semId));
        vo.setOpenClasses(Math.toIntExact(classMapper.selectCount(Wrappers.<TeachingClass>lambdaQuery()
                .eq(TeachingClass::getSemesterId, semId).eq(TeachingClass::getStatus, "OPEN"))));
        SelectionRound round = roundMapper.selectCurrentOpen(semId);
        vo.setActiveRoundName(round == null ? "未开放" : round.getName());

        List<Map<String, Object>> rates = statsMapper.collegeRates(semId);
        rates.forEach(m -> m.put("rate", rate(m.get("enrolled"), m.get("capacity"))));
        vo.setCollegeRates(rates);

        List<Map<String, Object>> hot = statsMapper.hotCourses(semId);
        hot.forEach(m -> m.put("rate", rate(m.get("selected"), m.get("capacity"))));
        vo.setHotCourses(hot);
        return vo;
    }

    /** 导出教学班名单（CSV，Excel 可直接打开） */
    public byte[] exportClass(Long classId) {
        TeachingClass tc = classMapper.selectById(classId);
        if (tc == null) {
            throw new BizException(ErrorCode.PARAM_INVALID.getCode(), "教学班不存在");
        }
        Course course = courseMapper.selectById(tc.getCourseId());
        List<Enrollment> list = enrollmentMapper.selectList(Wrappers.<Enrollment>lambdaQuery()
                .eq(Enrollment::getTeachingClassId, classId)
                .eq(Enrollment::getStatus, "SELECTED")
                .orderByAsc(Enrollment::getId));

        StringBuilder sb = new StringBuilder("\uFEFF");
        sb.append("学号,姓名,专业,行政班,课程,学分,选课时间\n");
        Map<Long, Student> cache = new HashMap<>();
        for (Enrollment e : list) {
            Student s = cache.computeIfAbsent(e.getStudentId(), studentMapper::selectById);
            if (s == null) {
                continue;
            }
            sb.append(s.getStudentNo()).append(',')
                    .append(s.getName()).append(',')
                    .append(studentMapper.selectMajorName(s.getMajorId())).append(',')
                    .append(s.getClassName()).append(',')
                    .append(course == null ? "" : course.getName()).append(',')
                    .append(e.getCredits()).append(',')
                    .append(e.getCreatedAt() == null ? "" : e.getCreatedAt().format(FMT))
                    .append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void validate(Api.ClassSaveReq req) {
        if (req.courseId() == null || req.teacherId() == null) {
            throw new BizException(ErrorCode.PARAM_INVALID.getCode(), "请选择课程与授课教师");
        }
        if (req.capacity() == null || req.capacity() <= 0) {
            throw new BizException(ErrorCode.PARAM_INVALID.getCode(), "容量必须大于 0");
        }
        if (req.timeSlots() == null || req.timeSlots().isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID.getCode(), "请至少安排一个上课时间");
        }
    }

    private void applyRequest(TeachingClass tc, Api.ClassSaveReq req) {
        if (req.courseId() != null) {
            tc.setCourseId(req.courseId());
        }
        if (req.classNo() != null && !req.classNo().isBlank()) {
            tc.setClassNo(req.classNo());
        }
        if (tc.getClassNo() == null) {
            tc.setClassNo("01");
        }
        if (req.teacherId() != null) {
            tc.setTeacherId(req.teacherId());
        }
        if (req.capacity() != null) {
            tc.setCapacity(req.capacity());
        }
        tc.setCampus(req.campus() == null ? "军工路" : req.campus());
        tc.setClassroom(req.classroom());
        if (tc.getStatus() == null) {
            tc.setStatus("OPEN");
        }
        if (req.timeSlots() != null) {
            long mask = 0L;
            for (Api.TimeSlot slot : req.timeSlots()) {
                if (slot.dayOfWeek() == null || slot.startSection() == null || slot.endSection() == null) {
                    continue;
                }
                int width = slot.endSection() - slot.startSection() + 1;
                mask |= ((1L << width) - 1) << ((slot.dayOfWeek() - 1) * 12 + slot.startSection() - 1);
            }
            tc.setTimeBitmap(mask);
        }
    }

    private void saveTimes(Long classId, Api.ClassSaveReq req) {
        if (req.timeSlots() == null) {
            return;
        }
        for (Api.TimeSlot slot : req.timeSlots()) {
            ClassTime ct = new ClassTime();
            ct.setTeachingClassId(classId);
            ct.setDayOfWeek(slot.dayOfWeek());
            ct.setStartSection(slot.startSection());
            ct.setEndSection(slot.endSection());
            ct.setWeekType(slot.weekType() == null ? "ALL" : slot.weekType());
            classTimeMapper.insert(ct);
        }
    }

    private double rate(Object numerator, Object denominator) {
        if (numerator == null || denominator == null) {
            return 0;
        }
        double d = ((Number) denominator).doubleValue();
        if (d <= 0) {
            return 0;
        }
        return BigDecimal.valueOf(((Number) numerator).doubleValue() / d)
                .setScale(3, RoundingMode.HALF_UP).doubleValue();
    }

    public LocalDateTime now() {
        return LocalDateTime.now();
    }
}
