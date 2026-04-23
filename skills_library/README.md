# AI Tool → 中台 API 对接文档

> 本文档供 AI 队友参考，说明各 Skill Tool 对应的中台 HTTP 接口。
> SKILL.md 和 tools.json 由 AI 端维护，本文档只描述中台侧的接口规范。

---

## 认证方式

所有请求必须携带以下两个 Header：

```
X-Api-Key: HealthHyperAiToolKey2026
X-User-Id: <当前用户ID>
```

## 基础信息

- 中台地址：`http://<中台IP>:8080`
- 统一响应格式：`{ "code": 200, "message": "ok", "data": ... }`
- 请求体字段命名：**camelCase**（如 `drugName`、`reportType`）

---

## Skill → 接口映射

### user_profile（用户档案）

| Tool | 方法 | 路径 | 参数 |
|------|------|------|------|
| `get_user_profile` | GET | `/api/user/profile` | 无 |
| `update_user_profile` | PUT | `/api/user/profile` | body: `{ gender, birthDate, heightCm, weightKg, bloodType, medicalHistory, allergyInfo, lifestyleInfo }` |

> 档案更新后中台会自动触发摘要更新（AI 会收到一条 system_summary 对话）

### physiological_analysis（生理分析）

| Tool | 方法 | 路径 | 参数 |
|------|------|------|------|
| `get_current_physiological` | GET | `/api/vital/current` | `?metric=all\|hr\|spo2\|bt` |
| `get_physiological_stats` | GET | `/api/vital/stats` | `?metric=all\|hr\|spo2\|bt&days=7` |

### physiological_baseline（基线）

| Tool | 方法 | 路径 | 参数 |
|------|------|------|------|
| `get_baseline` | GET | `/api/baseline` | `?metric=all\|hr\|spo2\|bt\|hr_cv\|sdann` |
| `update_one_baseline` | PUT | `/api/baseline` | body: `{ "metric": "hr", "value": 72.5 }` |
| `update_all_baseline` | PUT | `/api/baseline/all` | body: `{ hrBase, spo2Base, btBase, hrCvBase, sdannBase }` |

### sleep_analysis（睡眠）

| Tool | 方法 | 路径 | 参数 |
|------|------|------|------|
| `obtain_single_night_sleep` | GET | `/api/sleep/latest` | 无 |
| `obtain_multi_night_sleep` | GET | `/api/sleep/recent` | `?days=7` |
| `update_sleep_analysis` | PUT | `/api/sleep/latest/analysis` | body: `{ "aiAnalysis": "分析结论文本" }`（自动回填最新记录） |
| `update_sleep_analysis`（指定 ID） | PUT | `/api/sleep/{id}/analysis` | body: `{ "aiAnalysis": "分析结论文本" }` |
| `get_sleep_plan` | GET | `/api/sleep/plan` | 无 |
| `update_sleep_plan` | PUT | `/api/sleep/plan` | body: `{ "items_json": "[...]" }` |

### medication_assistant（用药助手）

| Tool | 方法 | 路径 | 参数 |
|------|------|------|------|
| `add_medication_plan` | POST | `/api/medication/plan` | body: `{ drugName, dosage, frequency, timeSlots, notes }`（startDate/endDate 可不传，默认今天起） |
| `obtain_medication_plan` | GET | `/api/medication/plan/list` | 无（返回全部有效计划列表） |
| `record_medication_taking` | POST | `/api/medication/log` | body: `{ "drugName": "药品名", "time": "2026-04-09 08:15:00" }` |
| `obtain_medication_log` | GET | `/api/medication/log` | `?days=3`（返回含药品名、剂量等计划信息） |

### daily_tracking（日常记录）

| Tool | 方法 | 路径 | 参数 |
|------|------|------|------|
| `add_daily_record` | POST | `/api/daily` | body: `{ logType, content, extraJson }` |
| `get_daily_records` | GET | `/api/daily` | `?logType=symptom&days=7&limit=20` |

> `daily_log` 是纯日志记事本，`extra_json` 是不固定的扩展字段。打卡请用 health_plan。

### medical_report_analysis（报告分析）

| Tool | 方法 | 路径 | 参数 |
|------|------|------|------|
| `generate_medical_report` | POST | `/api/medical/report` | body: `{ reportType, reportTitle, reportContent, chatSessionId }` |

> **report_type 约定**：`exam_report`（体检报告）、`health_analysis`（健康分析报告）

### health_planning（健康计划）

| Tool | 方法 | 路径 | 参数 |
|------|------|------|------|
| `create_health_plan` | POST | `/api/plan` | body: `{ title, content, days, source }` |
| `get_health_plans` | GET | `/api/plan` | `?days=3` |
| `checkin_health_plan` | PUT | `/api/plan/{id}/checkin` | 无 body（打卡 status→1） |

> status: 0=未完成/待打卡，1=已完成/已打卡。用户或 AI 都可以创建和打卡。

### conversation_summary（对话摘要）

| Tool | 方法 | 路径 | 参数 |
|------|------|------|------|
| `summarize_conversation` | POST | `/api/ai/summary` | body: `{ "summary": "JSON字符串" }` |
| `get_conversation_memory` | GET | `/api/ai/summary/latest` | 无 |

### weather（天气）

天气和空气质量工具不经中台，直接调外部 API。

---

## 联调验证

```bash
curl -s http://localhost:8080/api/user/profile \
  -H "X-Api-Key: HealthHyperAiToolKey2026" \
  -H "X-User-Id: 3" | python -m json.tool
```
