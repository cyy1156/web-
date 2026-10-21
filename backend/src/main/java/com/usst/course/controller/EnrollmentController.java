package com.usst.course.controller;

import com.usst.course.common.R;
import com.usst.course.dto.Api;
import com.usst.course.security.RequireRole;
import com.usst.course.security.UserContext;
import com.usst.course.service.EnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
@RequireRole("STUDENT")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping
    public R<Api.EnrollRes> enroll(@RequestHeader(value = "Idempotency-Key", required = false) String key,
                                   @Valid @RequestBody Api.EnrollReq req) {
        String idemKey = (key == null || key.isBlank()) ? UUID.randomUUID().toString() : key;
        return R.ok(enrollmentService.enroll(UserContext.requireStudentId(), req.teachingClassId(), idemKey));
    }

    @DeleteMapping("/{id}")
    public R<Void> drop(@PathVariable Long id) {
        enrollmentService.drop(UserContext.requireStudentId(), id);
        return R.ok();
    }
}
