package com.usst.course.controller;

import com.usst.course.common.R;
import com.usst.course.dto.Api;
import com.usst.course.dto.EnrolledVO;
import com.usst.course.security.RequireRole;
import com.usst.course.security.UserContext;
import com.usst.course.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
@RequireRole("STUDENT")
public class StudentController {

    private final StudentService studentService;

    @GetMapping("/enrollments")
    public R<List<EnrolledVO>> enrollments(@RequestParam(required = false) Long semesterId) {
        return R.ok(studentService.listEnrollments(UserContext.requireStudentId(), semesterId));
    }

    @GetMapping("/timetable")
    public R<Api.TimetableRes> timetable(@RequestParam(required = false) Long semesterId) {
        return R.ok(studentService.timetable(UserContext.requireStudentId(), semesterId));
    }

    @GetMapping("/credit-status")
    public R<Api.CreditStatus> creditStatus(@RequestParam(required = false) Long semesterId) {
        return R.ok(studentService.creditStatus(UserContext.requireStudentId(), semesterId));
    }

    @GetMapping("/plan")
    public R<Api.PlanRes> plan(@RequestParam(required = false) Long semesterId) {
        return R.ok(studentService.plan(UserContext.requireStudentId(), semesterId));
    }
}
