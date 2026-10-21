package com.usst.course.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.usst.course.common.PageResult;
import com.usst.course.common.R;
import com.usst.course.dto.Api;
import com.usst.course.dto.ClassVO;
import com.usst.course.dto.CourseVO;
import com.usst.course.entity.College;
import com.usst.course.entity.Teacher;
import com.usst.course.mapper.CollegeMapper;
import com.usst.course.mapper.TeacherMapper;
import com.usst.course.security.LoginUser;
import com.usst.course.security.UserContext;
import com.usst.course.service.CourseService;
import com.usst.course.service.SemesterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final SemesterService semesterService;
    private final CollegeMapper collegeMapper;
    private final TeacherMapper teacherMapper;

    @GetMapping("/colleges")
    public R<List<College>> colleges() {
        return R.ok(collegeMapper.selectList(null));
    }

    @GetMapping("/teachers")
    public R<List<Teacher>> teachers(@RequestParam(required = false) Long collegeId) {
        return R.ok(teacherMapper.selectList(Wrappers.<Teacher>lambdaQuery()
                .eq(collegeId != null, Teacher::getCollegeId, collegeId)
                .orderByAsc(Teacher::getTeacherNo)));
    }

    @GetMapping("/courses")
    public R<PageResult<CourseVO>> courses(@RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) String courseType,
                                           @RequestParam(required = false) Long collegeId,
                                           @RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        return R.ok(courseService.listCourses(keyword, courseType, collegeId, page, size));
    }

    @GetMapping("/teaching-classes")
    public R<PageResult<ClassVO>> classes(@RequestParam(required = false) Long semesterId,
                                          @RequestParam(required = false) Long courseId,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) Long collegeId,
                                          @RequestParam(required = false) String courseType,
                                          @RequestParam(defaultValue = "false") boolean onlyAvailable,
                                          @RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        Long semId = semesterId != null ? semesterId : semesterService.current().getId();
        LoginUser user = UserContext.require();
        Long studentId = user.isStudent() ? user.refId() : null;
        return R.ok(courseService.listClasses(semId, courseId, keyword, collegeId, courseType,
                onlyAvailable, "OPEN", page, size, studentId));
    }

    @PostMapping("/teaching-classes/remain")
    public R<List<Api.RemainItem>> remain(@RequestBody RemainReq req) {
        return R.ok(courseService.remain(req.ids()));
    }

    public record RemainReq(List<Long> ids) {
    }
}
