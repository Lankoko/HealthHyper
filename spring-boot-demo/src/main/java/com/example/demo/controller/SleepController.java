package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.common.UserContext;
import com.example.demo.dto.sleep.SleepUploadRequest;
import com.example.demo.entity.HealthPlan;
import com.example.demo.entity.SleepSession;
import com.example.demo.service.SleepService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.Map;

@Tag(name = "6. 睡眠", description = "睡眠数据上传 / 查询 / AI分析回填 / 睡眠计划（Skill: sleep_analysis）")
@RestController
@RequestMapping("/api/sleep")
@RequiredArgsConstructor
public class SleepController {

    private final SleepService sleepService;

    @PostMapping("/upload")
    public Result<SleepSession> upload(@RequestBody SleepUploadRequest req) {
        return Result.ok(sleepService.uploadSleep(UserContext.get(), req));
    }

    @GetMapping("/latest")
    public Result<Map<String, Object>> getLatest() {
        return Result.ok(sleepService.getLatestNight(UserContext.get()));
    }

    @GetMapping("/recent")
    public Result<List<Map<String, Object>>> getRecent(
            @RequestParam(defaultValue = "7") int days) {
        return Result.ok(sleepService.getRecentNights(UserContext.get(), days));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteSleepSession(@PathVariable Long id) {
        sleepService.deleteSleepSession(UserContext.get(), id);
        return Result.ok();
    }

    @PutMapping("/latest/analysis")
    public Result<SleepSession> updateLatestAnalysis(@RequestBody Map<String, String> body) {
        return Result.ok(sleepService.updateLatestAiAnalysis(
                UserContext.get(), body.get("aiAnalysis")));
    }

    @PutMapping("/{id}/analysis")
    public Result<SleepSession> updateAnalysis(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return Result.ok(sleepService.updateAiAnalysis(
                UserContext.get(), id, body.get("aiAnalysis")));
    }

    @GetMapping("/plan")
    public Result<HealthPlan> getSleepPlan() {
        return Result.ok(sleepService.getSleepPlan(UserContext.get()));
    }

    @PutMapping("/plan")
    public Result<HealthPlan> updateSleepPlan(@RequestBody Map<String, String> body) {
        String itemsJson = body.get("items_json");
        return Result.ok(sleepService.updateSleepPlan(UserContext.get(), itemsJson));
    }
}
