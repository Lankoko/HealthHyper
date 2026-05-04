package com.example.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.dto.baseline.BaselineUpdateAllRequest;
import com.example.demo.entity.Baseline;
import com.example.demo.mapper.BaselineMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BaselineService {

    private final BaselineMapper baselineMapper;

    public Map<String, Object> getBaseline(Long userId, String metric) {
        Baseline latest = baselineMapper.selectOne(
                new LambdaQueryWrapper<Baseline>()
                        .eq(Baseline::getUserId, userId)
                        .orderByDesc(Baseline::getCreatedAt)
                        .last("LIMIT 1"));
        if (latest == null) {
            return Map.of("message", "暂无基线数据");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("effective_at", latest.getEffectiveAt());
        result.put("created_at", latest.getCreatedAt());
        result.put("source", latest.getSource());

        boolean all = "all".equals(metric);
        if (all || "hr".equals(metric)) result.put("hr_base", latest.getHrBase());
        if (all || "spo2".equals(metric)) result.put("spo2_base", latest.getSpo2Base());
        if (all || "bt".equals(metric)) result.put("bt_base", latest.getBtBase());
        if (all || "hr_cv".equals(metric)) result.put("hr_cv_base", latest.getHrCvBase());
        if (all || "sdann".equals(metric)) result.put("sdann_base", latest.getSdannBase());
        return result;
    }

    public Baseline updateOne(Long userId, String metric, Float value) {
        Baseline bl = getOrCreateLatest(userId, "cloud_ai");
        switch (metric) {
            case "hr" -> bl.setHrBase(value);
            case "spo2" -> bl.setSpo2Base(value);
            case "bt" -> bl.setBtBase(value);
            case "hr_cv" -> bl.setHrCvBase(value);
            case "sdann" -> bl.setSdannBase(value);
            default -> throw new IllegalArgumentException("不支持的指标: " + metric);
        }
        bl.setEffectiveAt(LocalDateTime.now());
        if (bl.getId() == null) {
            bl.setCreatedAt(LocalDateTime.now());
            baselineMapper.insert(bl);
        } else {
            baselineMapper.updateById(bl);
        }
        return bl;
    }

    public Baseline updateAll(Long userId, BaselineUpdateAllRequest req) {
        Baseline bl = new Baseline();
        bl.setUserId(userId);
        bl.setSource("cloud_ai");
        bl.setHrBase(req.getHrBase());
        bl.setSpo2Base(req.getSpo2Base());
        bl.setBtBase(req.getBtBase());
        bl.setHrCvBase(req.getHrCvBase());
        bl.setSdannBase(req.getSdannBase());
        bl.setEffectiveAt(LocalDateTime.now());
        bl.setCreatedAt(LocalDateTime.now());
        baselineMapper.insert(bl);
        return bl;
    }

    /**
     * 设备影子同步基线：只更新影子中存在的字段，缺失字段保留原值。
     * source 标记为 "device"，与 AI 更新的 "cloud_ai" 来源区分。
     */
    public void updateFromDevice(Long userId, Float hrBase, Float spo2Base,
                                 Float btBase, Float hrCvBase, Float sdannBase) {
        if (hrBase == null && spo2Base == null && btBase == null
                && hrCvBase == null && sdannBase == null) return;

        Baseline bl = getOrCreateLatest(userId, "device");
        if (hrBase != null) bl.setHrBase(hrBase);
        if (spo2Base != null) bl.setSpo2Base(spo2Base);
        if (btBase != null) bl.setBtBase(btBase);
        if (hrCvBase != null) bl.setHrCvBase(hrCvBase);
        if (sdannBase != null) bl.setSdannBase(sdannBase);
        bl.setEffectiveAt(LocalDateTime.now());
        if (bl.getId() == null) {
            bl.setCreatedAt(LocalDateTime.now());
            baselineMapper.insert(bl);
        } else {
            baselineMapper.updateById(bl);
        }
    }

    private Baseline getOrCreateLatest(Long userId, String source) {
        Baseline latest = baselineMapper.selectOne(
                new LambdaQueryWrapper<Baseline>()
                        .eq(Baseline::getUserId, userId)
                        .orderByDesc(Baseline::getCreatedAt)
                        .last("LIMIT 1"));
        if (latest != null) return latest;
        Baseline bl = new Baseline();
        bl.setUserId(userId);
        bl.setSource(source);
        return bl;
    }
}
