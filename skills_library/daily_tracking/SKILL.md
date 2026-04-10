---
name: daily_tracking
description: 日常记录，关于用户身体健康的随时记录，如饮水、步数、睡眠时长、心情等。支持记录和查询。
---

# daily_tracking

## 适用场景

- 随时记录用户日常行为，如饮食、运动、情绪、症状、计划完成等
- 用户需要查询历史行为记录数据，按照类型或者时间范围
- 其他SKILL需要最近记录数据辅助分析

## 提供的工具

- `add_daily_record`: 添加一条日常记录

  参数:

  - log_type: 记录类型(string)，已经预定设计为 diet（饮食）, exercise（运动）, mood（情绪）, symptom（症状）, plan_checkin（计划打卡）, other（其他)。可以根据用户的消息，动态增加记录类型
  - content: 记录内容(string)，可以是用户输入的消息，也可以是其他SKILL触发产生的记录内容
  - log_date: 记录日期
  - extra_json: 记录摘要和补充(string)，将用户消息合理拆成不同字段，以JSON形式的字符串输出，其中的字段消息不规定字段，按用户记录内容拆解成合适的字段、加上需要补充的说明和注意组成


  返回值(string): 添加记录成功与否

- `get_daily_records`: 查询时间段内的记录

  参数
  
  - log_type: 传入需要查找的类型
  - days: 最新几天的数据
  - limit: 最多返回条数

## 支持的记录类型

- diet: 饮食
- exercise: 运动
- mood: 情绪
- symptom: 临时症状
- plan_checkin: 计划完成打卡
- other: 其他

# 使用流程

1. 当用户明确表达记录意图（“我今天走了8000步”、“记录一下：头痛”）时，直接调用 `add_daily_record` 保存。
2. 当用户询问历史数据（“最近一周的饮食记录”）时，调用 `get_daily_records` 查询并整理回答。
3. 当其他技能（如感冒分析）判断需要记录症状时，应调用本工具的 `add_daily_record` 自动记录（例如症状类型 symptom，内容“咳嗽、发烧”）。
4. 鼓励用户坚持打卡，给予正向反馈。