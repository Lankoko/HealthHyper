package com.healthhyper.cloudiotda.service;

import com.healthhyper.cloudiotda.client.HuaweiIotDaClient;
import com.huaweicloud.sdk.iotda.v5.model.*;

public class DevicePropertyService {
    private final HuaweiIotDaClient huaweiIotDaClient;

    public DevicePropertyService(HuaweiIotDaClient huaweiIotDaClient) {
        this.huaweiIotDaClient = huaweiIotDaClient;
    }

    public UpdatePropertiesResponse updateDevicePropertyRequest(String deviceId, DevicePropertiesRequest body) {
        UpdatePropertiesRequest request = new UpdatePropertiesRequest();
        request.setDeviceId(deviceId);
        request.setInstanceId(huaweiIotDaClient.getIotDaConfig().instanceId());
        request.setBody(body);
        try {
            return huaweiIotDaClient.getIotDaClient().updateProperties(request);
        } catch (Exception e) {
            huaweiIotDaClient.handleException(e);
            return null;
        }
    }

    public ListPropertiesResponse queryProperty(String deviceId, String serviceId) {
        ListPropertiesRequest request = new ListPropertiesRequest();
        request.setDeviceId(deviceId);
        request.setInstanceId(huaweiIotDaClient.getIotDaConfig().instanceId());
        request.setServiceId(serviceId);
        try {
            return huaweiIotDaClient.getIotDaClient().listProperties(request);
        } catch (Exception e) {
            huaweiIotDaClient.handleException(e);
            return null;
        }
    }
}
