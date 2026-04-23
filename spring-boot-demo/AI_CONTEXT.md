# HealthHyper 中台 — AI 开发上下文文档

> 本文档供 AI 助手阅读，用于快速理解项目现状。人类开发者也可参考。
> 详细功能设计、数据库字段说明、数据流定义见 `中台设计文档.md`（项目根目录）。

---

## 1. 项目概况

**项目**：基于 AI 的个人健康管家系统（3 人毕业设计大赛）

**架构**：`硬件(MCU) --蓝牙--> 手机(鸿蒙) --HTTP--> 中台(Spring Boot) --HTTP--> 云AI(LangChain)`

**本仓库**：中台部分，负责数据枢纽 + 业务引擎。

| 角色 | 技术 | 负责人 |
|------|------|--------|
| 手机端 | HarmonyOS, MindSpore Lite | 队友A |
| **中台** | **Spring Boot 3.3.2, Java 17, MySQL, MyBatis-Plus, JWT** | **本人** |
| 云 AI | LangChain, RAG, 大模型 | 队友B |

---

## 2. 技术栈 & 关键配置

| 项 | 值 |
|----|----|
| Spring Boot | 3.3.2 |
| Java | 17 |
| ORM | MyBatis-Plus 3.5.7 |
| 数据库 | MySQL 8，库名 `health_hyper` |
| 认证 | JWT (jjwt 0.12.6)，密码用 jbcrypt |
| 构建 | Maven (mvnw) |
| 配置文件 | `src/main/resources/application.yml` |
| 建表脚本 | `src/main/resources/schema.sql`（17 张表） |
| 端口 | 8080 |

**`application.yml` 中的关键自定义配置**：
```yaml
app:
  jwt:
    secret: HealthHyperJwtSecretKey2026ForDemoProjectX
    expiration-hours: 72
  ai:
    base-url: <云AI的地址>           # 当前为队友的 cloudstudio URL
    mock-enabled: false              # true=不调真AI用本地mock，false=调真实云AI
    api-key: HealthHyperAiToolKey2026  # AI端调用中台接口的API Key
```

---

## 3. 认证机制（双重认证）

中台支持两种认证方式，所有 `/api/**`（除 `/api/auth/**`）均需认证：

### 3.1 手机端 — JWT Token
```
Header: Authorization: Bearer <jwt_token>
```
通过 `JwtInterceptor` 从 token 中提取 `userId`，写入 `UserContext`。

### 3.2 AI 端 — API Key
```
Header: X-Api-Key: HealthHyperAiToolKey2026
Header: X-User-Id: <目标用户ID>
```
AI 的 LangChain Tool 调用中台接口时使用此方式。`JwtInterceptor` 验证 API Key 后，从 `X-User-Id` 提取用户 ID 写入 `UserContext`。

---

## 4. 代码结构

