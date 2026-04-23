package com.example.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.common.BusinessException;
import com.example.demo.dto.alert.HealthAlertRequest;
import com.example.demo.entity.HealthAlert;
import com.example.demo.mapper.HealthAlertMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthAlertService {

    private final HealthAlertMapper healthAlertMapper;

    public HealthAlert create(Long userId, HealthAlertRequest req) {
        HealthAlert alert = new HealthAlert();
        alert.setUserId(userId);
        alert.setAlertType(req.getAlertType());
        alert.setSeverity(req.getSeverity() != null ? req.getSeverity() : 1);
        alert.setMessage(req.getMessage());
        alert.setVitalSignId(req.getVitalSignId());
        alert.setIsRead(0);
        alert.setCreatedAt(LocalDateTime.now());
        healthAlertMapper.insert(alert);
        return alert;
    }

    public List<HealthAlert> list(Long userId, Boolean unreadOnly) {
        LambdaQueryWrapper<HealthAlert> wrapper = new LambdaQueryWrapper<HealthAlert>()
                .eq(HealthAlert::getUserId, userId)
                .orderByDesc(HealthAlert::getCreatedAt);
        if (Boolean.TRUE.equals(unreadOnly)) {
            wrapper.eq(HealthAlert::getIsRead, 0);
        }
        return healthAlertMapper.selectList(wrapper);
    }

    public void markRead(Long userId, Long alertId) {
        HealthAlert alert = healthAlertMapper.selectById(alertId);
        if (alert == null || !alert.getUserId().equals(userId)) {
            throw new BusinessException("告警不存在");
        }
        alert.setIsRead(1);
        healthAlertMapper.updateById(alert);
    }

    public void markAllRead(Long userId) {
        List<HealthAlert> unread = healthAlertMapper.selectList(
                new LambdaQueryWrapper<HealthAlert>()
                        .eq(HealthAlert::getUserId, userId)
                        .eq(HealthAlert::getIsRead, 0));
        for (HealthAlert alert : unread) {
            alert.setIsRead(1);
            healthAlertMapper.updateById(alert);
        }
    }

    public void deleteAlert(Long userId, Long alertId) {
        HealthAlert alert = healthAlertMapper.selectById(alertId);
        if (alert == null || !alert.getUserId().equals(userId)) {
            throw new BusinessException("告警不存在");
        }
        healthAlertMapper.deleteById(alertId);
    }
}
