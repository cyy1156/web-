package com.usst.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("selection_round")
public class SelectionRound {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long semesterId;
    private Integer roundNo;
    private String name;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer allowDrop;
    /** PENDING / OPEN / CLOSED */
    private String status;
}
