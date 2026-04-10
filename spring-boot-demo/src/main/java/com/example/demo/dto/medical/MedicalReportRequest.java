package com.example.demo.dto.medical;

import lombok.Data;

import java.time.LocalDate;

@Data
public class MedicalReportRequest {
    private String reportType;
    private String reportTitle;
    private String reportContent;
    private LocalDate recordDate;
    private Long chatSessionId;
}
