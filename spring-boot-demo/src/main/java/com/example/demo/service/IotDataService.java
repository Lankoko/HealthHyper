package com.example.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.common.BusinessException;
import com.example.demo.config.IotDaProperties;
import com.example.demo.dto.vital.VitalSignUploadRequest;
import com.example.demo.entity.Device;
import com.example.demo.entity.VitalSign;
import com.example.demo.mapper.DeviceMapper;
import com.healthhyper.cloudiotda.service.DeviceShadowService;
import com.huaweicloud.sdk.iotda.v5.model.DeviceShadowData;
import com.huaweicloud.sdk.iotda.v5.model.ShowDeviceShadowResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * IoTDA 数据拉取服务。
 *
 * 数据流：华为云 IoTDA（设备影子）→ 中台解析 → vital_sign 表 → 手机轮询。
 * 保留原有 /api/vital/upload 蓝牙直传方案，二者互不干扰。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "huawei.iotda", name = "enabled", havingValue = "true")
public class IotDataService {

    private final DeviceShadowService deviceShadowService;
    private final VitalSignService vitalSignService;
    // private final BaselineService baselineService;  // 已停用：不再同步IoTDA设备基线，仅保留AI基线
    private final DeviceMapper deviceMapper;
    private final IotDaProperties iotDaProperties;

    // ── 设备绑定 ──────────────────────────────────────────────────────

    /**
     * 将 IoTDA deviceId 绑定到当前用户。
     * IoTDA deviceId 存入 device.device_sn 字段。
     * 每个用户只保留一个活跃绑定（重复绑定覆盖旧记录）。
     */
    public Device bindDevice(Long userId, String iotdaDeviceId, String deviceName) {
        Device existing = deviceMapper.selectOne(
                new LambdaQueryWrapper<Device>().eq(Device::getUserId, userId));
        if (existing != null) {
            existing.setDeviceSn(iotdaDeviceId);
            existing.setDeviceName(deviceName != null ? deviceName : existing.getDeviceName());
            existing.setStatus(1);
            existing.setBoundAt(LocalDateTime.now());
            deviceMapper.updateById(existing);
            return existing;
        }
        Device device = new Device();
        device.setUserId(userId);
        device.setDeviceSn(iotdaDeviceId);
        device.setDeviceName(deviceName != null ? deviceName : "健康手环");
        device.setStatus(1);
        device.setBoundAt(LocalDateTime.now());
        deviceMapper.insert(device);
        return device;
    }

    public Device getBinding(Long userId) {
        return deviceMapper.selectOne(
                new LambdaQueryWrapper<Device>()
                        .eq(Device::getUserId, userId)
                        .eq(Device::getStatus, 1));
    }

    public void unbindDevice(Long userId) {
        Device device = deviceMapper.selectOne(
                new LambdaQueryWrapper<Device>().eq(Device::getUserId, userId));
        if (device == null) throw new BusinessException("未绑定设备");
        device.setStatus(0);
        deviceMapper.updateById(device);
    }

    // ── 数据拉取 ──────────────────────────────────────────────────────

    /**
     * 手机主动触发：从 IoTDA 拉取该用户设备的最新影子数据，
     * 解析后写入 vital_sign 并返回结果。
     */
    public VitalSign fetchLatest(Long userId) {
        Device device = getBinding(userId);
        if (device == null) throw new BusinessException("未绑定 IoTDA 设备");
        return fetchAndSave(userId, device.getDeviceSn(), device.getId());
    }

    /**
     * 定时任务：每隔 pollIntervalMs 毫秒拉取所有已绑定设备的最新数据。
     * fixedDelayString 从配置读取，默认 30s。
     */
    @Scheduled(fixedDelayString = "${huawei.iotda.poll-interval-ms:30000}")
    public void scheduledSync() {
        List<Device> devices = deviceMapper.selectList(
                new LambdaQueryWrapper<Device>().eq(Device::getStatus, 1));
        if (devices.isEmpty()) return;
        log.debug("IoTDA 定时同步，共 {} 个绑定设备", devices.size());
        for (Device device : devices) {
            try {
                fetchAndSave(device.getUserId(), device.getDeviceSn(), device.getId());
            } catch (Exception e) {
                log.warn("IoTDA 同步失败: userId={}, deviceSn={}, err={}",
                        device.getUserId(), device.getDeviceSn(), e.getMessage());
            }
        }
    }

    // ── 内部：拉取+解析+存储 ──────────────────────────────────────────

