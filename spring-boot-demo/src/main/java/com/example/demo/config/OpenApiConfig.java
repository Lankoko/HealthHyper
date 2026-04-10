package com.example.demo.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI healthHyperOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("HealthHyper 健康管家中台 API")
                        .description("基于 AI 的个人健康管家系统 —— 中台接口文档\n\n"
                                + "**认证方式**：\n"
                                + "- 手机端：`Authorization: Bearer <jwt_token>`\n"
                                + "- AI 端：`X-Api-Key` + `X-User-Id` Header")
                        .version("1.0.0"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Token", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("手机端登录后获取的 JWT Token"))
                        .addSecuritySchemes("AI Api Key", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Api-Key")
                                .description("AI 端调用中台接口的 API Key"))
                        .addSecuritySchemes("AI User Id", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-User-Id")
                                .description("AI 端指定操作的目标用户 ID")))
                .addSecurityItem(new SecurityRequirement()
                        .addList("Bearer Token")
                        .addList("AI Api Key")
                        .addList("AI User Id"));
    }
}
