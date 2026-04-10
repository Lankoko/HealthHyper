package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.common.UserContext;
import com.example.demo.dto.summary.AiSummaryRequest;
import com.example.demo.entity.AiHealthSummary;
import com.example.demo.service.AiSummaryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "11. AI 健康摘要", description = "对话摘要保存 / 查询（Skill: conversation_summary）")
@RestController
@RequestMapping("/api/ai/summary")
@RequiredArgsConstructor
public class AiSummaryController {

    private final AiSummaryService aiSummaryService;

    @PostMapping
    public Result<AiHealthSummary> summarize(@RequestBody AiSummaryRequest req) {
        return Result.ok(aiSummaryService.summarize(UserContext.get(), req.getSummary()));
    }

    @GetMapping("/latest")
    public Result<AiHealthSummary> getLatest() {
        return Result.ok(aiSummaryService.getLatest(UserContext.get()));
    }
}
