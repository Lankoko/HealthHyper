# HealthHyper 手机端（鸿蒙）接口文档

> 面向手机端开发者，按 **页面/场景** 组织，每个接口包含完整请求和响应示例。
>
> 中台地址：`http://<IP>:8080`  |  在线文档：启动后访问 `http://<IP>:8080/swagger-ui.html`

---

按**手机端页面场景**：


| 章节              | 对应 App 页面 | 核心接口数        |
| --------------- | --------- | ------------ |
| 1. 登录注册         | 启动页       | 2            |
| 2. 首页 Dashboard | 首页多卡片     | 5 个并行请求      |
| 3. AI 对话        | 聊天页       | 4（含 SSE 流式）  |
| 4. 健康档案         | 个人信息      | 2            |
| 5. 硬件上报         | 后台静默      | 2            |
| 6. 生理详情         | 数据图表      | 3            |
| 7. 睡眠详情         | 睡眠报告      | 2            |
| 8. 用药管理         | 用药页       | 3            |
| 9. 健康计划         | 计划打卡      | 4            |
| 10. 报告          | 报告列表      | 1（支持 Tab 筛选） |
| 11. 告警          | 消息通知      | 3            |
| 12. 日常记录        | 记录页       | 2            |


几个对手机端特别有用的细节：

- **首页建议并行请求**哪些接口来拼 Dashboard
- **SSE 流式处理要点**（鸿蒙端怎么读流、怎么做打字机效果）
- **告警跳转 AI 对话**（`triggeredSessionId` 的用法）
- **用药 timeSlots** 可用于本地定时提醒
- **附录 A 速查表**：一张表看完全部接口
- **附录 B 待开发**：明确告诉 App 哪些还没做

## 通用约定

### 统一响应格式

