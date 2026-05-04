package com.healthhyper.cloudiotda.client;

import com.healthhyper.cloudiotda.config.IotDaConfig;

import com.healthhyper.cloudiotda.exception.IotDaException;
import com.huaweicloud.sdk.core.auth.AbstractCredentials;
import com.huaweicloud.sdk.core.auth.BasicCredentials;
import com.huaweicloud.sdk.core.auth.ICredential;
import com.huaweicloud.sdk.core.exception.ConnectionException;
import com.huaweicloud.sdk.core.exception.RequestTimeoutException;
import com.huaweicloud.sdk.core.exception.ServiceResponseException;
import com.huaweicloud.sdk.core.region.Region;
import com.huaweicloud.sdk.iotda.v5.IoTDAClient;

public class HuaweiIotDaClient {
    private final IoTDAClient iotDaClient;
    private final IotDaConfig iotDaConfig;

    public HuaweiIotDaClient(IotDaConfig iotDaConfig) {
        this.iotDaConfig = iotDaConfig;

        ICredential auth = new BasicCredentials()
                .withProjectId(iotDaConfig.projectId())
                .withDerivedPredicate(AbstractCredentials.DEFAULT_DERIVED_PREDICATE)
                .withAk(iotDaConfig.ak())
                .withSk(iotDaConfig.sk());

        try {
            this.iotDaClient = IoTDAClient.newBuilder()
                    .withCredential(auth)
                    .withRegion(new Region(iotDaConfig.region(), iotDaConfig.iotdaEndpoint()))
                    .build();
        } catch (Exception e) {
            throw new IotDaException("初始化 huaweicloud IoTDA 客户端失败", e);
        }
    }

    public IoTDAClient getIotDaClient() {
        return iotDaClient;
    }

    public IotDaConfig getIotDaConfig() {
        return iotDaConfig;
    }

    public void handleException(Exception e) {
        if (e instanceof ServiceResponseException sre) {
            throw new IotDaException(
                    String.format("huawei IoTDA 服务错误：HTTP状态码=%d，错误码=%s，消息=%s",
                            sre.getHttpStatusCode(), sre.getErrorCode(), sre.getErrorMsg()),
                    sre.getErrorCode(),
                    sre
            );
        } else if (e instanceof ConnectionException) {
            throw new IotDaException("huawei IoTDA 连接失败：" + e.getMessage(), e);
        } else if (e instanceof RequestTimeoutException) {
            throw new IotDaException("huawei IoTDA 请求超时：" + e.getMessage(), e);
        } else {
            throw new IotDaException("huawei IoTDA 未知错误：" + e.getMessage(), e);
        }
    }
}
