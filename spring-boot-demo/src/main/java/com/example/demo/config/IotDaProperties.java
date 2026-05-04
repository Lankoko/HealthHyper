package com.example.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 华为云 IoTDA 配置属性，绑定 application.yml 中 huawei.iotda.* 配置项。
 */
@Component
@ConfigurationProperties(prefix = "huawei.iotda")
public class IotDaProperties {

    private String ak;
    private String sk;
    private String region;
    private String iotdaEndpoint;
    private String projectId;
    private String instanceId;
    private String appId;
    private String productId;
    private String serviceId = "healthdata";
    private long pollIntervalMs = 30000;
    private boolean enabled = false;

    public String getAk() { return ak; }
    public void setAk(String ak) { this.ak = ak; }

    public String getSk() { return sk; }
    public void setSk(String sk) { this.sk = sk; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getIotdaEndpoint() { return iotdaEndpoint; }
    public void setIotdaEndpoint(String iotdaEndpoint) { this.iotdaEndpoint = iotdaEndpoint; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getInstanceId() { return instanceId; }
    public void setInstanceId(String instanceId) { this.instanceId = instanceId; }

    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getServiceId() { return serviceId; }
    public void setServiceId(String serviceId) { this.serviceId = serviceId; }

    public long getPollIntervalMs() { return pollIntervalMs; }
    public void setPollIntervalMs(long pollIntervalMs) { this.pollIntervalMs = pollIntervalMs; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
