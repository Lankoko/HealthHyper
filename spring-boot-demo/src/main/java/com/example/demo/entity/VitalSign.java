package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("vital_sign")
public class VitalSign {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long deviceId;
    private Short hr;
    private Short spo2;
    private BigDecimal bt;
    private Short activity;
    private Float sdann;
    private Float hrCv;
    private Integer flag;
    private LocalDateTime recordedAt;
}
