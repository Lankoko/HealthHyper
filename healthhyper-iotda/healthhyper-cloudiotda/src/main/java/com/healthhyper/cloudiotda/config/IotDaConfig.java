package com.healthhyper.cloudiotda.config;

/*
 *             ak: 华为云账号ak
 *             sk: 华为云账号sk
 *         region: 华为云服务区域
 *  iotdaEndpoint: 华为云服务终端地址
 *      projectId: 华为云服务项目，与云服务区域相关。
 *     instanceId: 华为物联网平台提供实例助力解决数据和资源隔离等问题。
 *          appId: 资源空间从业务层面提供了独立的设备管理和平台配置能力。
 *                 作为业务管理的基本单元，在平台中创建的资源（如产品、设备等）都需要归属到某个资源空间中。
 *      productId: 在物联网平台中，某一类具有相同能力或特征的设备的合集被称为一款产品。
 *      serviceId: 描述设备具备的业务能力。将设备业务能力拆分成若干个服务后，再定义每个服务具备的属性、命令以及命令的参数。
 */
public record IotDaConfig(String ak, String sk, String region, String iotdaEndpoint,
                          String projectId, String instanceId, String appId,
                          String productId, String serviceId) {
}
