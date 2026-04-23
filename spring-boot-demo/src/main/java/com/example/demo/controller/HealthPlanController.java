package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.common.UserContext;
import com.example.demo.dto.plan.HealthPlanRequest;
import com.example.demo.entity.HealthPlan;
import com.example.demo.service.HealthPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "10. 健康计划", description = "计划创建 / 打卡 / 查询（Skill: health_planning）")
@RestController
@RequestMapping("/api/plan")
@RequiredArgsConstructor
public class HealthPlanController {

    private final HealthPlanService healthPlanService;

    @PostMapping
    public Result<HealthPlan> createPlan(@RequestBody HealthPlanRequest req) {
        return Result.ok(healthPlanService.createPlan(UserContext.get(), req));
    }

    @PutMapping("/{id}")
    public Result<HealthPlan> updatePlan(@PathVariable Long id, @RequestBody HealthPlanRequest req) {
        return Result.ok(healthPlanService.updatePlan(UserContext.get(), id, req));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deletePlan(@PathVariable Long id) {
        healthPlanService.deletePlan(UserContext.get(), id);
        return Result.ok();
    }

    @GetMapping
    public Result<List<HealthPlan>> getPlans(
            @RequestParam(defaultValue = "3") int days) {
        return Result.ok(healthPlanService.getPlans(UserContext.get(), days));
    }

    @GetMapping("/latest")
    public Result<HealthPlan> getLatest() {
        return Result.ok(healthPlanService.getLatest(UserContext.get()));
    }

    @PutMapping("/{id}/checkin")
    public Result<HealthPlan> checkin(@PathVariable Long id) {
        return Result.ok(healthPlanService.checkin(UserContext.get(), id));
    }

    @PutMapping("/{id}/uncheckin")
    public Result<HealthPlan> uncheckin(@PathVariable Long id) {
        return Result.ok(healthPlanService.uncheckin(UserContext.get(), id));
    }
}
