---
name: physiological_baseline
description:  建立用户个性化的生理指标基线
---

# physiological_baseline

## 适用场景

- 用户首次使用生理监测功能，需要建立个性化基线。
- 用户数据积累一段时间后，要求重新计算基线（例如每月更新）。
- 其他健康分析SKILL需要获取用户的生理基线（正常范围、预警阈值）作为判断依据。

## 提供的工具

- `update_one_baseline`: 经分析用户历史生理数据、健康档案、生活习惯后得到的生理指标值，将该生理指标基线值保存

  参数:
  
  - metric: 指定生理指标类型，指定类型参数为 hr(心率), spo2(血氧浓度), bt(体温), hr_cv(心率变异系数), sdann(SDANN)
  - value: 对应生理指标的个人基线值(float类型)

  返回值(string): 保存成功与否

- `update_all_baseline`:  经分析用户历史生理数据、健康档案、生活习惯后得到的所有生理指标值保存

  参数:
  
  - hr_base: 个人心率基线值, float类型
  - spo2_base: 个人血氧浓度基线值, float类型
  - bt_base: 个人体温基线值, float类型
  - hr_cv_base: 个人心率变异系数基线值, float类型
  - sdann_base: 个人SDANN基线值, float类型

  返回值: 保存成功与否

- `get_baseline`: 获取用户当前存储的生理基线

  参数:

  - metric: 指定生理指标类型，指定类型参数为 hr(心率), spo2(血氧浓度), bt(体温), hr_cv(心率变异系数), sdann(SDANN), all(所有基线指标)
  
  返回值: 基线数据(带时间戳), string类型

## 使用流程

1. 用户询问生理指标基线值时，先尝试 `get_baseline` 查看是否有基线存在，如果没有或者基线数据时间过旧，则调用 `update_one_baseline` 或 `update_all_baseline` 更新
2. 调用 `update_one_baseline` 或 `update_all_baseline` 需要先结合用户历史生理数据、健康档案、生活习惯经分析计算后再更新保存
3. 其他技能（如异常检测）通过 `get_baseline` 获取基线，对比实时数据