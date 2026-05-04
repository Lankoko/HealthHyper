package com.example.demo.config;

import com.healthhyper.cloudiotda.client.HuaweiIotDaClient;
import com.healthhyper.cloudiotda.config.IotDaConfig;
import com.healthhyper.cloudiotda.service.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 华为云 IoTDA 自动装配。
 * 仅在 huawei.iotda.enabled=true 时激活，避免未配置时启动失败。
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "huawei.iotda", name = "enabled", havingValue = "true")
public class IotDaAutoConfiguration {

    @Bean
    public IotDaConfig iotDaConfig(IotDaProperties props) {
        return new IotDaConfig(
                props.getAk(),
                props.getSk(),
                props.getRegion(),
                props.getIotdaEndpoint(),
                props.getProjectId(),
                props.getInstanceId(),
                props.getAppId(),
                props.getProductId(),
                props.getServiceId()
        );
    }

    @Bean
    public HuaweiIotDaClient huaweiIotDaClient(IotDaConfig iotDaConfig) {
        return new HuaweiIotDaClient(iotDaConfig);
    }

    @Bean
    public DeviceService iotDeviceService(HuaweiIotDaClient client) {
        return new DeviceService(client);
    }

    @Bean
    public DevicePropertyService devicePropertyService(HuaweiIotDaClient client) {
        return new DevicePropertyService(client);
    }

    @Bean
    public DeviceShadowService deviceShadowService(HuaweiIotDaClient client) {
        return new DeviceShadowService(client);
    }

    @Bean
    public DeviceCommandService deviceCommandService(HuaweiIotDaClient client) {
        return new DeviceCommandService(client);
    }

    @Bean
    public DeviceMessageService deviceMessageService(HuaweiIotDaClient client) {
        return new DeviceMessageService(client);
    }

    @Bean
    public SpaceService spaceService(HuaweiIotDaClient client) {
        return new SpaceService(client);
    }
}
