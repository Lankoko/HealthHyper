package com.example.demo.dto.daily;

import lombok.Data;

import java.time.LocalDate;

@Data
public class DailyLogRequest {
    private String logType;
    private String content;
    private LocalDate logDate;
    private String extraJson;
}
