package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.common.UserContext;
import com.example.demo.dto.vital.VitalSignUploadRequest;
import com.example.demo.entity.VitalSign;
import com.example.demo.service.VitalSignService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Map;

@Tag(name = "4. 实时生理数据", description = "上报 / 查询 / 统计（Skill: physiological_analysis）")
@RestController
@RequestMapping("/api/vital")
@RequiredArgsConstructor
public class VitalSignController {

    private final VitalSignService vitalSignService;

    @PostMapping("/upload")
    public Result<VitalSign> upload(@RequestBody VitalSignUploadRequest req) {
        return Result.ok(vitalSignService.upload(UserContext.get(), req));
    }

    @GetMapping("/current")
    public Result<Map<String, Object>> getCurrent(
            @RequestParam(defaultValue = "all") String metric) {
        return Result.ok(vitalSignService.getCurrent(UserContext.get(), metric));
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats(
            @RequestParam(defaultValue = "all") String metric,
            @RequestParam(defaultValue = "7") int days) {
        return Result.ok(vitalSignService.getStats(UserContext.get(), metric, days));
    }
}
