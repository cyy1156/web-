package com.usst.course.common;

/**
 * 业务异常。message 为可直接展示给学生的中文文案。
 */
public class BizException extends RuntimeException {

    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public static BizException of(ErrorCode ec) {
        return new BizException(ec.getCode(), ec.getDefaultMessage());
    }

    public static BizException of(ErrorCode ec, String message) {
        return new BizException(ec.getCode(), message);
    }

    public int getCode() {
        return code;
    }
}
