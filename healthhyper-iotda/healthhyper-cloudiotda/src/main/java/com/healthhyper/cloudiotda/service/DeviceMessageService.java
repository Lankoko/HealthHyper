package com.healthhyper.cloudiotda.service;

import com.healthhyper.cloudiotda.client.HuaweiIotDaClient;
import com.huaweicloud.sdk.iotda.v5.model.*;

public class DeviceMessageService {
    private final HuaweiIotDaClient huaweiIotDaClient;

    public DeviceMessageService(HuaweiIotDaClient huaweiIotDaClient) {
        this.huaweiIotDaClient = huaweiIotDaClient;
    }

    public CreateMessageResponse createMessage(String deviceId, DeviceMessageRequest body) {
        CreateMessageRequest request = new CreateMessageRequest();
        request.setDeviceId(deviceId);
        request.setBody(body);
        request.setInstanceId(huaweiIotDaClient.getIotDaConfig().instanceId());
        try {
            return huaweiIotDaClient.getIotDaClient().createMessage(request);
        } catch (Exception e) {
            huaweiIotDaClient.handleException(e);
            return null;
        }
    }

    public ShowDeviceMessageResponse queryMessageDetail(String deviceId, String messageId) {
        ShowDeviceMessageRequest request = new ShowDeviceMessageRequest();
        request.setDeviceId(deviceId);
        request.setMessageId(messageId);
        request.setInstanceId(huaweiIotDaClient.getIotDaConfig().instanceId());
        try {
            return huaweiIotDaClient.getIotDaClient().showDeviceMessage(request);
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

    public Task queryTask(String taskId) {
        ShowBatchTaskRequest request = new ShowBatchTaskRequest();
        request.setInstanceId(huaweiIotDaClient.getIotDaConfig().instanceId());
        request.setTaskId(taskId);
        ShowBatchTaskResponse showBatchTaskResponse = huaweiIotDaClient.getIotDaClient().showBatchTask(request);
        return showBatchTaskResponse.getBatchtask();
    }

    public ListDeviceMessagesResponse queryMessageList(String deviceId) {
        ListDeviceMessagesRequest request = new ListDeviceMessagesRequest();
        request.setDeviceId(deviceId);
        request.setInstanceId(huaweiIotDaClient.getIotDaConfig().instanceId());
        try {
            return huaweiIotDaClient.getIotDaClient().listDeviceMessages(request);
        } catch (Exception e) {
            huaweiIotDaClient.handleException(e);
            return null;
        }
    }
}