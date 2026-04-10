package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("daily_log")
public class DailyLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String logType;
    private String content;
    private String extraJson;
    private LocalDate logDate;
    private LocalDateTime createdAt;
}
