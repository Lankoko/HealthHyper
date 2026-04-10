package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("sleep_session")
public class SleepSession {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private LocalDate sleepDate;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Float nightAvgHr;
    private Float nightMaxHr;
    private Float nightMinHr;
    private Float nightAvgSpo2;
    private Float nightMaxSpo2;
    private Float nightMinSpo2;
    private Float nightAvgBt;
    private Float nightMaxBt;
    private Float nightMinBt;
    private String aiAnalysis;
    private LocalDateTime createdAt;
}