```
com.example.demo/
├── SpringBootDemoApplication.java   # 启动类，@MapperScan("com.example.demo.mapper")
├── common/
│   ├── Result.java                  # 统一返回 { code, message, data }
│   ├── BusinessException.java       # 业务异常，code + message
│   ├── GlobalExceptionHandler.java  # @RestControllerAdvice 全局异常处理
│   ├── JwtUtil.java                 # JWT 签发/校验/提取 userId
│   └── UserContext.java             # ThreadLocal<Long> 存当前登录用户 ID
├── config/
│   ├── JwtInterceptor.java          # 拦截 /api/** 校验 Bearer token 或 API Key
│   └── WebMvcConfig.java            # 注册拦截器 + CORS 放行
├── controller/
│   ├── AuthController.java          # /api/auth/register, /api/auth/login
│   ├── ChatController.java          # /api/chat/sessions/*, SSE 流式端点
│   ├── HealthProfileController.java # /api/user/profile GET/PUT
│   ├── VitalSignController.java     # /api/vital/*
│   ├── BaselineController.java      # /api/baseline/*
│   ├── SleepController.java         # /api/sleep/*
│   ├── MedicationController.java    # /api/medication/*
│   ├── DailyLogController.java      # /api/daily
│   ├── MedicalRecordController.java # /api/medical/*
│   ├── HealthPlanController.java    # /api/plan
│   ├── AiSummaryController.java     # /api/ai/summary
│   └── HealthAlertController.java   # /api/alert
├── dto/
│   ├── auth/     LoginRequest, RegisterRequest
│   ├── chat/     ChatSendRequest
│   ├── profile/  ProfileUpdateRequest
│   ├── vital/    VitalSignUploadRequest
│   ├── baseline/ BaselineUpdateRequest, BaselineUpdateAllRequest
│   ├── sleep/    SleepUploadRequest
│   ├── medication/ MedicationPlanRequest, MedicationTakingRequest
│   ├── daily/    DailyLogRequest
│   ├── medical/  MedicalReportRequest
│   ├── plan/     HealthPlanRequest
│   ├── summary/  AiSummaryRequest
│   └── alert/    HealthAlertRequest
├── entity/       User, HealthProfile, HealthProfileHistory, ChatSession,
│                 ChatMessage, HealthPlan, AiHealthSummary,
│                 VitalSign, Baseline, SleepSession, SleepStage,
│                 MedicationPlan, MedicationLog, MedicalRecord,
│                 DailyLog, HealthAlert, Device  (共 17 个实体)
├── mapper/       每个实体对应一个 extends BaseMapper<T> 的 Mapper 接口
└── service/
    ├── AuthService.java             # 注册/登录
    ├── HealthProfileService.java    # 档案CRUD + 变更历史
    ├── AiService.java               # 调云AI的HTTP客户端（含mock模式）
    ├── ChatService.java             # 会话管理 + SSE 流式中转 + ACTIONS（遗留兼容）
    ├── SystemTriggerService.java    # 中台主动触发 AI（档案变更→摘要更新, 异常→AI主动对话）
    ├── VitalSignService.java        # 生理数据上报/查询/统计 + flag→告警自动生成
    ├── BaselineService.java         # 基线查询/更新
    ├── SleepService.java            # 睡眠数据上传/查询 + 夜间聚合 + 睡眠计划
    ├── MedicationService.java       # 用药计划/服药记录
    ├── DailyLogService.java         # 日常记录CRUD
    ├── MedicalRecordService.java    # 医疗报告/记录
    ├── HealthPlanService.java       # 健康计划CRUD
    ├── AiSummaryService.java        # AI健康摘要/对话记忆
    └── HealthAlertService.java      # 异常告警CRUD
```

---

## 5. 全部 API 端点

### 5.1 认证（无需 token）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/register` | 注册，返回 token + userId |
| POST | `/api/auth/login` | 登录，返回 token + userId |

