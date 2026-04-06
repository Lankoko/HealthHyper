package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_health_summary")
public class AiHealthSummary {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String summaryJson;
    private LocalDateTime lastAnalyzedAt;
    private LocalDateTime updatedAt;
}
