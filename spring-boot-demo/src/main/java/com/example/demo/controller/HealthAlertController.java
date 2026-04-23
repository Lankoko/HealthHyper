package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.common.UserContext;
import com.example.demo.dto.alert.HealthAlertRequest;
import com.example.demo.entity.HealthAlert;
import com.example.demo.service.HealthAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "12. 异常告警", description = "告警创建 / 查询 / 已读（vital_sign flag>0 自动生成）")
@RestController
@RequestMapping("/api/alert")
@RequiredArgsConstructor
public class HealthAlertController {

    private final HealthAlertService healthAlertService;

    @PostMapping
    public Result<HealthAlert> create(@RequestBody HealthAlertRequest req) {
        return Result.ok(healthAlertService.create(UserContext.get(), req));
    }

    @GetMapping
    public Result<List<HealthAlert>> list(
            @RequestParam(required = false) Boolean unreadOnly) {
        return Result.ok(healthAlertService.list(UserContext.get(), unreadOnly));
    }

    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        healthAlertService.markRead(UserContext.get(), id);
        return Result.ok();
    }

    @PutMapping("/read-all")
    public Result<Void> markAllRead() {
        healthAlertService.markAllRead(UserContext.get());
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteAlert(@PathVariable Long id) {
        healthAlertService.deleteAlert(UserContext.get(), id);
        return Result.ok();
    }
}
