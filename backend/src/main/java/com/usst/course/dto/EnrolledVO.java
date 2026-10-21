package com.usst.course.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class EnrolledVO {
    private Long id;
    private Long teachingClassId;
    private Long courseId;
    private String courseName;
    private BigDecimal credits;
    private String courseType;
    private String courseTypeText;
    private String teacherName;
    private String timeText;
    private String campus;
    private String classroom;
    private Boolean conflicted;
}
