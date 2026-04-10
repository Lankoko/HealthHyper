package com.example.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.dto.medication.MedicationPlanRequest;
import com.example.demo.dto.medication.MedicationTakingRequest;
import com.example.demo.entity.MedicationLog;
import com.example.demo.entity.MedicationPlan;
import com.example.demo.mapper.MedicationLogMapper;
import com.example.demo.mapper.MedicationPlanMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MedicationService {

    private final MedicationPlanMapper planMapper;
    private final MedicationLogMapper logMapper;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public MedicationPlan addPlan(Long userId, MedicationPlanRequest req) {
        MedicationPlan plan = new MedicationPlan();
        plan.setUserId(userId);
        plan.setDrugName(req.getDrugName());
        plan.setDosage(req.getDosage());
        plan.setFrequency(req.getFrequency());
        plan.setTimeSlots(req.getTimeSlots());
        plan.setStartDate(req.getStartDate() != null ? req.getStartDate() : LocalDate.now());
        plan.setEndDate(req.getEndDate());
        plan.setNotes(req.getNotes());
        plan.setStatus(1);
        plan.setCreatedAt(LocalDateTime.now());
        planMapper.insert(plan);
        return plan;
    }

    public MedicationPlan getLatestPlan(Long userId) {
        return planMapper.selectOne(
                new LambdaQueryWrapper<MedicationPlan>()
                        .eq(MedicationPlan::getUserId, userId)
                        .eq(MedicationPlan::getStatus, 1)
                        .orderByDesc(MedicationPlan::getCreatedAt)
                        .last("LIMIT 1"));
    }

    public MedicationLog recordTaking(Long userId, MedicationTakingRequest req) {
        MedicationPlan plan = planMapper.selectOne(
                new LambdaQueryWrapper<MedicationPlan>()
                        .eq(MedicationPlan::getUserId, userId)
                        .eq(MedicationPlan::getStatus, 1)
                        .like(MedicationPlan::getDrugName, req.getDrugName())
                        .orderByDesc(MedicationPlan::getCreatedAt)
                        .last("LIMIT 1"));

        MedicationLog mlog = new MedicationLog();
        mlog.setPlanId(plan != null ? plan.getId() : 0L);
        mlog.setUserId(userId);
        mlog.setScheduledTime(LocalDateTime.now());
        if (req.getTime() != null && !req.getTime().isBlank()) {
            try {
                mlog.setActualTime(LocalDateTime.parse(req.getTime(), DT_FMT));
            } catch (Exception e) {
                mlog.setActualTime(LocalDateTime.now());
            }
        } else {
            mlog.setActualTime(LocalDateTime.now());
        }
        mlog.setAction("taken");
        mlog.setCreatedAt(LocalDateTime.now());
        logMapper.insert(mlog);
        return mlog;
    }

    public List<Map<String, Object>> getLogs(Long userId, int days) {
        LocalDateTime since = LocalDate.now().minusDays(days).atStartOfDay();
        List<MedicationLog> logs = logMapper.selectList(
                new LambdaQueryWrapper<MedicationLog>()
                        .eq(MedicationLog::getUserId, userId)
                        .ge(MedicationLog::getCreatedAt, since)
                        .orderByDesc(MedicationLog::getCreatedAt));

        Set<Long> planIds = logs.stream()
                .map(MedicationLog::getPlanId).filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        Map<Long, MedicationPlan> planMap = new HashMap<>();
        if (!planIds.isEmpty()) {
            planMapper.selectBatchIds(planIds).forEach(p -> planMap.put(p.getId(), p));
        }

        return logs.stream().map(mlog -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", mlog.getId());
            item.put("planId", mlog.getPlanId());
            item.put("action", mlog.getAction());
            item.put("actualTime", mlog.getActualTime());
            item.put("scheduledTime", mlog.getScheduledTime());
            item.put("createdAt", mlog.getCreatedAt());
            MedicationPlan plan = planMap.get(mlog.getPlanId());
            if (plan != null) {
                item.put("drugName", plan.getDrugName());
                item.put("dosage", plan.getDosage());
                item.put("frequency", plan.getFrequency());
                item.put("timeSlots", plan.getTimeSlots());
            }
            return item;
        }).toList();
    }
}
