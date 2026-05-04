# 面向 AI 工具的集成说明：把 `healthhyper-cloudiotda` 接入你的 Spring Boot 项目

> 目标：让 AI 工具在你的“另一个 Spring Boot 项目”里，**快速完成依赖引入 + 配置绑定 + Bean 注册 + 示例调用**，从而直接使用华为云 IoTDA 的查询/下发能力。

---

## 你要对 AI 工具说什么（推荐直接复制粘贴）

把下面这段话发给你的 AI 工具，并按需补充“你的项目路径/模块名”：

> 我有一个 Java 模块 `healthhyper-cloudiotda`（包名 `com.healthhyper.cloudiotda`），里面封装了华为云 IoTDA 的 Java SDK 调用（核心类 `HuaweiIotDaClient`，配置 record `IotDaConfig`，服务类在 `com.healthhyper.cloudiotda.service.*`）。
>
> 请你把它集成到我当前 Spring Boot 项目中，要求：
> - 用 Maven 引入该模块（本地 install 或发布到私服均可，优先本地 install）。
> - 在我的 Spring Boot 项目里新增 `@ConfigurationProperties` 来承接配置 `huawei.iotda.*`，并注册这些 Bean：
>   - `IotDaConfig`
>   - `HuaweiIotDaClient`
>   - `DeviceService`、`SpaceService`、`DevicePropertyService`、`DeviceShadowService`、`DeviceCommandService`、`DeviceMessageService`
> - 给一个最小可运行的示例：写一个 `@RestController`（或 `CommandLineRunner`）演示 `listDevices`/`showDevice` 调用。
> - 如果依赖冲突（例如 `slf4j-simple` 和 Spring Boot 默认 logback 冲突），请用 Maven exclude 处理。

---

## 集成方式 A（推荐）：先本地安装模块，再作为依赖引入

### 1）在本仓库执行本地安装

在 `healthhyper-iot` 根目录执行：

```bash
mvn -pl healthhyper-cloudiotda -am clean install
```

安装成功后，你本机 Maven 仓库会有：
- `com.healthhyper:healthhyper-cloudiotda:1.0-SNAPSHOT`

> 注意：该模块 POM 里明确依赖了 `huaweicloud-sdk-iotda`、`huaweicloud-sdk-core`。

### 2）在“你的 Spring Boot 项目”的 `pom.xml` 加依赖

在你的 Spring Boot 项目 `pom.xml` 增加：

```xml
<dependency>
  <groupId>com.healthhyper</groupId>
  <artifactId>healthhyper-cloudiotda</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
```

#### 如果你遇到日志冲突（很常见）

`healthhyper-cloudiotda` 依赖了 `slf4j-simple`，而 Spring Boot 默认用 logback。
如果启动报错类似 “multiple SLF4J bindings”，让 AI 工具做以下改动（推荐保留 Spring Boot 的 logback）：

```xml
<dependency>
  <groupId>com.healthhyper</groupId>
  <artifactId>healthhyper-cloudiotda</artifactId>
  <version>1.0-SNAPSHOT</version>
  <exclusions>
    <exclusion>
      <groupId>org.slf4j</groupId>
      <artifactId>slf4j-simple</artifactId>
    </exclusion>
  </exclusions>
</dependency>
```

---

## 集成方式 B：把模块源码直接拷贝/合并进你的项目（不推荐，但也能用）

适合不想走 Maven install 的情况。让 AI 工具把 `healthhyper-cloudiotda/src/main/java` 和 `src/main/resources/application.yml` 的配置段迁移到你的项目里，并按下面的“Spring Boot 配置绑定”做适配。

---

## 你在 Spring Boot 项目里需要提供的配置（`application.yml`）

把下列配置放到你项目的 `application.yml`（值按你的环境填写）：

```yaml
huawei:
  iotda:
    ak: "你的AK"
    sk: "你的SK"
    region: "cn-north-4"
    iotdaEndpoint: "xxxx.iotda-app.cn-north-4.myhuaweicloud.com"
    projectId: "你的projectId"
    instanceId: "你的instanceId"
    appId: "你的appId"
    productId: "你的productId"
    serviceId: "你的serviceId"
```

> 说明：该模块仓库自带的 `IotDaConfigLoader` 是“自己读 classpath 下的 `application.yml`”，但在 Spring Boot 项目中，更推荐走 **`@ConfigurationProperties` 自动绑定**（下面有模板）。

---

## 在你的 Spring Boot 项目里新增这些类（让 AI 工具生成）

### 1）配置承接类：`IotDaProperties`

让 AI 工具新增一个 `@ConfigurationProperties(prefix="huawei.iotda")` 的类（字段包括：`ak/sk/region/iotdaEndpoint/projectId/instanceId/appId/productId/serviceId`）。

### 2）自动装配：`IotDaAutoConfiguration`

让 AI 工具新增一个 `@Configuration`，并声明以下 Bean：
- `IotDaConfig`（从 `IotDaProperties` 转成 record `IotDaConfig`）
- `HuaweiIotDaClient`
- `DeviceService`
- `SpaceService`
- `DevicePropertyService`
- `DeviceShadowService`
- `DeviceCommandService`
- `DeviceMessageService`

### 3）最小可运行示例（两选一）

#### 选项 A：HTTP 接口示例（推荐）

让 AI 工具写一个 `@RestController`，注入 `DeviceService`，提供两个接口：
- `GET /iotda/devices?limit=10&offset=0` → 调 `listDevices`
- `GET /iotda/devices/{deviceId}` → 调 `showDevice`

#### 选项 B：启动即跑（适合快速验证）

让 AI 工具写一个 `CommandLineRunner`（或 `ApplicationRunner`），启动时调用一次 `listDevices(10,0)` 并打印结果。

---

## 常见问题（让 AI 工具优先按这些方向排查）

- **启动报 SLF4J 绑定冲突**：按上面 Maven exclusion 移除 `slf4j-simple`。
- **请求返回 401/403**：AK/SK 或 projectId/region/endpoint 不匹配。
- **返回 404 或资源不存在**：`instanceId/appId/productId/deviceId/serviceId` 填错或不属于该项目/空间。
- **网络问题/超时**：检查 endpoint 是否正确、公司网络/代理、防火墙。
- **空指针**：你的配置没绑定上（`@ConfigurationProperties` 未启用或 prefix 写错）。

---

## 该模块里你可以直接用的能力（类名速查）

- **设备**：`com.healthhyper.cloudiotda.service.DeviceService`
- **资源空间**：`com.healthhyper.cloudiotda.service.SpaceService`
- **设备属性**：`com.healthhyper.cloudiotda.service.DevicePropertyService`
- **设备影子**：`com.healthhyper.cloudiotda.service.DeviceShadowService`
- **下发命令**：`com.healthhyper.cloudiotda.service.DeviceCommandService`
- **设备消息**：`com.healthhyper.cloudiotda.service.DeviceMessageService`

