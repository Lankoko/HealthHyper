package com.example.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.dto.sleep.SleepUploadRequest;
import com.example.demo.entity.HealthPlan;
import com.example.demo.entity.SleepSession;
import com.example.demo.entity.SleepStage;
import com.example.demo.entity.VitalSign;
import com.example.demo.mapper.HealthPlanMapper;
import com.example.demo.mapper.SleepSessionMapper;
import com.example.demo.mapper.SleepStageMapper;
import com.example.demo.mapper.VitalSignMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SleepService {

    private final SleepSessionMapper sleepSessionMapper;
    private final SleepStageMapper sleepStageMapper;
    private final VitalSignMapper vitalSignMapper;
    private final HealthPlanMapper healthPlanMapper;

    /**
     * 上传一晚的睡眠数据（含分期）。
     * 自动从 vital_sign 聚合夜间生理指标。
     */
    public SleepSession uploadSleep(Long userId, SleepUploadRequest req) {
        SleepSession session = new SleepSession();
        session.setUserId(userId);
        session.setSleepDate(req.getSleepDate() != null ? req.getSleepDate() : LocalDate.now());
        session.setStartTime(req.getStartTime());
        session.setEndTime(req.getEndTime());
        session.setCreatedAt(LocalDateTime.now());

        if (req.getStartTime() != null && req.getEndTime() != null) {
            aggregateNightVitals(userId, req.getStartTime(), req.getEndTime(), session);
        }

        sleepSessionMapper.insert(session);

        if (req.getStages() != null) {
            for (SleepUploadRequest.StageItem item : req.getStages()) {
                SleepStage stage = new SleepStage();
                stage.setSessionId(session.getId());
                stage.setStage(item.getStage());
                stage.setStartTime(item.getStartTime());
                stage.setDurationSec(item.getDurationSec());
                sleepStageMapper.insert(stage);
            }
        }
        return session;
    }

    /**
     * 获取最近一晚的睡眠数据（含分期明细）。
     */
    public Map<String, Object> getLatestNight(Long userId) {
        SleepSession session = sleepSessionMapper.selectOne(
                new LambdaQueryWrapper<SleepSession>()
                        .eq(SleepSession::getUserId, userId)
                        .orderByDesc(SleepSession::getSleepDate)
                        .last("LIMIT 1"));
        if (session == null) {
            return Map.of("message", "暂无睡眠数据");
        }
        return buildSleepResult(session);
    }

    /**
     * 获取最近 N 晚的睡眠数据。
     */
    public List<Map<String, Object>> getRecentNights(Long userId, int days) {
        LocalDate since = LocalDate.now().minusDays(days);
        List<SleepSession> sessions = sleepSessionMapper.selectList(
                new LambdaQueryWrapper<SleepSession>()
                        .eq(SleepSession::getUserId, userId)
                        .ge(SleepSession::getSleepDate, since)
                        .orderByDesc(SleepSession::getSleepDate));
        return sessions.stream().map(this::buildSleepResult).toList();
    }

    /**
     * 获取用户最新的睡眠改善计划（从 health_plan 中查询 category 含 sleep 的）。
     */
    public HealthPlan getSleepPlan(Long userId) {
        return healthPlanMapper.selectOne(
                new LambdaQueryWrapper<HealthPlan>()
                        .eq(HealthPlan::getUserId, userId)
                        .eq(HealthPlan::getStatus, 1)
                        .like(HealthPlan::getItemsJson, "sleep")
                        .orderByDesc(HealthPlan::getCreatedAt)
                        .last("LIMIT 1"));
    }

    /**
     * 创建/更新睡眠改善计划。
     */
    public HealthPlan updateSleepPlan(Long userId, String itemsJson) {
        HealthPlan plan = new HealthPlan();
        plan.setUserId(userId);
        plan.setTitle("睡眠改善计划");
        plan.setItemsJson(itemsJson);
        plan.setSource("ai");
        plan.setStatus(1);
        plan.setStartDate(LocalDate.now());
        plan.setCreatedAt(LocalDateTime.now());
        healthPlanMapper.insert(plan);
        return plan;
    }

    public void deleteSleepSession(Long userId, Long sessionId) {
        SleepSession session = sleepSessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new com.example.demo.common.BusinessException("睡眠记录不存在");
        }
        sleepStageMapper.delete(
                new LambdaQueryWrapper<SleepStage>().eq(SleepStage::getSessionId, sessionId));
        sleepSessionMapper.deleteById(sessionId);
    }

    /**
     * AI 回填最新一条睡眠记录的分析结论（无需指定 ID）。
     */
    public SleepSession updateLatestAiAnalysis(Long userId, String aiAnalysis) {
        SleepSession session = sleepSessionMapper.selectOne(
                new LambdaQueryWrapper<SleepSession>()
                        .eq(SleepSession::getUserId, userId)
                        .orderByDesc(SleepSession::getSleepDate)
                        .last("LIMIT 1"));
        if (session == null) {
            throw new com.example.demo.common.BusinessException("暂无睡眠记录");
        }
        session.setAiAnalysis(aiAnalysis);
        sleepSessionMapper.updateById(session);
        return session;
    }

    /**
     * 按 ID 回填指定睡眠记录的分析结论。
     */
    public SleepSession updateAiAnalysis(Long userId, Long sessionId, String aiAnalysis) {
        SleepSession session = sleepSessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new com.example.demo.common.BusinessException("睡眠记录不存在");
        }
        session.setAiAnalysis(aiAnalysis);
        sleepSessionMapper.updateById(session);
        return session;
    }

    private void aggregateNightVitals(Long userId, LocalDateTime start, LocalDateTime end,
                                       SleepSession session) {
        List<VitalSign> vitals = vitalSignMapper.selectList(
                new LambdaQueryWrapper<VitalSign>()
                        .eq(VitalSign::getUserId, userId)
                        .ge(VitalSign::getRecordedAt, start)
                        .le(VitalSign::getRecordedAt, end));
        if (vitals.isEmpty()) return;

        DoubleSummaryStatistics hrStats = vitals.stream()
                .filter(v -> v.getHr() != null).mapToDouble(v -> v.getHr()).summaryStatistics();
        DoubleSummaryStatistics spo2Stats = vitals.stream()
                .filter(v -> v.getSpo2() != null).mapToDouble(v -> v.getSpo2()).summaryStatistics();
        DoubleSummaryStatistics btStats = vitals.stream()
                .filter(v -> v.getBt() != null).mapToDouble(v -> v.getBt().doubleValue()).summaryStatistics();

        if (hrStats.getCount() > 0) {
            session.setNightAvgHr((float) hrStats.getAverage());
            session.setNightMaxHr((float) hrStats.getMax());
            session.setNightMinHr((float) hrStats.getMin());
        }
        if (spo2Stats.getCount() > 0) {
            session.setNightAvgSpo2((float) spo2Stats.getAverage());
            session.setNightMaxSpo2((float) spo2Stats.getMax());
            session.setNightMinSpo2((float) spo2Stats.getMin());
        }
        if (btStats.getCount() > 0) {
            session.setNightAvgBt((float) btStats.getAverage());
            session.setNightMaxBt((float) btStats.getMax());
            session.setNightMinBt((float) btStats.getMin());
        }
    }

    private Map<String, Object> buildSleepResult(SleepSession session) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", session.getId());
        result.put("sleep_date", session.getSleepDate());
        result.put("start_time", session.getStartTime());
        result.put("end_time", session.getEndTime());

        Map<String, Object> hr = new LinkedHashMap<>();
        hr.put("avg", session.getNightAvgHr());
        hr.put("max", session.getNightMaxHr());
        hr.put("min", session.getNightMinHr());
        result.put("heart_rate", hr);

        Map<String, Object> spo2 = new LinkedHashMap<>();
        spo2.put("avg", session.getNightAvgSpo2());
        spo2.put("max", session.getNightMaxSpo2());
        spo2.put("min", session.getNightMinSpo2());
        result.put("spo2", spo2);

        Map<String, Object> bt = new LinkedHashMap<>();
        bt.put("avg", session.getNightAvgBt());
        bt.put("max", session.getNightMaxBt());
        bt.put("min", session.getNightMinBt());
        result.put("body_temp", bt);

        result.put("ai_analysis", session.getAiAnalysis());

        List<SleepStage> stages = sleepStageMapper.selectList(
                new LambdaQueryWrapper<SleepStage>()
                        .eq(SleepStage::getSessionId, session.getId())
                        .orderByAsc(SleepStage::getStartTime));
        List<Map<String, Object>> stageList = new ArrayList<>();
        int totalSec = 0;
        Map<String, Integer> stageDurations = new LinkedHashMap<>();
        for (SleepStage s : stages) {
            Map<String, Object> sm = new LinkedHashMap<>();
            sm.put("stage", s.getStage());
            sm.put("start_time", s.getStartTime());
            sm.put("duration_sec", s.getDurationSec());
            stageList.add(sm);
            totalSec += s.getDurationSec();
            stageDurations.merge(s.getStage(), s.getDurationSec(), Integer::sum);
        }
        result.put("stages", stageList);
        result.put("total_sleep_sec", totalSec);

        Map<String, String> stagePercents = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : stageDurations.entrySet()) {
            double pct = totalSec > 0 ? (e.getValue() * 100.0 / totalSec) : 0;
            stagePercents.put(e.getKey(), String.format("%.1f%%", pct));
        }
        result.put("stage_percentages", stagePercents);

        return result;
    }
}
