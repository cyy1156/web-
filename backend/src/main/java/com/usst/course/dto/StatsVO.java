package com.usst.course.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class StatsVO {
    private long totalEnrollments;
    private long totalStudents;
    private double avgCourses;
    private int fullClasses;
    private int openClasses;
    private String activeRoundName;
    private List<Map<String, Object>> collegeRates = new ArrayList<>();
    private List<Map<String, Object>> hotCourses = new ArrayList<>();
}
