package com.usst.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 选课记录。status_flag 生成列由数据库维护，这里不映射。
 */
@Data
@TableName("enrollment")
public class Enrollment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private Long semesterId;
    private Long teachingClassId;
    private Long courseId;
    private Long roundId;
    private BigDecimal credits;
    /** SELECTED / DROPPED */
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime droppedAt;
}
