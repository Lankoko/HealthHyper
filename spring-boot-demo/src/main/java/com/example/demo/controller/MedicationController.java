package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.common.UserContext;
import com.example.demo.dto.medication.MedicationPlanRequest;
import com.example.demo.dto.medication.MedicationTakingRequest;
import com.example.demo.entity.MedicationLog;
import com.example.demo.entity.MedicationPlan;
import com.example.demo.service.MedicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.Map;

@Tag(name = "7. 用药管理", description = "用药计划 / 服药记录（Skill: medication_assistant）")
@RestController
@RequestMapping("/api/medication")
@RequiredArgsConstructor
public class MedicationController {

    private final MedicationService medicationService;

    @PostMapping("/plan")
    public Result<MedicationPlan> addPlan(@RequestBody MedicationPlanRequest req) {
        return Result.ok(medicationService.addPlan(UserContext.get(), req));
    }

    @GetMapping("/plan/latest")
    public Result<MedicationPlan> getLatestPlan() {
        return Result.ok(medicationService.getLatestPlan(UserContext.get()));
    }

    @PostMapping("/log")
    public Result<MedicationLog> recordTaking(@RequestBody MedicationTakingRequest req) {
        return Result.ok(medicationService.recordTaking(UserContext.get(), req));
    }

    @GetMapping("/log")
    public Result<List<Map<String, Object>>> getLogs(
            @RequestParam(defaultValue = "3") int days) {
        return Result.ok(medicationService.getLogs(UserContext.get(), days));
    }
}