    private VitalSign fetchAndSave(Long userId, String iotdaDeviceId, Long deviceTableId) {
        ShowDeviceShadowResponse shadow = deviceShadowService.queryShadowDetail(iotdaDeviceId);
        if (shadow == null || shadow.getShadow() == null || shadow.getShadow().isEmpty()) {
            throw new BusinessException("IoTDA 未返回设备影子数据");
        }

        // 找到与配置 serviceId 匹配的影子分组；找不到则取第一个
        DeviceShadowData shadowData = shadow.getShadow().stream()
                .filter(s -> iotDaProperties.getServiceId().equals(s.getServiceId()))
                .findFirst()
                .orElse(shadow.getShadow().get(0));

        if (shadowData.getReported() == null || shadowData.getReported().getProperties() == null) {
            throw new BusinessException("设备影子 reported 为空，设备可能从未上报数据");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) shadowData.getReported().getProperties();

        // 同步基线（已停用：不再同步IoTDA设备基线，仅保留AI基线）
        // syncBaseline(userId, props);

        VitalSignUploadRequest req = parseVitalSign(props, shadowData.getReported().getEventTime());
        req.setDeviceId(deviceTableId);

        VitalSign vs = vitalSignService.upload(userId, req);
        log.info("IoTDA 数据同步完成: userId={}, deviceSn={}, flag={}", userId, iotdaDeviceId, vs.getFlag());
        return vs;
    }

    /**
     * 将 IoTDA 属性 Map 映射为 VitalSignUploadRequest。
     * 所有数值字段统一使用 Float，兼容 camelCase 和 snake_case 命名。
     */
    private VitalSignUploadRequest parseVitalSign(Map<String, Object> props, String eventTime) {
        VitalSignUploadRequest req = new VitalSignUploadRequest();

        req.setHr(toFloat(props.getOrDefault("hr", props.get("heartRate"))));
        req.setSpo2(toFloat(props.getOrDefault("spo2", props.get("bloodOxygen"))));
        req.setBt(toFloat(props.getOrDefault("bt", props.get("bodyTemp"))));
        req.setActivity(toFloat(props.getOrDefault("activity", null)));
        req.setTurnOut(toFloat(props.getOrDefault("turnOut", props.getOrDefault("turn_out", null))));
        req.setSdann(toFloat(props.getOrDefault("sdann", null)));
        req.setHrCv(toFloat(props.getOrDefault("hrCv", props.getOrDefault("hr_cv", null))));
        req.setFlag(toInt(props.getOrDefault("flag", 0)));
        req.setRecordedAt(parseEventTime(eventTime));

        return req;
    }

    // 已停用：不再同步IoTDA设备基线，仅保留AI基线
    // /**
    //  * 若影子属性中含基线字段，同步写入 baseline 表（source=device）。
    //  */
    // private void syncBaseline(Long userId, Map<String, Object> props) {
    //     Float hrBase   = toFloat(props.getOrDefault("hrBase",   props.get("hr_base")));
    //     Float spo2Base = toFloat(props.getOrDefault("spo2Base", props.get("spo2_base")));
    //     Float btBase   = toFloat(props.getOrDefault("btBase",   props.get("bt_base")));
    //     Float hrCvBase = toFloat(props.getOrDefault("hrCvBase", props.get("hr_cv_base")));
    //     Float sdannBase= toFloat(props.getOrDefault("sdannBase",props.get("sdann_base")));
    //     baselineService.updateFromDevice(userId, hrBase, spo2Base, btBase, hrCvBase, sdannBase);
    // }

    private LocalDateTime parseEventTime(String eventTime) {
        if (eventTime == null || eventTime.isBlank()) return LocalDateTime.now();
        // IoTDA 格式示例：20260409T080000Z 或 2026-04-09T08:00:00Z
        try {
            if (eventTime.contains("-")) {
                return LocalDateTime.parse(eventTime.replace("Z", ""),
                        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
            } else {
                return LocalDateTime.parse(eventTime.replace("Z", ""),
                        DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));
            }
        } catch (DateTimeParseException e) {
            log.debug("IoTDA eventTime 解析失败: {}, 使用当前时间", eventTime);
            return LocalDateTime.now();
        }
    }

    // ── 类型转换工具 ──────────────────────────────────────────────────

    private Float toFloat(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.floatValue();
        try { return Float.parseFloat(v.toString()); } catch (Exception e) { return null; }
    }

    private Integer toInt(Object v) {
        if (v == null) return 0;
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(v.toString()); } catch (Exception e) { return 0; }
    }

    /**
     * 将解析后的 VitalSign 封装为带来源标记的 Map，供 Controller 返回。
     */
    public Map<String, Object> buildResponse(VitalSign vs) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("source", "iotda");
        result.put("id", vs.getId());
        result.put("hr", vs.getHr());
        result.put("spo2", vs.getSpo2());
        result.put("bt", vs.getBt());
        result.put("activity", vs.getActivity());
        result.put("turn_out", vs.getTurnOut());
        result.put("sdann", vs.getSdann());
        result.put("hr_cv", vs.getHrCv());
        result.put("flag", vs.getFlag());
        result.put("recorded_at", vs.getRecordedAt());
        return result;
    }
}