所有接口返回 JSON：

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```


| code | 含义            |
| ---- | ------------- |
| 200  | 成功            |
| 400  | 参数错误          |
| 401  | 未登录或 token 过期 |
| 500  | 服务器内部错误       |


### 认证方式

除 `/api/auth/**` 外，所有接口需要在 Header 中携带 JWT Token：

```
Authorization: Bearer <登录时返回的 token>
```

Token 有效期 72 小时，过期后需重新登录。

### 请求体字段命名

所有 JSON 请求体使用 **camelCase**（如 `heightCm`、`bloodType`）。

### 日期时间格式


| 类型   | 格式                    | 示例                    |
| ---- | --------------------- | --------------------- |
| 日期   | `yyyy-MM-dd`          | `2026-04-09`          |
| 日期时间 | `yyyy-MM-ddTHH:mm:ss` | `2026-04-09T08:30:00` |


---

## 1. 登录注册页

### 1.1 注册

```
POST /api/auth/register
```

**请求体：**

```json
{
  "username": "zhangsan",
  "password": "123456",
  "nickname": "张三",
  "phone": "13800000000"
}
```


| 字段       | 类型     | 必填  | 说明               |
| -------- | ------ | --- | ---------------- |
| username | string | ✅   | 登录账号，唯一          |
| password | string | ✅   | 密码（明文传输，服务端加密存储） |
| nickname | string | ❌   | 昵称               |
| phone    | string | ❌   | 手机号              |


**成功响应：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOi...",
    "userId": 3
  }
}
```

> 注册成功后直接返回 token，无需再调登录接口。

### 1.2 登录

```
POST /api/auth/login
```

**请求体：**

```json
{
  "username": "zhangsan",
  "password": "123456"
}
```

**成功响应：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOi...",
    "userId": 3
  }
}
```

> **token 存储建议**：保存到应用本地存储，后续每个请求的 `Authorization` Header 都带上。

---

## 2. 首页 / Dashboard

首页需要组合调用多个接口拼出概览卡片，建议并行请求：


| 卡片   | 调用接口                                | 说明           |
| ---- | ----------------------------------- | ------------ |
| 实时生理 | `GET /api/vital/current?metric=all` | 最新心率/血氧/体温   |
| 未读告警 | `GET /api/alert?unreadOnly=true`    | 未读告警列表（红点提示） |
| 今日计划 | `GET /api/plan?days=1`              | 今天的计划及打卡状态   |
| 最新睡眠 | `GET /api/sleep/latest`             | 昨晚睡眠质量概要     |
| 用药提醒 | `GET /api/medication/plan/list`     | 当前全部有效用药计划   |


### 2.1 获取最新生理数据

```
GET /api/vital/current?metric=all
```

**响应 data：**

```json
{
  "recorded_at": "2026-04-09T14:30:00",
  "hr": 78,
  "spo2": 98,
  "bt": 36.6,
  "activity": 10,
  "sdann": 120.5,
  "hr_cv": 0.048,
  "flag": 0
}
```


| 字段       | 说明            |
| -------- | ------------- |
| hr       | 心率 (bpm)      |
| spo2     | 血氧 (%)        |
| bt       | 体温 (℃)        |
| activity | 活动强度          |
| sdann    | HRV 指标        |
| hr_cv    | 心率变异系数        |
| flag     | 0=正常，1-3=异常等级 |


> `metric` 参数可选 `all`、`hr`、`spo2`、`bt`，控制返回哪些字段。

### 2.2 获取未读告警

```
GET /api/alert?unreadOnly=true
```

**响应 data（数组）：**

```json
[
  {
    "id": 1,
    "alertType": "hr_abnormal",
    "severity": 2,
    "message": "硬件检测到异常(flag=2)：心率135bpm 血氧88%",
    "triggeredSessionId": 5,
    "isRead": 0,
    "createdAt": "2026-04-09T15:00:00"
  }
]
```


| 字段                 | 说明                             |
| ------------------ | ------------------------------ |
| severity           | 1=轻微，2=中等，3=严重                 |
| triggeredSessionId | 不为 null 时表示 AI 已主动发起对话，可跳转到该会话 |


---

## 3. AI 对话页

### 3.1 获取会话列表

```
GET /api/chat/sessions
```

**响应 data（数组，按更新时间倒序）：**

```json
[
  {
    "id": 5,
    "userId": 3,
    "title": "健康异常提醒",
    "source": "system_alert",
    "isRead": 0,
    "createdAt": "2026-04-09T15:00:00",
    "updatedAt": "2026-04-09T15:01:00"
  },
  {
    "id": 2,
    "userId": 3,
    "title": "你好，简单介绍一下你能做什么",
    "source": "user",
    "isRead": 1,
    "createdAt": "2026-04-09T10:00:00",
    "updatedAt": "2026-04-09T10:05:00"
  }
]
```


| source 值         | 含义                | UI 建议               |
| ---------------- | ----------------- | ------------------- |
| `user`           | 用户主动发起的对话         | 普通对话图标              |
| `system_alert`   | 异常告警触发的 AI 主动对话   | 告警图标 + "AI 主动发起" 标签 |
| `system_summary` | 系统摘要更新（用户一般不需要看到） | 可隐藏或灰色显示            |


> **未读提示**：`isRead=0` 的会话在列表中显示红点。

### 3.2 创建新会话

```
POST /api/chat/sessions
```

无请求体。

**响应 data：**

```json
{
  "id": 6,
  "userId": 3,
  "source": "user",
  "isRead": 1,
  "createdAt": "2026-04-09T16:00:00",
  "updatedAt": "2026-04-09T16:00:00"
}
```

### 3.3 获取消息历史

```
GET /api/chat/sessions/{sessionId}/messages
```

**响应 data（数组，按时间正序）：**

```json
[
  {
    "id": 1,
    "sessionId": 2,
    "role": "user",
    "msgType": "text",
    "content": "你好，简单介绍一下你能做什么",
    "createdAt": "2026-04-09T10:00:00"
  },
  {
    "id": 2,
    "sessionId": 2,
    "role": "assistant",
    "msgType": "text",
    "content": "你好！我是你的健康管家...",
    "createdAt": "2026-04-09T10:00:05"
  }
]
```


| role        | 说明               |
| ----------- | ---------------- |
| `user`      | 用户消息（或系统伪装的用户消息） |
| `assistant` | AI 回复            |



| msgType      | 说明                  |
| ------------ | ------------------- |
| `text`       | 纯文本                 |
| `image_text` | 含图片的消息（图片不落库，仅标记来源） |


### 3.4 发送消息（SSE 流式）

```
POST /api/chat/sessions/{sessionId}/send
Content-Type: application/json
```

**请求体：**

```json
{
  "text": "我最近睡眠不太好，有什么建议吗？",
  "imgBase64": null
}
```


| 字段        | 类型     | 必填  | 说明                                                 |
| --------- | ------ | --- | -------------------------------------------------- |
| text      | string | ✅   | 用户输入的文本                                            |
| imgBase64 | string | ❌   | 图片 base64（含 `data:image/jpeg;base64,` 前缀），无图传 null |


**响应格式**：`text/event-stream`（SSE），非标准 JSON。

```
data: 你
data: 好
data: ！
data: 我来
data: 帮你
data: 分析
...
```

**鸿蒙端 SSE 处理要点**：

1. 用 HTTP 流式读取（逐行读取 response body）
2. 每行格式为 `data: 内容`，提取 `data:` 后面的文本
3. 逐段追加到 UI 的气泡文字中，实现打字机效果
4. 流结束后，完整的 AI 回复已自动存入 `chat_message` 表

---

## 4. 健康档案页

### 4.1 获取当前档案

```
GET /api/user/profile
```

**响应 data：**

```json
{
  "id": 1,
  "userId": 3,
  "gender": 1,
  "birthDate": "2000-05-15",
  "heightCm": 175.0,
  "weightKg": 68.5,
  "bloodType": "A",
  "medicalHistory": "无重大病史",
  "allergyInfo": "青霉素过敏",
  "lifestyleInfo": "久坐办公，偶尔跑步",
  "extraJson": null,
  "updatedAt": "2026-04-09T14:00:00"
}
```


| 字段                  | 说明              |
| ------------------- | --------------- |
| gender              | 1=男，2=女         |
| birthDate           | 出生日期            |
| heightCm / weightKg | 身高(cm) / 体重(kg) |
| bloodType           | 血型              |
| medicalHistory      | 既往病史            |
| allergyInfo         | 过敏信息            |
| lifestyleInfo       | 生活习惯            |


> 首次访问如果没有档案，返回的 data 中只有 `userId`，其余字段为 null。

### 4.2 更新档案

```
PUT /api/user/profile
```

**请求体（只传要修改的字段）：**

```json
{
  "heightCm": 175.5,
  "weightKg": 69.0,
  "lifestyleInfo": "开始每天跑步"
}
```

> 更新成功后，中台会自动触发 AI 摘要更新（后台异步，不影响响应速度）。

---

## 5. 硬件数据上报

手机通过蓝牙从 MCU 接收数据后，调用以下接口上报中台。

### 5.1 上报生理数据

```
POST /api/vital/upload
```

**请求体：**

```json
{
  "hr": 78,
  "spo2": 98,
  "bt": 36.6,
  "activity": 10,
  "sdann": 120.5,
  "hrCv": 0.048,
  "flag": 0,
  "deviceId": 1,
  "recordedAt": "2026-04-09T14:30:00"
}
```


| 字段         | 类型       | 必填  | 说明                          |
| ---------- | -------- | --- | --------------------------- |
| hr         | short    | ❌   | 心率 (bpm)                    |
| spo2       | short    | ❌   | 血氧 (%)                      |
| bt         | decimal  | ❌   | 体温 (℃)                      |
| activity   | short    | ❌   | 活动强度                        |
| sdann      | float    | ❌   | HRV 指标                      |
| hrCv       | float    | ❌   | 心率变异系数                      |
| flag       | int      | ❌   | 0=正常，1=轻微异常，2=中等，3=严重（硬件判定） |
| deviceId   | long     | ❌   | 设备 ID                       |
| recordedAt | datetime | ❌   | 采集时间，不传默认当前时间               |


> **flag > 0 自动行为**：中台会自动生成 `health_alert`（严重程度 = flag），当 flag ≥ 2 时还会自动创建 AI 对话会话，用户会在会话列表看到 AI 的主动关心消息。

### 5.2 上报睡眠数据

```
POST /api/sleep/upload
```

**请求体：**

```json
{
  "sleepDate": "2026-04-08",
  "startTime": "2026-04-08T23:15:00",
  "endTime": "2026-04-09T07:20:00",
  "stages": [
    { "stage": "n1",  "startTime": "2026-04-08T23:15:00", "durationSec": 900 },
    { "stage": "n2",  "startTime": "2026-04-08T23:30:00", "durationSec": 5400 },
    { "stage": "n3",  "startTime": "2026-04-09T01:00:00", "durationSec": 4500 },
    { "stage": "rem", "startTime": "2026-04-09T02:15:00", "durationSec": 5400 },
    { "stage": "wake","startTime": "2026-04-09T06:45:00", "durationSec": 2100 }
  ]
}
```


| 字段                   | 说明                                                   |
| -------------------- | ---------------------------------------------------- |
| sleepDate            | 睡眠日期（入睡那天）                                           |
| startTime / endTime  | 入睡/醒来时间                                              |
| stages[].stage       | 分期：`n1`(浅睡)、`n2`(中睡)、`n3`(深睡)、`rem`(快速眼动)、`wake`(清醒) |
| stages[].durationSec | 该阶段持续秒数                                              |


> 中台会自动用该时间段内的 vital_sign 数据聚合出整晚心率/血氧/体温的均值、最大、最小值。

---

## 6. 生理数据详情页

### 6.1 最新生理数据

见 [2.1 获取最新生理数据](#21-获取最新生理数据)

### 6.2 历史统计

```
GET /api/vital/stats?metric=all&days=7
```

**响应 data：**

```json
{
  "days": 7,
  "count": 42,
  "hr": { "avg": 73.2, "max": 108, "min": 58, "count": 42 },
  "spo2": { "avg": 97.5, "max": 99, "min": 92, "count": 42 },
  "bt": { "avg": 36.5, "max": 37.9, "min": 36.1, "count": 42 }
}
```

> 适合用于趋势图/柱状图展示。`days` 参数可调（1/3/7/30）。

### 6.3 查看基线

```
GET /api/baseline?metric=all
```

**响应 data：**

```json
{
  "hr_base": 72.5,
  "spo2_base": 97.0,
  "bt_base": 36.5,
  "hr_cv_base": 0.05,
  "sdann_base": 120.0,
  "effective_at": "2026-04-09T10:00:00"
}
```

> 基线由 AI 分析后设置，手机端只读展示，可用于在数据图表上画"正常基线"参考线。

---

## 7. 睡眠详情页

### 7.1 最近一晚

```
GET /api/sleep/latest
```

**响应 data：**

```json
{
  "id": 1,
  "sleep_date": "2026-04-08",
  "start_time": "2026-04-08T23:15:00",
  "end_time": "2026-04-09T07:20:00",
  "heart_rate": { "avg": 61.7, "max": 65, "min": 58 },
  "spo2": { "avg": 97.0, "max": 98, "min": 96 },
  "body_temp": { "avg": 36.3, "max": 36.4, "min": 36.1 },
  "ai_analysis": "整晚睡眠约8小时，深睡占比28%...",
  "stages": [
    { "stage": "n1",  "start_time": "2026-04-08T23:15:00", "duration_sec": 900 },
    { "stage": "n2",  "start_time": "2026-04-08T23:30:00", "duration_sec": 5400 }
  ],
  "total_sleep_sec": 29700,
  "stage_percentages": { "n1": "3.0%", "n2": "30.3%", "n3": "27.3%", "rem": "30.3%", "wake": "7.1%" }
}
```

> **UI 建议**：`stage_percentages` 可直接用于饼图；`heart_rate`/`spo2`/`body_temp` 展示为"整晚生理概要"卡片；`ai_analysis` 作为 AI 解读文字展示（可能为 null，表示 AI 尚未分析）。

### 7.2 最近多晚

```
GET /api/sleep/recent?days=7
```

返回数组，每个元素结构同 7.1。适合做睡眠趋势图。

---

## 8. 用药管理页

### 8.1 查看当前用药计划

```
GET /api/medication/plan/list
```

**响应 data：**

```json
{
  "id": 1,
  "userId": 3,
  "drugName": "头孢克洛胶囊",
  "dosage": "每次0.5g（2粒）",
  "frequency": "每日3次",
  "timeSlots": "08:00,12:00,18:00",
  "startDate": "2026-04-09",
  "endDate": "2026-04-16",
  "notes": "饭后服用",
  "status": 1,
  "createdAt": "2026-04-09T10:00:00"
}
```

> `timeSlots` 是逗号分隔的提醒时间，可用于本地定时通知。如果返回 data 为 null 则表示没有正在执行的计划。

### 8.2 记录服药

```
POST /api/medication/log
```

**请求体：**

```json
{
  "drugName": "头孢克洛",
  "time": "2026-04-09 08:15:00"
}
```


| 字段       | 类型     | 必填  | 说明              |
| -------- | ------ | --- | --------------- |
| drugName | string | ✅   | 药品名称（模糊匹配关联计划）  |
| time     | string | ❌   | 实际服药时间，不传默认当前时间 |


### 8.3 查看服药记录

```
GET /api/medication/log?days=3
```

**响应 data（数组，含关联的计划信息）：**

```json
[
  {
    "id": 1,
    "planId": 1,
    "action": "taken",
    "actualTime": "2026-04-09T08:15:00",
    "scheduledTime": "2026-04-09T08:15:00",
    "createdAt": "2026-04-09T08:15:00",
    "drugName": "头孢克洛胶囊",
    "dosage": "每次0.5g（2粒）",
    "frequency": "每日3次",
    "timeSlots": "08:00,12:00,18:00"
  }
]
```

> 每条记录已包含药品名和剂量等信息，无需再调计划接口。

---

## 9. 健康计划页

### 9.1 查看计划列表

```
GET /api/plan?days=7
```

**响应 data（数组）：**

```json
[
  {
    "id": 1,
    "userId": 3,
    "title": "血糖管理计划",
    "itemsJson": "[{\"index\":0,\"desc\":\"每天饭后散步20分钟\",\"category\":\"exercise\"},{\"index\":1,\"desc\":\"减少白米饭\",\"category\":\"diet\"}]",
    "source": "ai",
    "status": 0,
    "startDate": "2026-04-09",
    "endDate": "2026-05-09",
    "createdAt": "2026-04-09T10:00:00"
  }
]
```


| 字段        | 说明                              |
| --------- | ------------------------------- |
| status    | **0** = 未完成/待打卡，**1** = 已打卡/已完成 |
| source    | `ai` = AI 创建，`manual` = 用户自建    |
| itemsJson | 计划明细（JSON 字符串，需前端解析）            |


### 9.2 用户自建计划

```
POST /api/plan
```

**请求体：**

```json
{
  "title": "我的运动计划",
  "content": "[{\"desc\":\"每天跑步30分钟\"},{\"desc\":\"周末爬山\"}]",
  "days": 30,
  "source": "manual"
}
```

### 9.3 打卡

```
PUT /api/plan/{id}/checkin
```

无请求体。成功后该计划 `status` 变为 1。

### 9.4 取消打卡

```
PUT /api/plan/{id}/uncheckin
```

无请求体。`status` 恢复为 0。

---

## 10. 医疗报告页

### 10.1 获取报告列表

```
GET /api/medical/records?limit=20
```

**按类型筛选：**

```
GET /api/medical/records?recordType=exam_report&limit=10
GET /api/medical/records?recordType=health_analysis&limit=10
```


| recordType        | 含义          | UI 建议       |
| ----------------- | ----------- | ----------- |
| `exam_report`     | 体检报告 / 检查报告 | 放在"检查报告"Tab |
| `health_analysis` | AI 健康分析报告   | 放在"健康分析"Tab |


**响应 data（数组）：**

```json
[
  {
    "id": 1,
    "userId": 3,
    "recordType": "exam_report",
    "title": "2026年春季体检报告解读",
    "content": "{\"blood_glucose\":6.2,\"summary\":\"血糖偏高\",\"suggestion\":\"减少精制碳水\"}",
    "source": "ai_extract",
    "recordDate": "2026-04-01",
    "createdAt": "2026-04-09T10:00:00"
  }
]
```

> `content` 是 JSON 字符串，不同报告结构不同，前端可解析后格式化展示，也可直接展示为文本。

---

## 11. 告警页

### 11.1 全部告警

```
GET /api/alert
```

### 11.2 只看未读

```
GET /api/alert?unreadOnly=true
```

### 11.3 标记已读

```
PUT /api/alert/{id}/read
```

无请求体。

> **跳转到 AI 对话**：如果告警的 `triggeredSessionId` 不为 null，点击告警可直接跳转到对应 AI 会话（用 `GET /api/chat/sessions/{triggeredSessionId}/messages` 获取内容）。

---

## 12. 日常记录页

### 12.1 添加记录

```
POST /api/daily
```

**请求体示例（饮食）：**

```json
{
  "logType": "diet",
  "content": "午饭：米饭、西红柿炒蛋、紫菜汤",
  "extraJson": "{\"meal_type\":\"lunch\"}"
}
```

**请求体示例（运动）：**

```json
{
  "logType": "exercise",
  "content": "下午跑步30分钟",
  "extraJson": "{\"exercise_type\":\"running\",\"duration_min\":30}"
}
```

**请求体示例（情绪）：**

```json
{
  "logType": "mood",
  "content": "今天心情不错",
  "extraJson": "{\"mood_score\":4}"
}
```


| logType    | 说明   |
| ---------- | ---- |
| `diet`     | 饮食记录 |
| `exercise` | 运动记录 |
| `mood`     | 情绪记录 |
| `symptom`  | 症状记录 |
| `other`    | 其他   |


> `extraJson` 是自由格式的扩展字段，不同 logType 可以存不同结构，也可以不传。

### 12.2 查询记录

```
GET /api/daily?days=7&limit=20
GET /api/daily?logType=mood&days=7
```


| 参数      | 默认    | 说明     |
| ------- | ----- | ------ |
| logType | 无（全部） | 按类型筛选  |
| days    | 7     | 最近几天   |
| limit   | 50    | 最多返回条数 |


---

## 13. AI 健康摘要（可选展示）

```
GET /api/ai/summary/latest
```

**响应 data：**

```json
{
  "id": 1,
  "userId": 3,
  "summaryJson": "{\"recent_topics\":[\"睡眠质量差\",\"血糖偏高\"],\"key_findings\":\"深睡不足\",\"action_taken\":\"已创建睡眠改善计划\"}",
  "lastAnalyzedAt": "2026-04-09T10:00:00",
  "updatedAt": "2026-04-09T10:00:00"
}
```

> 这是 AI 对话后自动生成的健康概况摘要，可在"我的"页面或健康档案页中展示。

---

## 附录 A：接口速查表


| 场景   | 方法   | 路径                                        | 说明       |
| ---- | ---- | ----------------------------------------- | -------- |
| 注册   | POST | `/api/auth/register`                      | 返回 token |
| 登录   | POST | `/api/auth/login`                         | 返回 token |
| 获取档案 | GET  | `/api/user/profile`                       |          |
| 更新档案 | PUT  | `/api/user/profile`                       | 只传要改的字段  |
| 上报生理 | POST | `/api/vital/upload`                       | 硬件数据上报   |
| 最新生理 | GET  | `/api/vital/current?metric=`              |          |
| 生理统计 | GET  | `/api/vital/stats?metric=&days=`          |          |
| 查看基线 | GET  | `/api/baseline?metric=`                   |          |
| 上传睡眠 | POST | `/api/sleep/upload`                       | 含分期数据    |
| 最近一晚 | GET  | `/api/sleep/latest`                       |          |
| 最近多晚 | GET  | `/api/sleep/recent?days=`                 |          |
| 用药计划 | GET  | `/api/medication/plan/list`               | 全部有效计划   |
| 记录服药 | POST | `/api/medication/log`                     |          |
| 服药记录 | GET  | `/api/medication/log?days=`               | 含药品信息    |
| 创建对话 | POST | `/api/chat/sessions`                      |          |
| 会话列表 | GET  | `/api/chat/sessions`                      |          |
| 消息历史 | GET  | `/api/chat/sessions/{id}/messages`        |          |
| 发送消息 | POST | `/api/chat/sessions/{id}/send`            | SSE 流式   |
| 计划列表 | GET  | `/api/plan?days=`                         |          |
| 创建计划 | POST | `/api/plan`                               |          |
| 打卡   | PUT  | `/api/plan/{id}/checkin`                  |          |
| 取消打卡 | PUT  | `/api/plan/{id}/uncheckin`                |          |
| 报告列表 | GET  | `/api/medical/records?recordType=&limit=` |          |
| 告警列表 | GET  | `/api/alert?unreadOnly=`                  |          |
| 标记已读 | PUT  | `/api/alert/{id}/read`                    |          |
| 添加日志 | POST | `/api/daily`                              |          |
| 查询日志 | GET  | `/api/daily?logType=&days=&limit=`        |          |
| 健康摘要 | GET  | `/api/ai/summary/latest`                  |          |
| **删改接口** | | | |
| 删除会话 | DELETE | `/api/chat/sessions/{id}`               | 含消息一起删 |
| 修改日志 | PUT  | `/api/daily/{id}`                         |          |
| 删除日志 | DELETE | `/api/daily/{id}`                       |          |
| 停用用药计划 | PUT | `/api/medication/plan/{id}/stop`        | status→0 |
| 删除用药计划 | DELETE | `/api/medication/plan/{id}`           |          |
| 修改健康计划 | PUT | `/api/plan/{id}`                        |          |
| 删除健康计划 | DELETE | `/api/plan/{id}`                      |          |
| 删除医疗报告 | DELETE | `/api/medical/records/{id}`           |          |
| 删除睡眠记录 | DELETE | `/api/sleep/{id}`                     | 含分期一起删 |
| 全部已读 | PUT  | `/api/alert/read-all`                     |          |
| 删除告警 | DELETE | `/api/alert/{id}`                       |          |


---

## 附录 B：待开发接口（尚未实现）


| 功能                     | 说明                     | 优先级 |
| ---------------------- | ---------------------- | --- |
| `GET /api/notify/poll` | 通知轮询（新告警/新 AI 主动对话）    | P1  |
| 设备管理 CRUD              | `/api/device` 绑定/解绑/列表 | P2  |
| 用药依从率                  | 超时未服药标记 missed + 统计    | P3  |


