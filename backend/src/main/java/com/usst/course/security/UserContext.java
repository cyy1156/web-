package com.usst.course.security;

import com.usst.course.common.BizException;
import com.usst.course.common.ErrorCode;

/**
 * 当前线程的登录用户上下文。
 */
public final class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static LoginUser require() {
        LoginUser user = HOLDER.get();
        if (user == null) {
            throw BizException.of(ErrorCode.UNAUTHORIZED);
        }
        return user;
    }

    public static Long requireStudentId() {
        LoginUser user = require();
        if (user.refId() == null) {
            throw BizException.of(ErrorCode.FORBIDDEN);
        }
        return user.refId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
