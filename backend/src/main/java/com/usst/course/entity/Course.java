package com.usst.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("course")
public class Course {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    private BigDecimal credits;
    private Integer hours;
    /** REQUIRED 必修 / LIMITED 限选 / ELECTIVE 任选 */
    private String courseType;
    private String assessment;
    private Long collegeId;
    private String description;
}
