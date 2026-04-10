package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.common.UserContext;
import com.example.demo.dto.baseline.BaselineUpdateAllRequest;
import com.example.demo.dto.baseline.BaselineUpdateRequest;
import com.example.demo.entity.Baseline;
import com.example.demo.service.BaselineService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Map;

@Tag(name = "5. 生理基线", description = "基线查询 / 更新（Skill: physiological_baseline）")
@RestController
@RequestMapping("/api/baseline")
@RequiredArgsConstructor
public class BaselineController {

    private final BaselineService baselineService;

    @GetMapping
    public Result<Map<String, Object>> getBaseline(
            @RequestParam(defaultValue = "all") String metric) {
        return Result.ok(baselineService.getBaseline(UserContext.get(), metric));
    }

    @PutMapping
    public Result<Baseline> updateOne(@RequestBody BaselineUpdateRequest req) {
        return Result.ok(baselineService.updateOne(UserContext.get(), req.getMetric(), req.getValue()));
    }

    @PutMapping("/all")
    public Result<Baseline> updateAll(@RequestBody BaselineUpdateAllRequest req) {
        return Result.ok(baselineService.updateAll(UserContext.get(), req));
    }
}
