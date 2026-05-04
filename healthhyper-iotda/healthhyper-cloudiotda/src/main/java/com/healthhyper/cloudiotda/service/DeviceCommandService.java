package com.healthhyper.cloudiotda.service;

import com.healthhyper.cloudiotda.client.HuaweiIotDaClient;
import com.huaweicloud.sdk.iotda.v5.model.*;

public class DeviceCommandService {
    private final HuaweiIotDaClient huaweiIotDaClient;

    public DeviceCommandService(HuaweiIotDaClient huaweiIotDaClient) {
        this.huaweiIotDaClient = huaweiIotDaClient;
    }

    public CreateCommandResponse createCommand(String deviceId, DeviceCommandRequest body) {
        CreateCommandRequest request = new CreateCommandRequest();
        request.setDeviceId(deviceId);
        request.setInstanceId(huaweiIotDaClient.getIotDaConfig().instanceId());
        request.setBody(body);
        try {
            return huaweiIotDaClient.getIotDaClient().createCommand(request);
        } catch (Exception e) {
            huaweiIotDaClient.handleException(e);
            return null;
        }
    }

    public CreateBatchTaskResponse createTask(CreateBatchTask createBatchTask) {
        CreateBatchTaskRequest request = new CreateBatchTaskRequest();
        request.setInstanceId(huaweiIotDaClient.getIotDaConfig().instanceId());
        request.setBody(createBatchTask);
        try {
            return huaweiIotDaClient.getIotDaClient().createBatchTask(request);
        } catch (Exception e) {
            huaweiIotDaClient.handleException(e);
            return null;
        }
    }

    public ShowBatchTaskResponse queryTask(String taskId) {
        ShowBatchTaskRequest request = new ShowBatchTaskRequest();
        request.setInstanceId(huaweiIotDaClient.getIotDaConfig().instanceId());
        request.setTaskId(taskId);
        try {
            return huaweiIotDaClient.getIotDaClient().showBatchTask(request);
        } catch (Exception e) {
            huaweiIotDaClient.handleException(e);
            return null;
        }
    }
}
