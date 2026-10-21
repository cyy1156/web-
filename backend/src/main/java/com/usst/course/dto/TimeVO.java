package com.usst.course.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeVO {
    private Integer dayOfWeek;
    private Integer startSection;
    private Integer endSection;
    private String weekType;

    public String text() {
        return "周" + "一二三四五六日".charAt(dayOfWeek - 1) + " " + startSection + "-" + endSection + "节";
    }
}
