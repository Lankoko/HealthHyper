package com.example.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.dto.daily.DailyLogRequest;
import com.example.demo.entity.DailyLog;
import com.example.demo.mapper.DailyLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyLogService {

    private final DailyLogMapper dailyLogMapper;

    public DailyLog addRecord(Long userId, DailyLogRequest req) {
        DailyLog record = new DailyLog();
        record.setUserId(userId);
        record.setLogType(req.getLogType());
        record.setContent(req.getContent());
        record.setExtraJson(req.getExtraJson());
        record.setLogDate(req.getLogDate() != null ? req.getLogDate() : LocalDate.now());
        record.setCreatedAt(LocalDateTime.now());
        dailyLogMapper.insert(record);
        return record;
    }

    public DailyLog updateRecord(Long userId, Long id, DailyLogRequest req) {
        DailyLog record = dailyLogMapper.selectById(id);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new com.example.demo.common.BusinessException("记录不存在");
        }
        if (req.getLogType() != null) record.setLogType(req.getLogType());
        if (req.getContent() != null) record.setContent(req.getContent());
        if (req.getExtraJson() != null) record.setExtraJson(req.getExtraJson());
        if (req.getLogDate() != null) record.setLogDate(req.getLogDate());
        dailyLogMapper.updateById(record);
        return record;
    }

    public void deleteRecord(Long userId, Long id) {
        DailyLog record = dailyLogMapper.selectById(id);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new com.example.demo.common.BusinessException("记录不存在");
        }
        dailyLogMapper.deleteById(id);
    }

    public List<DailyLog> getRecords(Long userId, String logType, int days, int limit) {
        LocalDate since = LocalDate.now().minusDays(days);
        LambdaQueryWrapper<DailyLog> wrapper = new LambdaQueryWrapper<DailyLog>()
                .eq(DailyLog::getUserId, userId)
                .ge(DailyLog::getLogDate, since)
                .orderByDesc(DailyLog::getLogDate);
        if (logType != null && !logType.isBlank()) {
            wrapper.eq(DailyLog::getLogType, logType);
        }
        if (limit > 0) {
            wrapper.last("LIMIT " + limit);
        }
        return dailyLogMapper.selectList(wrapper);
    }
}
