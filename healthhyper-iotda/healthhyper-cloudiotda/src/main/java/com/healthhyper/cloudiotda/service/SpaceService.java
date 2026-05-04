package com.healthhyper.cloudiotda.service;

import com.healthhyper.cloudiotda.client.HuaweiIotDaClient;
import com.huaweicloud.sdk.iotda.v5.model.ShowApplicationRequest;
import com.huaweicloud.sdk.iotda.v5.model.ShowApplicationResponse;

public class SpaceService {
    private final HuaweiIotDaClient huaweiIotDaClient;

    public SpaceService(HuaweiIotDaClient huaweiIotDaClient) {
        this.huaweiIotDaClient = huaweiIotDaClient;
    }

    public ShowApplicationResponse showSpace() {
        ShowApplicationRequest request = new ShowApplicationRequest();
        request.setAppId(huaweiIotDaClient.getIotDaConfig().appId());
        try {
            return huaweiIotDaClient.getIotDaClient().showApplication(request);
        } catch (Exception e) {
            huaweiIotDaClient.handleException(e);
            return null;
        }
    }
}
