package com.usst.course.controller;

import com.usst.course.common.R;
import com.usst.course.dto.Api;
import com.usst.course.service.SemesterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/semesters")
@RequiredArgsConstructor
public class SemesterController {

    private final SemesterService semesterService;

    @GetMapping("/current")
    public R<Api.SemesterInfo> current() {
        return R.ok(semesterService.info());
    }
}
