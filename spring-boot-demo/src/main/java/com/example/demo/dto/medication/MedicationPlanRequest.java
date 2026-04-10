package com.example.demo.dto.medication;

import lombok.Data;

import java.time.LocalDate;

@Data
public class MedicationPlanRequest {
    private String drugName;
    private String dosage;
    private String frequency;
    private String timeSlots;
    private LocalDate startDate;
    private LocalDate endDate;
    private String notes;
}
