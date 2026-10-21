package com.usst.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName("semester")
public class Semester {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer year;
    private Integer term;
    private String name;
    private LocalDate startDate;
    private Integer totalWeeks;
    private Integer currentWeek;
    /** 名单是否锁定 */
    private Integer locked;
}
