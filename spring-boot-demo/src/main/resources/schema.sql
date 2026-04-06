CREATE DATABASE IF NOT EXISTS health_hyper DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_general_ci;
USE health_hyper;

-- 1. 用户表
CREATE TABLE IF NOT EXISTS `sys_user` (
    `id`            BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username`      VARCHAR(50)  NOT NULL UNIQUE,
    `password_hash` VARCHAR(255) NOT NULL,
    `nickname`      VARCHAR(50),
    `avatar_url`    VARCHAR(255),
    `phone`         VARCHAR(20),
    `created_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 健康档案
CREATE TABLE IF NOT EXISTS `health_profile` (
    `id`              BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id`         BIGINT NOT NULL UNIQUE,
    `gender`          TINYINT,
    `birth_date`      DATE,
    `height_cm`       DECIMAL(5,1),
    `weight_kg`       DECIMAL(5,1),
    `blood_type`      VARCHAR(10),
    `medical_history` TEXT,
    `allergy_info`    TEXT,
    `lifestyle_info`  TEXT,
    `extra_json`      JSON,
    `updated_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. 健康档案变更历史
CREATE TABLE IF NOT EXISTS `health_profile_history` (
    `id`             BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id`        BIGINT      NOT NULL,
    `trigger_source` VARCHAR(50) NOT NULL,
    `snapshot_json`  JSON,
    `change_desc`    VARCHAR(255),
    `created_at`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. 设备表
CREATE TABLE IF NOT EXISTS `device` (
    `id`          BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id`     BIGINT      NOT NULL,
    `device_sn`   VARCHAR(64) NOT NULL,
    `device_name` VARCHAR(100),
    `status`      TINYINT NOT NULL DEFAULT 1,
    `bound_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. 实时生理数据
CREATE TABLE IF NOT EXISTS `vital_sign` (
    `id`          BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id`     BIGINT NOT NULL,
    `device_id`   BIGINT,
    `hr`          SMALLINT,
    `spo2`        SMALLINT,
    `bt`          DECIMAL(4,1),
    `activity`    SMALLINT,
    `sdann`       FLOAT,
    `hr_cv`       FLOAT,
    `flag`        TINYINT NOT NULL DEFAULT 0,
    `recorded_at` DATETIME NOT NULL,
    INDEX `idx_user_recorded` (`user_id`, `recorded_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. 生理基线
CREATE TABLE IF NOT EXISTS `baseline` (
    `id`           BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id`      BIGINT      NOT NULL,
    `source`       VARCHAR(20) NOT NULL,
    `hr_base`      FLOAT,
    `spo2_base`    FLOAT,
    `bt_base`      FLOAT,
    `hr_cv_base`   FLOAT,
    `sdann_base`   FLOAT,
    `effective_at` DATETIME NOT NULL,
    `created_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7. 睡眠会话
CREATE TABLE IF NOT EXISTS `sleep_session` (
    `id`             BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id`        BIGINT NOT NULL,
    `sleep_date`     DATE   NOT NULL,
    `start_time`     DATETIME,
    `end_time`       DATETIME,
    `night_avg_hr`   FLOAT,
    `night_max_hr`   FLOAT,
    `night_min_hr`   FLOAT,
    `night_avg_spo2` FLOAT,
    `night_max_spo2` FLOAT,
    `night_min_spo2` FLOAT,
    `night_avg_bt`   FLOAT,
    `night_max_bt`   FLOAT,
    `night_min_bt`   FLOAT,
    `ai_analysis`    TEXT,
    `created_at`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user_date` (`user_id`, `sleep_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 8. 睡眠分期明细
CREATE TABLE IF NOT EXISTS `sleep_stage` (
    `id`           BIGINT AUTO_INCREMENT PRIMARY KEY,
    `session_id`   BIGINT      NOT NULL,
    `stage`        VARCHAR(10) NOT NULL,
    `start_time`   DATETIME    NOT NULL,
    `duration_sec` INT         NOT NULL,
    INDEX `idx_session_id` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 9. 用药计划
CREATE TABLE IF NOT EXISTS `medication_plan` (
    `id`         BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id`    BIGINT       NOT NULL,
    `drug_name`  VARCHAR(100) NOT NULL,
    `dosage`     VARCHAR(50),
    `frequency`  VARCHAR(50),
    `time_slots` VARCHAR(255),
    `start_date` DATE NOT NULL,
    `end_date`   DATE,
    `notes`      VARCHAR(255),
    `status`     TINYINT NOT NULL DEFAULT 1,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 10. 用药记录
CREATE TABLE IF NOT EXISTS `medication_log` (
    `id`             BIGINT AUTO_INCREMENT PRIMARY KEY,
    `plan_id`        BIGINT      NOT NULL,
    `user_id`        BIGINT      NOT NULL,
    `scheduled_time` DATETIME    NOT NULL,
    `actual_time`    DATETIME,
    `action`         VARCHAR(20) NOT NULL,
    `created_at`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_plan_id` (`plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 11. 医疗记录
CREATE TABLE IF NOT EXISTS `medical_record` (
    `id`              BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id`         BIGINT      NOT NULL,
    `record_type`     VARCHAR(30) NOT NULL,
    `title`           VARCHAR(200),
    `content`         TEXT,
    `source`          VARCHAR(20) NOT NULL DEFAULT 'manual',
    `chat_session_id` BIGINT,
    `record_date`     DATE,
    `created_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 12. 日常记录
CREATE TABLE IF NOT EXISTS `daily_log` (
    `id`         BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id`    BIGINT      NOT NULL,
    `log_type`   VARCHAR(20) NOT NULL,
    `content`    TEXT,
    `extra_json` JSON,
    `log_date`   DATE NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user_date` (`user_id`, `log_date`),
    INDEX `idx_user_type` (`user_id`, `log_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 13. 健康计划
CREATE TABLE IF NOT EXISTS `health_plan` (
    `id`              BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id`         BIGINT       NOT NULL,
    `title`           VARCHAR(100) NOT NULL,
    `items_json`      JSON         NOT NULL,
    `source`          VARCHAR(20)  NOT NULL DEFAULT 'ai',
    `chat_session_id` BIGINT,
    `status`          TINYINT NOT NULL DEFAULT 1,
    `start_date`      DATE,
    `end_date`        DATE,
    `created_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 14. AI 对话会话
CREATE TABLE IF NOT EXISTS `chat_session` (
    `id`         BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id`    BIGINT      NOT NULL,
    `title`      VARCHAR(100),
    `source`     VARCHAR(20) NOT NULL DEFAULT 'user',
    `is_read`    TINYINT NOT NULL DEFAULT 1,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 15. AI 对话消息
CREATE TABLE IF NOT EXISTS `chat_message` (
    `id`         BIGINT AUTO_INCREMENT PRIMARY KEY,
    `session_id` BIGINT      NOT NULL,
    `role`       VARCHAR(10) NOT NULL,
    `msg_type`   VARCHAR(20) NOT NULL DEFAULT 'text',
    `content`    TEXT,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_session_id` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 16. 异常告警
CREATE TABLE IF NOT EXISTS `health_alert` (
    `id`                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id`              BIGINT      NOT NULL,
    `alert_type`           VARCHAR(30) NOT NULL,
    `severity`             TINYINT NOT NULL DEFAULT 1,
    `message`              VARCHAR(500),
    `vital_sign_id`        BIGINT,
    `triggered_session_id` BIGINT,
    `is_read`              TINYINT NOT NULL DEFAULT 0,
    `created_at`           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 17. AI 健康摘要
CREATE TABLE IF NOT EXISTS `ai_health_summary` (
    `id`               BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id`          BIGINT NOT NULL UNIQUE,
    `summary_json`     JSON,
    `last_analyzed_at` DATETIME,
    `updated_at`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
