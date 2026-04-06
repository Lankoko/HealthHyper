package com.example.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.dto.profile.ProfileUpdateRequest;
import com.example.demo.entity.HealthProfile;
import com.example.demo.entity.HealthProfileHistory;
import com.example.demo.mapper.HealthProfileHistoryMapper;
import com.example.demo.mapper.HealthProfileMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthProfileService {

    private final HealthProfileMapper profileMapper;
    private final HealthProfileHistoryMapper historyMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public HealthProfile getProfile(Long userId) {
        HealthProfile profile = profileMapper.selectOne(
                new LambdaQueryWrapper<HealthProfile>()
                        .eq(HealthProfile::getUserId, userId));
        if (profile == null) {
            profile = new HealthProfile();
            profile.setUserId(userId);
        }
        return profile;
    }

    public HealthProfile updateProfile(Long userId, ProfileUpdateRequest req, String triggerSource) {
        HealthProfile profile = profileMapper.selectOne(
                new LambdaQueryWrapper<HealthProfile>()
                        .eq(HealthProfile::getUserId, userId));

        boolean isNew = (profile == null);
        if (isNew) {
            profile = new HealthProfile();
            profile.setUserId(userId);
        }

        if (req.getGender() != null) profile.setGender(req.getGender());
        if (req.getBirthDate() != null) profile.setBirthDate(req.getBirthDate());
        if (req.getHeightCm() != null) profile.setHeightCm(req.getHeightCm());
        if (req.getWeightKg() != null) profile.setWeightKg(req.getWeightKg());
        if (req.getBloodType() != null) profile.setBloodType(req.getBloodType());
        if (req.getMedicalHistory() != null) profile.setMedicalHistory(req.getMedicalHistory());
        if (req.getAllergyInfo() != null) profile.setAllergyInfo(req.getAllergyInfo());
        if (req.getLifestyleInfo() != null) profile.setLifestyleInfo(req.getLifestyleInfo());
        if (req.getExtraJson() != null) profile.setExtraJson(req.getExtraJson());
        profile.setUpdatedAt(LocalDateTime.now());

        if (isNew) {
            profileMapper.insert(profile);
        } else {
            profileMapper.updateById(profile);
        }

        saveHistory(userId, profile, triggerSource);
        return profile;
    }

    private void saveHistory(Long userId, HealthProfile profile, String triggerSource) {
        try {
            HealthProfileHistory history = new HealthProfileHistory();
            history.setUserId(userId);
            history.setTriggerSource(triggerSource);
            history.setSnapshotJson(objectMapper.writeValueAsString(profile));
            history.setChangeDesc("用户更新健康档案");
            history.setCreatedAt(LocalDateTime.now());
            historyMapper.insert(history);
        } catch (Exception e) {
            log.warn("保存档案变更历史失败", e);
        }
    }
}
