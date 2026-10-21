package com.usst.course.security;

/**
 * 当前请求的登录用户。
 */
public record LoginUser(Long userId, String username, String role, Long refId, String name) {

    public boolean isStudent() {
        return "STUDENT".equals(role);
    }

    /** 学生 id，仅学生角色可用 */
    public Long studentId() {
        return refId;
    }
}
