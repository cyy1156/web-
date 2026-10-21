package com.usst.course.controller;

import com.usst.course.common.R;
import com.usst.course.dto.Api;
import com.usst.course.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public R<Api.LoginRes> login(@Valid @RequestBody Api.LoginReq req) {
        return R.ok(authService.login(req));
    }

    @GetMapping("/me")
    public R<Api.MeRes> me() {
        return R.ok(authService.me());
    }
}
