---
name: conversation_summary
description: 与用户对话内容进行摘要
---

# conversation_summary

## 适用场景

- 用户希望回顾之前讨论过的健康话题、计划、症状等
- 需要提炼关键信息用于保存上下文

## 提供的工具

- `summarize_conversation`: 将当前对话内容整合为文本数据后存储

  参数:

  - summary: 通过分析用户对话中做出的摘要内容(string)，摘要内容的字符串格式按照JSON形式，其中字段无固定，根据用户对话记录动态定义

  返回值: 存储成功与否

- `get_conversation_memory`: 获取最近一条摘要数据

  参数: 无参数
  返回值: 最近一条摘要内容(string)

## 使用流程

- 用户需要进行总结对话时，先进行整理对话数据后准备摘要内容，再通过 `summarize_conversation` 存储
- 摘要内容需要保留用户具体数据或者特殊情况说明
- 每条摘要内容在100-300子内，控制摘要字数