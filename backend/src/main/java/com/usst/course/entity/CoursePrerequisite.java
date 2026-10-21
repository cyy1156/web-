package com.usst.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("course_prerequisite")
public class CoursePrerequisite {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long courseId;
    private Long preCourseId;
    private Integer minScore;
}
