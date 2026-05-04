package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.common.UserContext;
import com.example.demo.entity.Device;
import com.example.demo.entity.VitalSign;
import com.example.demo.service.IotDataService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 华为云 IoTDA 数据接口。
 * 新数据流：IoTDA → 中台拉取 → vital_sign → 手机轮询本接口。
 * 原蓝牙直传方案（/api/vital/upload）保持不变，二者并存。
 *
 * 激活条件：huawei.iotda.enabled=true
 */
@Tag(name = "4b. IoTDA 硬件数据", description = "华为云IoTDA拉取 / 设备绑定 / 手机轮询（需 iotda.enabled=true）")
@RestController
@RequestMapping("/api/iotda")
@RequiredArgsConstructor
@ConditionalOnBean(IotDataService.class)
public class IotDataController {

    private final IotDataService iotDataService;

    // ── 设备绑定管理 ──────────────────────────────────────────────────

    /**
     * 绑定 IoTDA 设备到当前用户。
     * body: { "iotdaDeviceId": "xxx", "deviceName": "我的健康手环" }
     */
    @PostMapping("/device/bind")
    public Result<Device> bindDevice(@RequestBody Map<String, String> body) {
        String iotdaDeviceId = body.get("iotdaDeviceId");
        if (iotdaDeviceId == null || iotdaDeviceId.isBlank()) {
            return Result.error(400, "iotdaDeviceId 不能为空");
        }
        return Result.ok(iotDataService.bindDevice(
                UserContext.get(), iotdaDeviceId, body.get("deviceName")));
    }

    /**
     * 查询当前用户绑定的设备信息。
     */
    @GetMapping("/device")
    public Result<Device> getDevice() {
        return Result.ok(iotDataService.getBinding(UserContext.get()));
    }

    /**
     * 解绑设备。
     */
    @DeleteMapping("/device")
    public Result<Void> unbindDevice() {
        iotDataService.unbindDevice(UserContext.get());
        return Result.ok();
    }

    // ── 数据拉取 ──────────────────────────────────────────────────────

    /**
     * 手机轮询接口：立即从 IoTDA 拉取最新设备影子数据，
     * 解析写入 vital_sign 后返回。
     * 推荐轮询频率 ≥ 30s（与中台定时任务同步频率对齐即可）。
     */
    @GetMapping("/latest")
    public Result<Map<String, Object>> getLatest() {
        VitalSign vs = iotDataService.fetchLatest(UserContext.get());
        return Result.ok(iotDataService.buildResponse(vs));
    }
}
