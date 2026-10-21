package com.usst.course.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.usst.course.common.BizException;
import com.usst.course.common.ErrorCode;
import com.usst.course.dto.Api;
import com.usst.course.entity.Student;
import com.usst.course.entity.SysUser;
import com.usst.course.mapper.StudentMapper;
import com.usst.course.mapper.SysUserMapper;
import com.usst.course.security.JwtUtil;
import com.usst.course.security.LoginUser;
import com.usst.course.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final StudentMapper studentMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public Api.LoginRes login(Api.LoginReq req) {
        SysUser user = userMapper.selectOne(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, req.username().trim()));
        if (user == null || !passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new BizException(ErrorCode.PARAM_INVALID.getCode(), "学号或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(ErrorCode.FORBIDDEN.getCode(), "账号已被停用");
        }

        LoginUser loginUser = new LoginUser(user.getId(), user.getUsername(), user.getRole(),
                user.getRefId(), displayName(user));
        String token = jwtUtil.create(loginUser);

        if ("STUDENT".equals(user.getRole()) && user.getRefId() != null) {
            Student s = studentMapper.selectById(user.getRefId());
            if (s != null) {
                return new Api.LoginRes(token, user.getRole(), s.getName(), user.getId(), s.getId(),
                        s.getStudentNo(), s.getGrade(), studentMapper.selectMajorName(s.getMajorId()),
                        s.getClassName());
            }
        }
        return new Api.LoginRes(token, user.getRole(), "教务管理员", user.getId(), null,
                null, null, null, null);
    }

    public Api.MeRes me() {
        LoginUser user = UserContext.require();
        if ("STUDENT".equals(user.role()) && user.refId() != null) {
            Student s = studentMapper.selectById(user.refId());
            if (s != null) {
                return new Api.MeRes(user.role(), s.getName(), s.getId(), s.getStudentNo(), s.getGrade(),
                        studentMapper.selectMajorName(s.getMajorId()), s.getClassName());
            }
        }
        return new Api.MeRes(user.role(), user.name(), null, null, null, null, null);
    }

    private String displayName(SysUser user) {
        if ("STUDENT".equals(user.getRole()) && user.getRefId() != null) {
            Student s = studentMapper.selectById(user.getRefId());
            return s == null ? user.getUsername() : s.getName();
        }
        return "教务管理员";
    }
}
