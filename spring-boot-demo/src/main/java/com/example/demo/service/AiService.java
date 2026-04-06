package com.example.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Slf4j
@Service
public class AiService {

    @Value("${app.ai.base-url}")
    private String aiBaseUrl;

    @Value("${app.ai.mock-enabled:false}")
    private boolean mockEnabled;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String[] MOCK_REPLY_PARTS = {
        "好的，", "我来帮您", "分析一下。\n\n",
        "根据您目前的健康数据，", "整体状况", "看起来不错。\n",
        "建议您：\n", "1. 保持规律作息\n", "2. 每天适量运动\n", "3. 注意饮食均衡\n\n",
        "如有需要，", "我可以为您", "制定一个健康计划。"
    };

    private static final String MOCK_PLAN_REPLY =
        "好的，根据您的情况，我为您制定了以下计划：\n\n" +
        "1. 每天步行30分钟\n2. 睡前1小时不看手机\n3. 每天喝水2000ml\n\n" +
        "---ACTIONS---\n" +
        "[{\"action\":\"create_plan\",\"data\":{\"title\":\"日常健康计划\",\"items\":[" +
        "{\"desc\":\"每天步行30分钟\",\"category\":\"exercise\"}," +
        "{\"desc\":\"睡前1小时不看手机\",\"category\":\"sleep\"}," +
        "{\"desc\":\"每天喝水2000ml\",\"category\":\"diet\"}]," +
        "\"days\":7}}]";

    /**
     * 流式调用云 AI 的 /chat/stream 接口。
     * 每收到一个 content 片段就调用 onContent，流结束后调用 onComplete。
     * 该方法是阻塞的，应在独立线程中调用。
     */
    public void streamChat(String prompt, String imgBase64,
                           Consumer<String> onContent, Runnable onComplete) {
        if (mockEnabled) {
            streamMock(prompt, onContent, onComplete);
            return;
        }

        try {
            Map<String, String> body = new HashMap<>();
            body.put("text", prompt);
            body.put("img_url", imgBase64 != null ? imgBase64 : "");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(aiBaseUrl + "/chat/stream"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .timeout(Duration.ofMinutes(3))
                    .build();

            HttpResponse<Stream<String>> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofLines());

            response.body().forEach(line -> {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) return;
                if ("[DONE]".equals(trimmed)) return;

                try {
                    JsonNode node = objectMapper.readTree(trimmed);
                    if (node.has("content")) {
                        String content = node.get("content").asText();
                        if (!content.isEmpty()) {
                            onContent.accept(content);
                        }
                    }
                } catch (Exception e) {
                    log.warn("解析AI响应行失败: {}", trimmed);
                }
            });

            onComplete.run();
        } catch (Exception e) {
            log.error("调用云AI失败", e);
            throw new RuntimeException("AI 服务暂时不可用", e);
        }
    }

    /**
     * 本地 mock 模式，模拟流式响应。
     * 若用户消息包含"计划"关键字，回复中附带 ACTIONS 指令，用于测试计划创建流程。
     */
    private void streamMock(String prompt, Consumer<String> onContent, Runnable onComplete) {
        log.info("[MOCK AI] 触发 mock 响应");
        boolean withPlan = prompt != null && (prompt.contains("计划") || prompt.contains("plan"));
        String[] parts = withPlan
                ? splitByChar(MOCK_PLAN_REPLY)
                : MOCK_REPLY_PARTS;

        for (String part : parts) {
            onContent.accept(part);
            try {
                Thread.sleep(80);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        onComplete.run();
    }

    private String[] splitByChar(String text) {
        String[] chars = new String[text.length()];
        for (int i = 0; i < text.length(); i++) {
            chars[i] = String.valueOf(text.charAt(i));
        }
        return chars;
    }
}
