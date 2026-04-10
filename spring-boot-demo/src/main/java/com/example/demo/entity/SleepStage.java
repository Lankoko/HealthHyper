package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sleep_stage")
public class SleepStage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private String stage;
    private LocalDateTime startTime;
    private Integer durationSec;
}
