package com.usst.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("training_plan")
public class TrainingPlan {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long majorId;
    private Integer grade;
    private Integer termNo;
    private Long courseId;
    private String requiredType;
}
