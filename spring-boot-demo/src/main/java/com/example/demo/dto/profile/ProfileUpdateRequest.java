package com.example.demo.dto.profile;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ProfileUpdateRequest {
    private Integer gender;
    private LocalDate birthDate;
    private BigDecimal heightCm;
    private BigDecimal weightKg;
    private String bloodType;
    private String medicalHistory;
    private String allergyInfo;
    private String lifestyleInfo;
    private String extraJson;
}