### 5.2 AI 对话

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/chat/sessions` | 创建对话会话 |
| GET | `/api/chat/sessions` | 会话列表 |
| GET | `/api/chat/sessions/{id}/messages` | 消息历史 |
| POST | `/api/chat/sessions/{id}/send` | **SSE 流式**发消息 |

### 5.3 用户档案（对应 Skill: user_profile）

| 方法 | 路径 | Tool 名 | 说明 |
|------|------|---------|------|
| GET | `/api/user/profile` | `get_user_profile` | 获取健康档案 |
| PUT | `/api/user/profile` | `update_user_profile` | 更新健康档案 |

### 5.4 实时生理数据（对应 Skill: physiological_analysis）

| 方法 | 路径 | Tool 名 | 说明 |
|------|------|---------|------|
| POST | `/api/vital/upload` | — | 上报生理数据（手机端） |
| GET | `/api/vital/current?metric=` | `get_current_physiological` | 获取最新生理数据 |
| GET | `/api/vital/stats?metric=&days=` | `get_physiological_stats` | 获取统计信息 |

### 5.5 生理基线（对应 Skill: physiological_baseline）

| 方法 | 路径 | Tool 名 | 说明 |
|------|------|---------|------|
| GET | `/api/baseline?metric=` | `get_baseline` | 获取基线 |
| PUT | `/api/baseline` | `update_one_baseline` | 更新单个指标基线 |
| PUT | `/api/baseline/all` | `update_all_baseline` | 更新全部基线 |

### 5.6 睡眠（对应 Skill: sleep_analysis）

| 方法 | 路径 | Tool 名 | 说明 |
|------|------|---------|------|
| POST | `/api/sleep/upload` | — | 上传睡眠数据（手机端），自动聚合夜间生理指标 |
| GET | `/api/sleep/latest` | `obtain_single_night_sleep` | 最近一晚睡眠 |
| GET | `/api/sleep/recent?days=` | `obtain_multi_night_sleep` | 最近N晚 |
| PUT | `/api/sleep/latest/analysis` | `update_sleep_analysis` | AI 回填最新睡眠记录的分析结论（推荐） |
| PUT | `/api/sleep/{id}/analysis` | `update_sleep_analysis` | 按 ID 回填指定记录的分析结论 |
| GET | `/api/sleep/plan` | `get_sleep_plan` | 获取睡眠计划 |
| PUT | `/api/sleep/plan` | `update_sleep_plan` | 更新睡眠计划 |

### 5.7 用药管理（对应 Skill: medication_assistant）

| 方法 | 路径 | Tool 名 | 说明 |
|------|------|---------|------|
| POST | `/api/medication/plan` | `add_medication_plan` | 创建用药计划（startDate/endDate 可不传，默认今天起） |
| GET | `/api/medication/plan/list` | `obtain_medication_plan` | 获取全部有效用药计划 |
| POST | `/api/medication/log` | `record_medication_taking` | 记录服药 |
| GET | `/api/medication/log?days=` | `obtain_medication_log` | 最近服药记录（**含药品名等计划信息**） |

### 5.8 日常记录（对应 Skill: daily_tracking）

| 方法 | 路径 | Tool 名 | 说明 |
|------|------|---------|------|
| POST | `/api/daily` | `add_daily_record` | 添加日常记录 |
| GET | `/api/daily?logType=&days=&limit=` | `get_daily_records` | 查询记录 |

### 5.9 医疗记录（对应 Skill: medical_report_analysis）

| 方法 | 路径 | Tool 名 | 说明 |
|------|------|---------|------|
| POST | `/api/medical/report` | `generate_medical_report` | 保存医疗报告 |
| GET | `/api/medical/records?recordType=&limit=` | — | 查询医疗记录 |

> **report_type 约定**：`exam_report`（体检/检查报告）、`health_analysis`（健康分析报告）。手机端可按类型筛选。

### 5.10 健康计划（对应 Skill: health_planning）

| 方法 | 路径 | Tool 名 | 说明 |
|------|------|---------|------|
| POST | `/api/plan` | `create_health_plan` | 创建计划（status=0 待打卡） |
| GET | `/api/plan?days=` | `get_health_plans` | 最近计划列表 |
| GET | `/api/plan/latest` | — | 最新未完成计划 |
| PUT | `/api/plan/{id}/checkin` | `checkin_health_plan` | 打卡（status→1） |
| PUT | `/api/plan/{id}/uncheckin` | — | 取消打卡（status→0） |

> **status 语义**：`0`=待完成/未打卡，`1`=已完成/已打卡。`daily_log` 仅作日志记事，不再与打卡关联。

### 5.11 AI 健康摘要（对应 Skill: conversation_summary）

| 方法 | 路径 | Tool 名 | 说明 |
|------|------|---------|------|
| POST | `/api/ai/summary` | `summarize_conversation` | 保存对话摘要 |
| GET | `/api/ai/summary/latest` | `get_conversation_memory` | 获取最新摘要 |

### 5.12 异常告警

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/alert` | 创建告警 |
| GET | `/api/alert?unreadOnly=` | 告警列表 |
| PUT | `/api/alert/{id}/read` | 标记已读 |

