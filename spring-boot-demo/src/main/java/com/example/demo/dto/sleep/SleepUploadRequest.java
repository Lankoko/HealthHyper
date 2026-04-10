package com.example.demo.dto.sleep;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SleepUploadRequest {
    private LocalDate sleepDate;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<StageItem> stages;

    @Data
    public static class StageItem {
        private String stage;
        private LocalDateTime startTime;
        private Integer durationSec;
    }
}
