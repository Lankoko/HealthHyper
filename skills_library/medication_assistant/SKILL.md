---
name: medication_assistant
description: 提供智能的用药服务
---

# medication_assistant

## 适用场景

- 用户需要制定用药计划、记录服药情况、是否有药物冲突等
- 用户希望分析用药依从性、了解漏服如何处理
- 用户反馈症状变化或副作用，需要判断是否换药或就医

## 依赖的其他SKILL
- `daily_tracking`: 记录症状、副作用等日常健康数据
- `user_profile`: 获取用户过敏史、慢性病史、肝肾功能等（用于禁忌提醒）

## 提供的工具

- `add_medication_plan`: 创建新的用药计划

  参数:

  - drug_name: 药品名称(string), 可以输入多个药品名称，使用逗号隔开
  - dosage: 剂量(string)，按照药品注明用药剂量
  - frequency: 频次(string)，按照药品注明服用频次
  - time_slots: 服用时间(string)，按照药品标记建议服用时间

  返回值: 保存成功与否

- `obtain_medication_paln`: 获取已记录的最新用药计划

  参数: 无参数输入
  返回值: 记录在案的最新一条用药计划(string)

- `record_medication_taking`: 记录单次服药情况

  参数:

  - drug_name: 药品名称
  - time: 服药时间，按照YY-MM-DD HH:MM:SS形式的string

  返回值: 记录成功与否

- `obtain_medication_log`: 获取最近服药记录

  参数:

  - days: 最近天数，天数小于3大于0

  返回值: 返回最近天数的服药情况

## 使用流程

1. 用户需要用药的最新情况下，调用 `add_medication_plan` 录用用药计划，不确定用户是否最新用药，先调用 `obtain_medication_paln` 获取情况
2. 记录用户说明的单次服药时，调用 `record_medication_taking` 记录用户用药情况
3. 当用户询问用药情况或者出现需要结合最近服药记录分析时，调用 `obtain_medication_log`获取近期用药情况

## 用药反馈流程

- 当用户在服药阶段时，需要做到反馈和更新
- 反馈需要结合用户状态信息，结合其他SKILL获取相关数据信息结合分析，动态指导用户用药
- 更新需要结合用药反馈情况，是否更新用药计划、记录用户特殊信息到基本档案、记录信息到日常记录或者提出就医建议等