---

## 6. 云 AI 集成协议（已联调通过）

### 6.1 中台 → 云 AI

```
POST {base-url}/api/v1/chat/stream
Content-Type: application/json

{
  "user_id":   "3",                            // 中台用户 ID（字符串）
  "thread_id": "4",                            // 中台 chat_session.id（字符串，全局唯一）
  "text":      "用户的消息",                     // 用户输入文本
  "image_url": "data:image/jpeg;base64,..."    // 可选，无图时整个字段不传
}
```

### 6.2 云 AI → 中台（SSE 流式响应）

```
event: status           ← 状态行，中台跳过
data: {"content":"你"}   ← 内容片段，中台透传给手机
data: {"content":"好"}   ← 逐段推送
data: {}                ← 结束标志，中台停止透传
```

### 6.3 AI 侧记忆

AI 侧自己维护多用户多标签页（thread）的对话记忆，**中台不需要拼历史消息**。中台只传用户原始消息文本。

### 6.4 Mock 模式

`app.ai.mock-enabled: true` 时 `AiService` 不发 HTTP 请求，本地模拟流式输出。
消息含"计划"二字时模拟带 `---ACTIONS---` 的回复，用于测试计划自动创建流程。

---

## 7. ACTIONS 机制（⚠️ 已废弃，遗留兼容）

> **AI 已切换为 Skill Tool 直接调中台 API**，不再需要在回复文本中嵌入 `---ACTIONS---` 指令。
> 代码仍保留解析逻辑（`ChatService.processActions()`）作为兼容，但不再主动使用。

---

## 7.5 中台主动触发 AI 机制

中台在以下两种场景会自动创建对话会话并向 AI 发送消息（`SystemTriggerService`）：

### 触发机制 1：档案变更 → 摘要更新
- **触发条件**：`health_profile` 任何字段被更新时
- **流程**：中台将完整档案快照包装为消息 → 创建 source=`system_summary` 的会话 → 发送给 AI
- **AI 预期行为**：使用 `conversation_summary` skill 更新健康摘要

### 触发机制 2：异常告警 → AI 主动对话
- **触发条件**：上报的 `vital_sign.flag > 0` 时自动生成 `health_alert`（severity = flag 值），当 **severity > 1** 时触发
- **流程**：中台创建 source=`system_alert`、is_read=0 的会话 → 发送异常详情给 AI → AI 回复后存为 assistant 消息
- **用户体验**：手机端看到一个"未读"会话，打开后就像 AI 主动关心用户
- **AI 预期行为**：分析异常数据，给出健康建议，主动关心用户

---

## 8. AI Tool 调用中台 API 示例

AI 队友的 LangChain Agent 通过 HTTP 调用中台接口，认证方式为 API Key：

```bash
# 获取用户健康档案
curl -X GET "http://中台地址:8080/api/user/profile" \
  -H "X-Api-Key: HealthHyperAiToolKey2026" \
  -H "X-User-Id: 3"

# 获取最新生理数据
curl -X GET "http://中台地址:8080/api/vital/current?metric=all" \
  -H "X-Api-Key: HealthHyperAiToolKey2026" \
  -H "X-User-Id: 3"

# 添加日常记录
curl -X POST "http://中台地址:8080/api/daily" \
  -H "X-Api-Key: HealthHyperAiToolKey2026" \
  -H "X-User-Id: 3" \
  -H "Content-Type: application/json" \
  -d '{"logType":"symptom","content":"头疼，持续2小时","extraJson":"{\"symptom_name\":\"headache\",\"severity\":2}"}'

# 保存对话摘要
curl -X POST "http://中台地址:8080/api/ai/summary" \
  -H "X-Api-Key: HealthHyperAiToolKey2026" \
  -H "X-User-Id: 3" \
  -H "Content-Type: application/json" \
  -d '{"summary":"{\"recent_topics\":[\"睡眠质量\",\"头疼\"],\"key_findings\":\"深睡不足\"}"}'
```

