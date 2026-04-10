package com.example.demo.dto.alert;

import lombok.Data;

@Data
public class HealthAlertRequest {
    private String alertType;
    private Integer severity;
    private String message;
    private Long vitalSignId;
}
