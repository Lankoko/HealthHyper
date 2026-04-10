package com.example.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.dto.vital.VitalSignUploadRequest;
import com.example.demo.entity.HealthAlert;
import com.example.demo.entity.VitalSign;
import com.example.demo.mapper.HealthAlertMapper;
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
public class VitalSignService {

    private final VitalSignMapper vitalSignMapper;
    private final HealthAlertMapper healthAlertMapper;
    private final SystemTriggerService systemTriggerService;

    public VitalSign upload(Long userId, VitalSignUploadRequest req) {
        VitalSign vs = new VitalSign();
        vs.setUserId(userId);
        vs.setDeviceId(req.getDeviceId());
        vs.setHr(req.getHr());
        vs.setSpo2(req.getSpo2());
        vs.setBt(req.getBt());
        vs.setActivity(req.getActivity());
        vs.setSdann(req.getSdann());
        vs.setHrCv(req.getHrCv());
        vs.setFlag(req.getFlag() != null ? req.getFlag() : 0);
        vs.setRecordedAt(req.getRecordedAt() != null ? req.getRecordedAt() : LocalDateTime.now());
        vitalSignMapper.insert(vs);

        if (vs.getFlag() != null && vs.getFlag() > 0) {
            createAlertFromFlag(userId, vs);
        }

        return vs;
    }

    private void createAlertFromFlag(Long userId, VitalSign vs) {
        HealthAlert alert = new HealthAlert();
        alert.setUserId(userId);
        alert.setSeverity(vs.getFlag());
        alert.setVitalSignId(vs.getId());
        alert.setIsRead(0);
        alert.setCreatedAt(LocalDateTime.now());

        StringBuilder msg = new StringBuilder("硬件检测到异常(flag=").append(vs.getFlag()).append(")：");
        if (vs.getHr() != null) msg.append("心率").append(vs.getHr()).append("bpm ");
        if (vs.getSpo2() != null) msg.append("血氧").append(vs.getSpo2()).append("% ");
        if (vs.getBt() != null) msg.append("体温").append(vs.getBt()).append("℃");
        alert.setMessage(msg.toString());

        if (vs.getHr() != null && vs.getHr() > 100) alert.setAlertType("hr_abnormal");
        else if (vs.getSpo2() != null && vs.getSpo2() < 95) alert.setAlertType("spo2_low");
        else alert.setAlertType("vital_abnormal");

        healthAlertMapper.insert(alert);
        log.info("生成异常告警: userId={}, alertType={}, severity={}", userId, alert.getAlertType(), alert.getSeverity());

        if (alert.getSeverity() > 1) {
            systemTriggerService.triggerAlertChat(userId, alert);
        }
    }

    /**
     * 获取最新一条生理数据（支持按 metric 过滤返回字段）。
     */
    public Map<String, Object> getCurrent(Long userId, String metric) {
        VitalSign latest = vitalSignMapper.selectOne(
                new LambdaQueryWrapper<VitalSign>()
                        .eq(VitalSign::getUserId, userId)
                        .orderByDesc(VitalSign::getRecordedAt)
                        .last("LIMIT 1"));
        if (latest == null) {
            return Map.of("message", "暂无生理数据");
        }
        return buildMetricResult(latest, metric);
    }

    /**
     * 获取最近 N 天的统计信息（均值、最大、最小）。
     */
    public Map<String, Object> getStats(Long userId, String metric, int days) {
        LocalDateTime since = LocalDate.now().minusDays(days).atStartOfDay();
        List<VitalSign> records = vitalSignMapper.selectList(
                new LambdaQueryWrapper<VitalSign>()
                        .eq(VitalSign::getUserId, userId)
                        .ge(VitalSign::getRecordedAt, since)
                        .orderByDesc(VitalSign::getRecordedAt));

        if (records.isEmpty()) {
            return Map.of("message", "该时间段内暂无生理数据", "days", days);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("days", days);
        result.put("count", records.size());

        boolean all = "all".equals(metric);
        if (all || "hr".equals(metric)) {
            result.put("hr", computeStats(records.stream()
                    .filter(r -> r.getHr() != null)
                    .map(r -> r.getHr().doubleValue()).toList()));
        }
        if (all || "spo2".equals(metric)) {
            result.put("spo2", computeStats(records.stream()
                    .filter(r -> r.getSpo2() != null)
                    .map(r -> r.getSpo2().doubleValue()).toList()));
        }
        if (all || "bt".equals(metric)) {
            result.put("bt", computeStats(records.stream()
                    .filter(r -> r.getBt() != null)
                    .map(r -> r.getBt().doubleValue()).toList()));
        }
        return result;
    }

    private Map<String, Object> buildMetricResult(VitalSign vs, String metric) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("recorded_at", vs.getRecordedAt());
        boolean all = "all".equals(metric);
        if (all || "hr".equals(metric)) result.put("hr", vs.getHr());
        if (all || "spo2".equals(metric)) result.put("spo2", vs.getSpo2());
        if (all || "bt".equals(metric)) result.put("bt", vs.getBt());
        if (all) {
            result.put("activity", vs.getActivity());
            result.put("sdann", vs.getSdann());
            result.put("hr_cv", vs.getHrCv());
            result.put("flag", vs.getFlag());
        }
        return result;
    }

    private Map<String, Object> computeStats(List<Double> values) {
        if (values.isEmpty()) return Map.of("message", "无数据");
        DoubleSummaryStatistics stats = values.stream()
                .mapToDouble(Double::doubleValue).summaryStatistics();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("avg", Math.round(stats.getAverage() * 10) / 10.0);
        m.put("max", stats.getMax());
        m.put("min", stats.getMin());
        m.put("count", stats.getCount());
        return m;
    }
}
