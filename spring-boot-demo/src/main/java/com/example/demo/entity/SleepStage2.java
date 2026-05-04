package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sleep_stage2")
public class SleepStage2 {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    /** 清醒占比 0-100，如 5.0 表示 5% */
    private Float wakePct;
    private Float n1Pct;
    private Float n2Pct;
    /** N3 深睡占比（N4 合并入此字段） */
    private Float n3Pct;
    private Float remPct;
}
