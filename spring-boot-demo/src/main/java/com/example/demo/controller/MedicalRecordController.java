package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.common.UserContext;
import com.example.demo.dto.medical.MedicalReportRequest;
import com.example.demo.entity.MedicalRecord;
import com.example.demo.service.MedicalRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "9. 医疗报告", description = "报告生成 / 查询，report_type: exam_report | health_analysis（Skill: medical_report_analysis）")
@RestController
@RequestMapping("/api/medical")
@RequiredArgsConstructor
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;

    @PostMapping("/report")
    public Result<MedicalRecord> generateReport(@RequestBody MedicalReportRequest req) {
        return Result.ok(medicalRecordService.generateReport(UserContext.get(), req));
    }

    @DeleteMapping("/records/{id}")
    public Result<Void> deleteRecord(@PathVariable Long id) {
        medicalRecordService.deleteRecord(UserContext.get(), id);
        return Result.ok();
    }

    @GetMapping("/records")
    public Result<List<MedicalRecord>> listRecords(
            @RequestParam(required = false) String recordType,
            @RequestParam(defaultValue = "20") int limit) {
        return Result.ok(medicalRecordService.listRecords(UserContext.get(), recordType, limit));
    }
}
