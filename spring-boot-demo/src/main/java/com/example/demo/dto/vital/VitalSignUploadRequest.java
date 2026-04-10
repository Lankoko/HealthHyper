package com.example.demo.dto.vital;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class VitalSignUploadRequest {
    private Long deviceId;
    private Short hr;
    private Short spo2;
    private BigDecimal bt;
    private Short activity;
    private Float sdann;
    private Float hrCv;
    private Integer flag;
    private LocalDateTime recordedAt;
}