---

## 9. Skill → API 映射总表

| Skill | Tool | HTTP 端点 |
|---|---|---|
| user_profile | `get_user_profile` | GET /api/user/profile |
| user_profile | `update_user_profile` | PUT /api/user/profile |
| physiological_analysis | `get_current_physiological` | GET /api/vital/current?metric= |
| physiological_analysis | `get_physiological_stats` | GET /api/vital/stats?metric=&days= |
| physiological_baseline | `get_baseline` | GET /api/baseline?metric= |
| physiological_baseline | `update_one_baseline` | PUT /api/baseline |
| physiological_baseline | `update_all_baseline` | PUT /api/baseline/all |
| sleep_analysis | `obtain_single_night_sleep` | GET /api/sleep/latest |
| sleep_analysis | `obtain_multi_night_sleep` | GET /api/sleep/recent?days= |
| sleep_analysis | `get_sleep_plan` | GET /api/sleep/plan |
| sleep_analysis | `update_sleep_plan` | PUT /api/sleep/plan |
| sleep_analysis | `update_sleep_analysis` | PUT /api/sleep/latest/analysis |
| medication_assistant | `add_medication_plan` | POST /api/medication/plan |
| medication_assistant | `obtain_medication_plan` | GET /api/medication/plan/list |
| medication_assistant | `record_medication_taking` | POST /api/medication/log |
| medication_assistant | `obtain_medication_log` | GET /api/medication/log?days= |
| daily_tracking | `add_daily_record` | POST /api/daily |
| daily_tracking | `get_daily_records` | GET /api/daily?logType=&days=&limit= |
| medical_report_analysis | `generate_medical_report` | POST /api/medical/report |
| health_planning | `create_health_plan` | POST /api/plan |
| health_planning | `get_health_plans` | GET /api/plan?days= |
| health_planning | `checkin_health_plan` | PUT /api/plan/{id}/checkin |
| conversation_summary | `summarize_conversation` | POST /api/ai/summary |
| conversation_summary | `get_conversation_memory` | GET /api/ai/summary/latest |
| weather | `query_weather` | 外部API，不经中台 |
| weather | `get_air_quality` | 外部API，不经中台 |

---

## 10. 数据库设计概要

共 17 张表（完整定义见 `schema.sql`），按功能分组：

| 分组 | 表 | 状态 |
|------|----|------|
| 用户 | `sys_user` | ✅ 已有代码 |
| 档案 | `health_profile`, `health_profile_history` | ✅ 已有代码 |
| 设备 | `device` | ✅ 已有实体/Mapper |
| 生理数据 | `vital_sign` | ✅ 已有代码（上报/查询/统计） |
| 基线 | `baseline` | ✅ 已有代码（查询/更新） |
| 睡眠 | `sleep_session`, `sleep_stage` | ✅ 已有代码（上传/查询/聚合） |
| 用药 | `medication_plan`, `medication_log` | ✅ 已有代码（计划CRUD/服药记录） |
| 医疗记录 | `medical_record` | ✅ 已有代码（报告生成/查询） |
| 日常记录 | `daily_log` | ✅ 已有代码（记录/查询） |
| 健康计划 | `health_plan` | ✅ 已有代码（创建/查询/打卡/取消打卡，status: 0=未完成 1=已打卡） |
| AI 对话 | `chat_session`, `chat_message` | ✅ 已有代码 |
| 告警 | `health_alert` | ✅ 已有代码（创建/查询/已读/vital_sign.flag自动生成） |
| AI 摘要 | `ai_health_summary` | ✅ 已有代码（保存/查询） |

---

## 11. 开发进度 & 后续计划

### 已完成

