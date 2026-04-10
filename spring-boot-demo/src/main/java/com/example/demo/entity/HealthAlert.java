package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("health_alert")
public class HealthAlert {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String alertType;
    private Integer severity;
    private String message;
    private Long vitalSignId;
    private Long triggeredSessionId;
    private Integer isRead;
    private LocalDateTime createdAt;
}
