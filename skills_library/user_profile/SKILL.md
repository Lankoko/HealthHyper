---
name: user_profile
description: 用户健康档案基础信息，如性别、年龄、身高、体重、血型、病史、生活习惯等
---

# user_profile

## 使用场景

- 用户主动询问个人基本的健康信息时，尤其涉及到病史和生活习惯等
- 其他SKILL需要使用用户信息结合分析时，提供获取信息工具
- 当用户需要更新个人基本信息时，提供修改信息工具

## 提供的工具

- `get_user_profile`: 获得系统提供的用户基本信息，如性别、出生日期、身高、体重、血型、病史、过敏信息和生活习惯等，无需传入参数
- `update_user_profile`: 更新用户基本档案信息中的一个或多个字段

   参数(可选, 只传更新参数字段和值)

  - gender: 性别（男/女）
  - birth_date: 出生日期，格式 YYYY-MM-DD
  - height: 身高（厘米，浮点数）
  - weight: 体重（千克，浮点数）
  - blood_type: 血型（字符串）
  - medical_history: 病史（字符串，多个用逗号分隔）
  - allergies: 过敏史（字符串，多个用逗号分隔）
  - lifestyle: 生活习惯（字符串）

  返回值(string): 更新成功与否
 
## 使用流程

1. 当用户询问到基本健康档案信息或其他SKILL需要获取时，使用`get_user_profile`获取
2. 当用户提供新的基本档案信息或其他SKILL需要更新时，使用`update_user_profile`更新