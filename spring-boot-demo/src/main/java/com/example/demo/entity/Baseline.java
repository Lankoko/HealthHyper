package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("baseline")
public class Baseline {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String source;
    private Float hrBase;
    private Float spo2Base;
    private Float btBase;
    private Float hrCvBase;
    private Float sdannBase;
    private LocalDateTime effectiveAt;
    private LocalDateTime createdAt;
}
