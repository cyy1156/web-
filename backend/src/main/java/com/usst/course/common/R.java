package com.usst.course.common;

import lombok.Data;

/**
 * 统一响应体。业务失败仍返回 HTTP 200，由 code 区分。
 */
@Data
public class R<T> {

    private int code;
    private String message;
    private T data;

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.code = 0;
        r.message = "ok";
        r.data = data;
        return r;
    }

    public static R<Void> ok() {
        return ok(null);
    }

    public static <T> R<T> fail(int code, String message) {
        R<T> r = new R<>();
        r.code = code;
        r.message = message;
        return r;
    }

    public static <T> R<T> fail(ErrorCode ec) {
        return fail(ec.getCode(), ec.getDefaultMessage());
    }
}
