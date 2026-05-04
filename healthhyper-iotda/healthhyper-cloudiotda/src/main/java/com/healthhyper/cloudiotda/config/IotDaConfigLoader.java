package com.healthhyper.cloudiotda.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

public class IotDaConfigLoader {
    private static final Logger logger = LoggerFactory.getLogger(IotDaConfigLoader.class);
    private static final String CONFIG_FILE = "application.yml";
    private static Map<String, Object> configMap;

    static {
        try (InputStream inputStream = IotDaConfigLoader.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (inputStream == null) {
                logger.warn("未找到配置文件 {}，将仅读取环境变量", CONFIG_FILE);
            }
            Yaml yaml = new Yaml();
            configMap = yaml.load(inputStream);
        } catch (Exception e) {
            logger.error("load config file error", e);
        }
    }

    public static IotDaConfig loadCoreConfig() {
        Map<String, Object> huaweiMap = (Map<String, Object>) configMap.get("huawei");
        Map<String, Object> iotdaMap = (Map<String, Object>) huaweiMap.get("iotda");

        String ak = (String) iotdaMap.get("ak");
        String sk = (String) iotdaMap.get("sk");
        String region = (String) iotdaMap.get("region");
        String iotdaEndpoint = (String) iotdaMap.get("iotdaEndpoint");
        String projectId = (String) iotdaMap.get("projectId");
        String instanceId = (String) iotdaMap.get("instanceId");
        String appId = (String) iotdaMap.get("appId");
        String productId = (String) iotdaMap.get("productId");
        String serviceId = (String) iotdaMap.get("serviceId");

        return new IotDaConfig(ak, sk, region, iotdaEndpoint, projectId, instanceId, appId, productId, serviceId);
    }

}