- [x] 项目骨架（pom.xml / yml / 全局异常 / 统一返回 / JWT / CORS）
- [x] 用户认证（注册 / 登录）
- [x] 健康档案 CRUD + 变更历史
- [x] AI 对话全链路（会话管理 / SSE 流式 / 云AI联调 / ACTIONS 解析）
- [x] Mock 模式（可离线自测）
- [x] 全部 17 张表建表脚本
- [x] 双重认证（JWT + API Key，支持手机端和AI端）
- [x] 实时生理数据（上报 / 查询最新 / 统计）
- [x] 生理基线管理（查询 / 单项更新 / 全量更新）
- [x] 睡眠数据（上传含分期 / 夜间聚合 / 单晚查询 / 多晚查询 / 睡眠计划）
- [x] 用药管理（创建计划 / 查询最新 / 记录服药 / 查询日志）
- [x] 日常记录（添加 / 按类型时间查询）
- [x] 医疗记录（报告生成 / 查询列表）
- [x] 健康计划（创建 / 列表 / 最新）
- [x] AI 健康摘要（保存 / 查询最新）
- [x] 异常告警（创建 / 列表 / 标记已读）
- [x] 全部 AI Skill Tool 对应的 REST API 端点

- [x] vital_sign.flag → health_alert 自动生成 + severity>1 → AI 主动对话
- [x] 健康计划打卡机制（status 0/1 + checkin/uncheckin 端点）
- [x] 用药日志返回时附带关联药品计划信息
- [x] 睡眠 AI 分析回填接口（PUT /api/sleep/latest/analysis 自动找最新 + PUT /api/sleep/{id}/analysis 按 ID）
- [x] 用药计划返回全部有效列表（GET /api/medication/plan/list）
- [x] 手机端删改接口补全（DailyLog、ChatSession、HealthPlan、MedicationPlan、MedicalRecord、SleepSession、HealthAlert）
- [x] health_profile 变更 → 自动触发 AI 摘要更新
- [x] ObjectMapper 注入修复（health_profile_history 可正常写入）
- [x] ACTIONS 机制标记为遗留兼容

### 待开发（按优先级）

| 优先级 | 模块 | 备注 |
|--------|------|------|
| P1 | 通知轮询 | `/api/notify/poll`，手机端轮询 system_alert 会话 |
| P2 | 设备管理 CRUD | `/api/device` |
| P3 | 用药依从率统计 | 超时未服药自动标记 missed |

---

## 12. 重要约定 & 注意事项

1. **不存图片**：base64 只在请求链路中临时传递，不落库。`chat_message.msg_type = "image_text"` 标记消息源自图片输入。
2. **表无物理外键**：逻辑层面用代码关联。
3. **`daily_log` 是日志记事本**：用 `log_type` 区分类别（diet/exercise/mood/symptom 等），`extra_json` 是不固定字段。`daily_log` 与 `health_plan` 已分离，打卡通过 `health_plan.status` 字段实现。
4. **`chat_session.id` 全局自增**：天然满足"不同用户的 thread_id 不重复"的要求。
5. **`UserContext.get()`**：在 Controller/Service 中通过 `UserContext.get()` 获取当前登录用户 ID，由 `JwtInterceptor` 在请求进入时写入 ThreadLocal（支持 JWT 和 API Key 两种来源）。
6. **测试用例文件**：`spring-boot-demo/api-tests.http`（VS Code REST Client 格式）。

---

## 13. 快速启动

```bash
# 1. 确保 MySQL 中已执行 schema.sql
# 2. 确认 application.yml 中数据库密码正确
# 3. 启动
cd spring-boot-demo
.\mvnw.cmd spring-boot:run     # Windows
./mvnw spring-boot:run         # Mac/Linux
# 4. 验证
curl http://localhost:8080/api/auth/login -X POST -H "Content-Type: application/json" -d '{"username":"apitest1","password":"123456"}'
```
