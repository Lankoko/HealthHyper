package com.example.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.dto.plan.HealthPlanRequest;
import com.example.demo.entity.HealthPlan;
import com.example.demo.mapper.HealthPlanMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthPlanService {

    private final HealthPlanMapper healthPlanMapper;

    public HealthPlan createPlan(Long userId, HealthPlanRequest req) {
        HealthPlan plan = new HealthPlan();
        plan.setUserId(userId);
        plan.setTitle(req.getTitle());
        plan.setItemsJson(req.getContent());
        plan.setSource(req.getSource() != null ? req.getSource() : "ai");
        plan.setChatSessionId(req.getChatSessionId());
        plan.setStatus(0);
        plan.setStartDate(LocalDate.now());
        if (req.getDays() != null && req.getDays() > 0) {
            plan.setEndDate(LocalDate.now().plusDays(req.getDays()));
        }
        plan.setCreatedAt(LocalDateTime.now());
        healthPlanMapper.insert(plan);
        return plan;
    }

    /**
     * 打卡：将计划 status 从 0(未完成) 切换到 1(已完成)。
     */
    public HealthPlan checkin(Long userId, Long planId) {
        HealthPlan plan = healthPlanMapper.selectById(planId);
        if (plan == null || !plan.getUserId().equals(userId)) {
            throw new com.example.demo.common.BusinessException("计划不存在");
        }
        plan.setStatus(1);
        healthPlanMapper.updateById(plan);
        return plan;
    }

    /**
     * 取消打卡 / 重置状态。
     */
    public HealthPlan uncheckin(Long userId, Long planId) {
        HealthPlan plan = healthPlanMapper.selectById(planId);
        if (plan == null || !plan.getUserId().equals(userId)) {
            throw new com.example.demo.common.BusinessException("计划不存在");
        }
        plan.setStatus(0);
        healthPlanMapper.updateById(plan);
        return plan;
    }

    public List<HealthPlan> getPlans(Long userId, int days) {
        LocalDateTime since = LocalDate.now().minusDays(days).atStartOfDay();
        return healthPlanMapper.selectList(
                new LambdaQueryWrapper<HealthPlan>()
                        .eq(HealthPlan::getUserId, userId)
                        .ge(HealthPlan::getCreatedAt, since)
                        .orderByDesc(HealthPlan::getCreatedAt));
    }

    public HealthPlan getLatest(Long userId) {
        return healthPlanMapper.selectOne(
                new LambdaQueryWrapper<HealthPlan>()
                        .eq(HealthPlan::getUserId, userId)
                        .eq(HealthPlan::getStatus, 0)
                        .orderByDesc(HealthPlan::getCreatedAt)
                        .last("LIMIT 1"));
    }
}
