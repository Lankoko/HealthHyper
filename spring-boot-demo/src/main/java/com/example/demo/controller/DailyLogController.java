package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.common.UserContext;
import com.example.demo.dto.daily.DailyLogRequest;
import com.example.demo.entity.DailyLog;
import com.example.demo.service.DailyLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "8. 日常记录", description = "日志记事本，extra_json 不固定（Skill: daily_tracking）")
@RestController
@RequestMapping("/api/daily")
@RequiredArgsConstructor
public class DailyLogController {

    private final DailyLogService dailyLogService;

    @PostMapping
    public Result<DailyLog> addRecord(@RequestBody DailyLogRequest req) {
        return Result.ok(dailyLogService.addRecord(UserContext.get(), req));
    }

    @PutMapping("/{id}")
    public Result<DailyLog> updateRecord(@PathVariable Long id, @RequestBody DailyLogRequest req) {
        return Result.ok(dailyLogService.updateRecord(UserContext.get(), id, req));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteRecord(@PathVariable Long id) {
        dailyLogService.deleteRecord(UserContext.get(), id);
        return Result.ok();
    }

    @GetMapping
    public Result<List<DailyLog>> getRecords(
            @RequestParam(required = false) String logType,
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "50") int limit) {
        return Result.ok(dailyLogService.getRecords(UserContext.get(), logType, days, limit));
    }
}
