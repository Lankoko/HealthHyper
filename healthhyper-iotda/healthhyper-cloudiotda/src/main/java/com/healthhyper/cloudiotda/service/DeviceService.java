package com.healthhyper.cloudiotda.service;

import com.healthhyper.cloudiotda.client.HuaweiIotDaClient;
import com.huaweicloud.sdk.iotda.v5.model.ListDevicesRequest;
import com.huaweicloud.sdk.iotda.v5.model.ListDevicesResponse;
import com.huaweicloud.sdk.iotda.v5.model.ShowDeviceRequest;
import com.huaweicloud.sdk.iotda.v5.model.ShowDeviceResponse;

public class DeviceService {
    private final HuaweiIotDaClient huaweiIotDaClient;

    public DeviceService(HuaweiIotDaClient huaweiIotDaClient) {
        this.huaweiIotDaClient = huaweiIotDaClient;
    }

    public ListDevicesResponse listDevices(Integer limit, Integer offset) {
        ListDevicesRequest request = new ListDevicesRequest();

        request.setProductId(huaweiIotDaClient.getIotDaConfig().productId());
        request.setAppId(huaweiIotDaClient.getIotDaConfig().appId());
        request.setLimit(limit);
        request.setOffset(offset);

        try {
            return huaweiIotDaClient.getIotDaClient().listDevices(request);
        } catch (Exception e) {
            huaweiIotDaClient.handleException(e);
            return null;
        }
    }

    public ShowDeviceResponse showDevice(String deviceId) {
        ShowDeviceRequest request = new ShowDeviceRequest();
        request.withDeviceId(deviceId);
        request.withInstanceId(huaweiIotDaClient.getIotDaConfig().instanceId());
        try {
            return huaweiIotDaClient.getIotDaClient().showDevice(request);
        } catch (Exception e) {
            huaweiIotDaClient.handleException(e);
            return null;
        }
    }

}