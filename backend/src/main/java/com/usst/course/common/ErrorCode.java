package com.usst.course.common;

import lombok.Getter;

/**
 * 业务错误码。前端直接展示 message，不展示数字码。
 */
@Getter
public enum ErrorCode {

    OK(0, "ok"),

    NOT_IN_WINDOW(40001, "当前不在选课时间内"),
    CLASS_FULL(40002, "该教学班名额已满"),
    TIME_CONFLICT(40003, "与已选课程时间冲突"),
    PREREQUISITE(40004, "先修课程未通过"),
    CREDIT_LIMIT(40005, "超出学期学分上限"),
    DUPLICATE(40006, "已选过该课程"),
    CROSS_GRADE(40007, "低年级不可选修高年级课程"),
    NOT_DROPPABLE(40008, "该课程不可退选"),
    TOO_FREQUENT(40009, "操作过于频繁，请稍后再试"),
    LOCKED(40010, "选课名单已锁定，无法变更"),

    PARAM_INVALID(40000, "请求参数不合法"),
    UNAUTHORIZED(40100, "登录状态已失效，请重新登录"),
    FORBIDDEN(40300, "无权访问该资源"),
    SERVER_ERROR(50000, "服务器开小差了，请稍后再试");

    private final int code;
    private final String defaultMessage;

    ErrorCode(int code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }
}
