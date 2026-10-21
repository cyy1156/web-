package com.usst.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("class_time")
public class ClassTime {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long teachingClassId;
    private Integer dayOfWeek;
    private Integer startSection;
    private Integer endSection;
    /** ALL / ODD / EVEN */
    private String weekType;
}
