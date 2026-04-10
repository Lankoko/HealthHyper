package com.example.demo.dto.plan;

import lombok.Data;

@Data
public class HealthPlanRequest {
    private String title;
    private String content;
    private Integer days;
    private Long chatSessionId;
    private String source;
}
