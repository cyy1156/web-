package com.usst.course.controller;

import com.usst.course.common.PageResult;
import com.usst.course.common.R;
import com.usst.course.dto.Api;
import com.usst.course.dto.ClassVO;
import com.usst.course.dto.StatsVO;
import com.usst.course.security.RequireRole;
import com.usst.course.service.AdminService;
import com.usst.course.service.CourseService;
import com.usst.course.service.SemesterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@RequireRole("ADMIN")
public class AdminController {

    private final AdminService adminService;
    private final CourseService courseService;
    private final SemesterService semesterService;

    @GetMapping("/rounds")
    public R<List<Api.RoundInfo>> rounds(@RequestParam(required = false) Long semesterId) {
        return R.ok(adminService.rounds(semesterId));
    }

    @PutMapping("/rounds/{id}")
    public R<Void> updateRound(@PathVariable Long id, @RequestBody Api.RoundUpdateReq req) {
        adminService.updateRound(id, req);
        return R.ok();
    }

    @GetMapping("/classes")
    public R<PageResult<ClassVO>> classes(@RequestParam(required = false) Long semesterId,
                                          @RequestParam(required = false) Long courseId,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) Long collegeId,
                                          @RequestParam(required = false) String courseType,
                                          @RequestParam(required = false) String status,
                                          @RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "10") int size) {
        Long semId = semesterId != null ? semesterId : semesterService.current().getId();
        return R.ok(courseService.listClasses(semId, courseId, keyword, collegeId, courseType,
                false, status, page, size, null));
    }

    @PostMapping("/classes")
    public R<Long> createClass(@RequestBody Api.ClassSaveReq req) {
        return R.ok(adminService.createClass(req));
    }

    @PutMapping("/classes/{id}")
    public R<Void> updateClass(@PathVariable Long id, @RequestBody Api.ClassSaveReq req) {
        adminService.updateClass(id, req);
        return R.ok();
    }

    @PostMapping("/classes/{id}/cancel")
    public R<Void> cancelClass(@PathVariable Long id) {
        adminService.cancelClass(id);
        return R.ok();
    }

    @GetMapping("/stats/overview")
    public R<StatsVO> stats(@RequestParam(required = false) Long semesterId) {
        return R.ok(adminService.stats(semesterId));
    }

    @GetMapping("/enrollments/export")
    public ResponseEntity<byte[]> export(@RequestParam Long teachingClassId) {
        byte[] data = adminService.exportClass(teachingClassId);
        String filename = URLEncoder.encode("选课名单-" + teachingClassId + ".csv", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(data);
    }
}
