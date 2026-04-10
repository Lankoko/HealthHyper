package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("medication_plan")
public class MedicationPlan {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String drugName;
    private String dosage;
    private String frequency;
    private String timeSlots;
    private LocalDate startDate;
    private LocalDate endDate;
    private String notes;
    private Integer status;
    private LocalDateTime createdAt;
}
