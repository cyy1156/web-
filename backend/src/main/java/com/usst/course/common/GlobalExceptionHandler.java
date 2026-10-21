package com.usst.course.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常转换：所有异常都转成统一响应，不允许把堆栈抛给前端。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<R<Void>> handleBiz(BizException e) {
        HttpStatus status = HttpStatus.OK;
        if (e.getCode() == ErrorCode.UNAUTHORIZED.getCode()) {
            status = HttpStatus.UNAUTHORIZED;
        } else if (e.getCode() == ErrorCode.FORBIDDEN.getCode()) {
            status = HttpStatus.FORBIDDEN;
        }
        return ResponseEntity.status(status).body(R.fail(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleValidation(MethodArgumentNotValidException e) {
        FieldError fe = e.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        String msg = fe == null ? ErrorCode.PARAM_INVALID.getDefaultMessage() : fe.getDefaultMessage();
        return R.fail(ErrorCode.PARAM_INVALID.getCode(), msg);
    }

    @ExceptionHandler(Exception.class)
    public R<Void> handleOther(Exception e) {
        log.error("未捕获异常", e);
        return R.fail(ErrorCode.SERVER_ERROR);
    }
}
