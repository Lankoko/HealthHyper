package com.example.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.entity.AiHealthSummary;
import com.example.demo.mapper.AiHealthSummaryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiSummaryService {

    private final AiHealthSummaryMapper aiHealthSummaryMapper;

    /**
     * 保存/更新对话摘要。每个用户只保留一条最新记录（upsert 逻辑）。
     */
    public AiHealthSummary summarize(Long userId, String summary) {
        AiHealthSummary existing = aiHealthSummaryMapper.selectOne(
                new LambdaQueryWrapper<AiHealthSummary>()
                        .eq(AiHealthSummary::getUserId, userId));
        if (existing != null) {
            existing.setSummaryJson(summary);
            existing.setLastAnalyzedAt(LocalDateTime.now());
            existing.setUpdatedAt(LocalDateTime.now());
            aiHealthSummaryMapper.updateById(existing);
            return existing;
        } else {
            AiHealthSummary record = new AiHealthSummary();
            record.setUserId(userId);
            record.setSummaryJson(summary);
            record.setLastAnalyzedAt(LocalDateTime.now());
            record.setUpdatedAt(LocalDateTime.now());
            aiHealthSummaryMapper.insert(record);
            return record;
        }
    }

    /**
     * 获取最新一条摘要数据。
     */
    public AiHealthSummary getLatest(Long userId) {
        return aiHealthSummaryMapper.selectOne(
                new LambdaQueryWrapper<AiHealthSummary>()
                        .eq(AiHealthSummary::getUserId, userId));
    }
}
