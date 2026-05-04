package com.example.demo.dto.sleep;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 新版睡眠上报格式（硬件直传，分期以百分比字符串表示）。
 * 字段名与硬件 JSON 完全一致（含拼写 startime）。
 */
@Data
public class SleepUpload2Request {

    /** 睡眠日期，如 "2026-04-09" */
    @JsonProperty("sleepdate")
    private String sleepdate;

    /** 入睡时间，如 "2026-04-08 23:00:00" 或 "2026-04-08T23:00:00" */
    @JsonProperty("startime")
    private String startime;

    /** 起床时间 */
    @JsonProperty("endtime")
    private String endtime;

    /** 清醒占比，如 "5%" */
    @JsonProperty("wake")
    private String wake;

    /** N1 浅睡占比 */
    @JsonProperty("n1")
    private String n1;

    /** N2 浅睡占比 */
    @JsonProperty("n2")
    private String n2;

    /** N3 深睡占比（N4 已合并，无 n4 字段） */
    @JsonProperty("n3")
    private String n3;

    /** REM 占比 */
    @JsonProperty("rem")
    private String rem;
}
