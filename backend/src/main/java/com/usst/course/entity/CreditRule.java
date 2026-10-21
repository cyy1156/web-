package com.usst.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("credit_rule")
public class CreditRule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long majorId;
    private Integer termNo;
    private BigDecimal planCredits;
    private BigDecimal minCredits;
    private BigDecimal maxCredits;
}
