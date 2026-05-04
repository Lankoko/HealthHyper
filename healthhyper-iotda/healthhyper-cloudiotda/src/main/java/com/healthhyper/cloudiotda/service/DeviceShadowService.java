package com.healthhyper.cloudiotda.service;

import com.healthhyper.cloudiotda.client.HuaweiIotDaClient;
import com.huaweicloud.sdk.iotda.v5.model.ShowDeviceShadowRequest;
import com.huaweicloud.sdk.iotda.v5.model.ShowDeviceShadowResponse;

public class DeviceShadowService {
    private final HuaweiIotDaClient huaweiIotDaClient;

    public DeviceShadowService(HuaweiIotDaClient huaweiIotDaClient) {
        this.huaweiIotDaClient = huaweiIotDaClient;
    }

    public ShowDeviceShadowResponse queryShadowDetail(String deviceId) {
        ShowDeviceShadowRequest request = new ShowDeviceShadowRequest();
        request.withDeviceId(deviceId);
        request.withInstanceId(huaweiIotDaClient.getIotDaConfig().instanceId());
        try {
            return huaweiIotDaClient.getIotDaClient().showDeviceShadow(request);
        } catch (Exception e) {
            huaweiIotDaClient.handleException(e);
            return null;
        }
    }
}
