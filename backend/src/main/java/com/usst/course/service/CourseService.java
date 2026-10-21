package com.usst.course.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.usst.course.common.PageResult;
import com.usst.course.dto.Api;
import com.usst.course.dto.ClassVO;
import com.usst.course.dto.CourseVO;
import com.usst.course.dto.TimeVO;
import com.usst.course.entity.ClassTime;
import com.usst.course.entity.College;
import com.usst.course.entity.Course;
import com.usst.course.entity.TeachingClass;
import com.usst.course.entity.Teacher;
import com.usst.course.mapper.ClassTimeMapper;
import com.usst.course.mapper.CollegeMapper;
import com.usst.course.mapper.CourseMapper;
import com.usst.course.mapper.EnrollmentMapper;
import com.usst.course.mapper.TeachingClassMapper;
import com.usst.course.mapper.TeacherMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseMapper courseMapper;
    private final CollegeMapper collegeMapper;
    private final TeacherMapper teacherMapper;
    private final ClassTimeMapper classTimeMapper;
    private final TeachingClassMapper classMapper;
    private final EnrollmentMapper enrollmentMapper;

    public static String typeText(String type) {
        if (type == null) {
            return "";
        }
        return switch (type) {
            case "REQUIRED" -> "必修";
            case "LIMITED" -> "限选";
            case "ELECTIVE" -> "任选";
            default -> type;
        };
    }

    public PageResult<CourseVO> listCourses(String keyword, String courseType, Long collegeId, int page, int size) {
        LambdaQueryWrapper<Course> q = Wrappers.lambdaQuery();
        if (StringUtils.hasText(keyword)) {
            q.and(w -> w.like(Course::getName, keyword).or().like(Course::getCode, keyword));
        }
        if (StringUtils.hasText(courseType)) {
            q.eq(Course::getCourseType, courseType);
        }
        if (collegeId != null) {
            q.eq(Course::getCollegeId, collegeId);
        }
        q.orderByAsc(Course::getCode);

        Page<Course> p = courseMapper.selectPage(new Page<>(page, size), q);
        Map<Long, String> collegeNames = collegeNames();
        List<CourseVO> list = p.getRecords().stream().map(c -> {
            CourseVO vo = new CourseVO();
            vo.setId(c.getId());
            vo.setCode(c.getCode());
            vo.setName(c.getName());
            vo.setCredits(c.getCredits());
            vo.setHours(c.getHours());
            vo.setCourseType(c.getCourseType());
            vo.setCourseTypeText(typeText(c.getCourseType()));
            vo.setAssessment("EXAM".equals(c.getAssessment()) ? "考试" : "考查");
            vo.setCollegeId(c.getCollegeId());
            vo.setCollegeName(collegeNames.getOrDefault(c.getCollegeId(), ""));
            vo.setDescription(c.getDescription());
            return vo;
        }).toList();
        return PageResult.of(list, p.getTotal(), page, size);
    }

    public PageResult<ClassVO> listClasses(Long semesterId, Long courseId, String keyword, Long collegeId,
                                           String courseType, boolean onlyAvailable, String status,
                                           int page, int size, Long studentId) {
        Set<Long> courseIds = null;
        boolean filtered = false;
        LambdaQueryWrapper<Course> cq = Wrappers.lambdaQuery();
        if (StringUtils.hasText(keyword)) {
            filtered = true;
            cq.and(w -> w.like(Course::getName, keyword).or().like(Course::getCode, keyword));
        }
        if (StringUtils.hasText(courseType)) {
            filtered = true;
            List<String> types = Arrays.asList(courseType.split(","));
            if (types.size() > 1) {
                cq.in(Course::getCourseType, types);
            } else {
                cq.eq(Course::getCourseType, courseType);
            }
        }
        if (collegeId != null) {
            filtered = true;
            cq.eq(Course::getCollegeId, collegeId);
        }
        if (filtered) {
            courseIds = courseMapper.selectList(cq).stream().map(Course::getId).collect(Collectors.toSet());
            if (courseIds.isEmpty()) {
                return PageResult.of(Collections.emptyList(), 0, page, size);
            }
        }

        LambdaQueryWrapper<TeachingClass> q = Wrappers.lambdaQuery();
        q.eq(TeachingClass::getSemesterId, semesterId);
        q.eq(courseId != null, TeachingClass::getCourseId, courseId);
        if (StringUtils.hasText(status)) {
            q.eq(TeachingClass::getStatus, status);
        } else {
            q.eq(TeachingClass::getStatus, "OPEN");
        }
        if (courseIds != null) {
            q.in(TeachingClass::getCourseId, courseIds);
        }
        if (onlyAvailable) {
            q.apply("selected_count < capacity");
        }
        q.orderByDesc(TeachingClass::getSelectedCount).orderByAsc(TeachingClass::getId);

        Page<TeachingClass> p = classMapper.selectPage(new Page<>(page, size), q);
        return PageResult.of(assemble(p.getRecords(), studentId), p.getTotal(), page, size);
    }

    /** 批量查询余量，供前端 3 秒轮询 */
    public List<Api.RemainItem> remain(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return classMapper.selectBatchIds(ids).stream()
                .map(tc -> new Api.RemainItem(tc.getId(),
                        Math.max(0, tc.getCapacity() - tc.getSelectedCount()),
                        tc.getSelectedCount(), tc.getStatus()))
                .toList();
    }

    public List<ClassVO> assemble(List<TeachingClass> classes, Long studentId) {
        if (classes.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> courseIds = classes.stream().map(TeachingClass::getCourseId).collect(Collectors.toSet());
        Set<Long> teacherIds = classes.stream().map(TeachingClass::getTeacherId).collect(Collectors.toSet());
        List<Long> classIds = classes.stream().map(TeachingClass::getId).toList();

        Map<Long, Course> courses = courseMapper.selectBatchIds(courseIds).stream()
                .collect(Collectors.toMap(Course::getId, c -> c));
        Map<Long, Teacher> teachers = teacherMapper.selectBatchIds(teacherIds).stream()
                .collect(Collectors.toMap(Teacher::getId, t -> t));
        Map<Long, List<TimeVO>> timeMap = classTimeMapper
                .selectList(Wrappers.<ClassTime>lambdaQuery().in(ClassTime::getTeachingClassId, classIds))
                .stream()
                .collect(Collectors.groupingBy(ClassTime::getTeachingClassId,
                        Collectors.mapping(t -> new TimeVO(t.getDayOfWeek(), t.getStartSection(),
                                t.getEndSection(), t.getWeekType()), Collectors.toList())));

        Set<Long> selected = new HashSet<>();
        Long busy = 0L;
        if (studentId != null) {
            Long semesterId = classes.get(0).getSemesterId();
            selected.addAll(enrollmentMapper.selectSelectedClassIds(studentId, semesterId));
            busy = enrollmentMapper.selectBusyBitmap(studentId, semesterId);
        }

        List<ClassVO> result = new ArrayList<>(classes.size());
        for (TeachingClass tc : classes) {
            Course c = courses.get(tc.getCourseId());
            Teacher t = teachers.get(tc.getTeacherId());
            ClassVO vo = new ClassVO();
            vo.setId(tc.getId());
            vo.setCourseId(tc.getCourseId());
            vo.setClassNo(tc.getClassNo());
            if (c != null) {
                vo.setCourseCode(c.getCode());
                vo.setCourseName(c.getName());
                vo.setCredits(c.getCredits());
                vo.setCourseType(c.getCourseType());
                vo.setCourseTypeText(typeText(c.getCourseType()));
            }
            if (t != null) {
                vo.setTeacherName(t.getName());
                vo.setTeacherTitle(t.getTitle());
            }
            List<TimeVO> times = new ArrayList<>(timeMap.getOrDefault(tc.getId(), List.of()));
            times.sort(Comparator.comparing(TimeVO::getDayOfWeek).thenComparing(TimeVO::getStartSection));
            vo.setTimes(times);
            vo.setTimeText(times.isEmpty() ? "时间待定"
                    : times.stream().map(TimeVO::text).collect(Collectors.joining(" / ")));
            vo.setCampus(tc.getCampus());
            vo.setClassroom(tc.getClassroom());
            vo.setCapacity(tc.getCapacity());
            vo.setSelectedCount(tc.getSelectedCount());
            vo.setRemaining(Math.max(0, tc.getCapacity() - tc.getSelectedCount()));
            vo.setStatus(tc.getStatus());
            boolean mine = selected.contains(tc.getId());
            vo.setMyStatus(mine ? "SELECTED" : "NONE");
            long mask = tc.getTimeBitmap() == null ? 0L : tc.getTimeBitmap();
            vo.setConflict(!mine && busy != null && mask != 0 && (busy & mask) != 0);
            result.add(vo);
        }
        return result;
    }

    private Map<Long, String> collegeNames() {
        Map<Long, String> map = new HashMap<>();
        for (College c : collegeMapper.selectList(null)) {
            map.put(c.getId(), c.getName());
        }
        return map;
    }
}
