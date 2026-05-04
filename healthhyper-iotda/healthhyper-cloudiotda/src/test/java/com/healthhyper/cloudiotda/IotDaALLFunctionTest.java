package com.healthhyper.cloudiotda;

import com.healthhyper.cloudiotda.client.HuaweiIotDaClient;
import com.healthhyper.cloudiotda.config.IotDaConfig;
import com.healthhyper.cloudiotda.config.IotDaConfigLoader;
import com.healthhyper.cloudiotda.service.*;
import org.junit.Before;
import org.junit.Test;

public class IotDaALLFunctionTest {
    // 测试参数（从配置加载）
    private String testProjectId;
    private String testInstanceId;
    private String testProductId;
    private String testAppId;
    private String testServiceId;
    private String testDeviceId;

    private DeviceService deviceService;
    private SpaceService spaceService;
    private DevicePropertyService devicePropertyService;
    private DeviceShadowService deviceShadowService;

    @Before
    public void init() {
        // 1. 加载核心配置（AK/SK/Region）
        IotDaConfig config = IotDaConfigLoader.loadCoreConfig();
        // 2. 加载测试参数
        this.testProjectId = config.projectId();
        this.testInstanceId = config.instanceId();
        this.testAppId = config.appId();
        this.testProductId = config.productId();
        this.testServiceId = config.serviceId();
        this.testDeviceId = "697b57aecbb0cf6bb9391ee4_healthhyper0";

        // 3. 创建核心客户端
        HuaweiIotDaClient client = new HuaweiIotDaClient(config);
        // 4. 初始化各服务
        deviceService = new DeviceService(client);
        spaceService = new SpaceService(client);
        devicePropertyService = new DevicePropertyService(client);
        deviceShadowService = new DeviceShadowService(client);
    }

    @Test
    public void showConfig() {
        System.out.println("===== 显示配置 =====");
        System.out.println("testProjectId: " + testProjectId);
        System.out.println("testInstanceId: " + testInstanceId);
        System.out.println("testAppId: " + testAppId);
        System.out.println("testProductId: " + testProductId);
        System.out.println("testServiceId: " + testServiceId);
        System.out.println("testDeviceId: " + testDeviceId);
    }

    @Test
    public void testListSpaces() {
        System.out.println("===== 测试查询空间列表 =====");
        var response = spaceService.showSpace();
        System.out.println("空间列表：{\n" + response.toString() + "\n}");
    }

    @Test
    public void testListDevices() {
        System.out.println("===== 测试查询设备列表 =====");
        var response = deviceService.listDevices(10, 0);
        System.out.println("设备列表：{\n" + response.toString() + "\n}");
    }

    @Test
    public void testShowDevice() {
        System.out.println("===== 测试查询设备详情 =====");
        var response = deviceService.showDevice(testDeviceId);
        System.out.println("设备详情：{\n" + response.toString() + "\n}");
    }

    @Test
    public void testListDeviceProperties() {
        System.out.println("===== 测试查询设备属性列表 =====");
        var response = devicePropertyService.queryProperty(testDeviceId, testServiceId);
        System.out.println("设备属性列表：{\n" + response.toString() + "\n}");
    }

    @Test
    public void testQueryShadowDetail() {
        System.out.println("===== 测试查询设备影子详情 =====");
        var response = deviceShadowService.queryShadowDetail(testDeviceId);
        System.out.println("设备影子详情：{\n" + response.toString() + "\n}");
    }
}
