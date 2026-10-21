package com.usst.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("teaching_class")
public class TeachingClass {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long semesterId;
    private Long courseId;
    private String classNo;
    private Long teacherId;
    private Integer capacity;
    /** 已选人数，只允许通过条件 UPDATE 修改 */
    private Integer selectedCount;
    /** 周 1-5 × 12 节共 60 位 */
    private Long timeBitmap;
    private String campus;
    private String classroom;
    /** OPEN / CANCELLED */
    private String status;
}
