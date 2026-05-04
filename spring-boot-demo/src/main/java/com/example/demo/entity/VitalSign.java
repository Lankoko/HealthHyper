package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("vital_sign")
public class VitalSign {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long deviceId;
    private Float hr;
    private Float spo2;
    private Float bt;
    private Float activity;
    private Float turnOut;
    private Float sdann;
    private Float hrCv;
    private Integer flag;
    private LocalDateTime recordedAt;
}
