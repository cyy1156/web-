package com.usst.course.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CourseVO {
    private Long id;
    private String code;
    private String name;
    private BigDecimal credits;
    private Integer hours;
    private String courseType;
    private String courseTypeText;
    private String assessment;
    private Long collegeId;
    private String collegeName;
    private String description;
}
