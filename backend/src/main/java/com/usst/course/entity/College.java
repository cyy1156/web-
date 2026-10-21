package com.usst.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("college")
public class College {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
}
