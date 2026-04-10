package com.example.demo.dto.medication;

import lombok.Data;

@Data
public class MedicationTakingRequest {
    private String drugName;
    private String time;
}
