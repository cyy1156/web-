package com.usst.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("enrollment_log")
public class EnrollmentLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private Long teachingClassId;
    private String action;
    private Integer resultCode;
    private String message;
    private String idempotencyKey;
    private Integer costMs;
    private LocalDateTime createdAt;
}
