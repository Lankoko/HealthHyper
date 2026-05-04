package com.example.demo.dto.vital;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VitalSignUploadRequest {
    private Long deviceId;
    private Float hr;
    private Float spo2;
    private Float bt;
    private Float activity;
    private Float turnOut;
    private Float sdann;
    private Float hrCv;
    private Integer flag;
    private LocalDateTime recordedAt;
}
