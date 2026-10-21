package com.usst.course.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 教学班视图，含余量与当前学生状态。
 */
@Data
public class ClassVO {
    private Long id;
    private Long courseId;
    private String classNo;
    private String courseCode;
    private String courseName;
    private BigDecimal credits;
    private String courseType;
    private String courseTypeText;
    private String teacherName;
    private String teacherTitle;
    private List<TimeVO> times = new ArrayList<>();
    private String timeText;
    private String campus;
    private String classroom;
    private Integer capacity;
    private Integer selectedCount;
    private Integer remaining;
    private String status;
    /** SELECTED / NONE */
    private String myStatus = "NONE";
    /** 与已选课程是否冲突 */
    private Boolean conflict = false;
}
