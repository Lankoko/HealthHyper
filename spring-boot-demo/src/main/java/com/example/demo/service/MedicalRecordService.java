package com.example.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.dto.medical.MedicalReportRequest;
import com.example.demo.entity.MedicalRecord;
import com.example.demo.mapper.MedicalRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MedicalRecordService {

    private final MedicalRecordMapper medicalRecordMapper;

    public MedicalRecord generateReport(Long userId, MedicalReportRequest req) {
        MedicalRecord record = new MedicalRecord();
        record.setUserId(userId);
        record.setRecordType(req.getReportType());
        record.setTitle(req.getReportTitle());
        record.setContent(req.getReportContent());
        record.setSource("ai_extract");
        record.setChatSessionId(req.getChatSessionId());
        record.setRecordDate(req.getRecordDate() != null ? req.getRecordDate() : LocalDate.now());
        record.setCreatedAt(LocalDateTime.now());
        medicalRecordMapper.insert(record);
        return record;
    }

    public void deleteRecord(Long userId, Long id) {
        MedicalRecord record = medicalRecordMapper.selectById(id);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new com.example.demo.common.BusinessException("报告不存在");
        }
        medicalRecordMapper.deleteById(id);
    }

    public List<MedicalRecord> listRecords(Long userId, String recordType, int limit) {
        LambdaQueryWrapper<MedicalRecord> wrapper = new LambdaQueryWrapper<MedicalRecord>()
                .eq(MedicalRecord::getUserId, userId)
                .orderByDesc(MedicalRecord::getCreatedAt);
        if (recordType != null && !recordType.isBlank()) {
            wrapper.eq(MedicalRecord::getRecordType, recordType);
        }
        if (limit > 0) {
            wrapper.last("LIMIT " + limit);
        }
        return medicalRecordMapper.selectList(wrapper);
    }
}
