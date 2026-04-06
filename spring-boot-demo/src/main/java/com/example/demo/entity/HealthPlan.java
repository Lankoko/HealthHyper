package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("health_plan")
public class HealthPlan {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private String itemsJson;
    private String source;
    private Long chatSessionId;
    private Integer status;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime createdAt;
}
