package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("health_profile_history")
public class HealthProfileHistory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String triggerSource;
    private String snapshotJson;
    private String changeDesc;
    private LocalDateTime createdAt;
}
