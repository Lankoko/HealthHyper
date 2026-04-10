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
     * 流式调用云 AI 的接口。
     *
     * 请求格式（POST /api/v1/chat/stream）：
     *   { "user_id": "2", "thread_id": "5", "text": "...", "image_url": "data:image/jpeg;base64,..." }
     *   注：image_url 无图时整个字段不传
     *
     * 响应格式（SSE）：
     *   event: status\ndata: {...}   → 状态行，跳过
     *   data: {"content": "..."}     → 内容片段
     *   data: {}                     → 结束标志
     *
     * @param text      用户消息文本（可含健康上下文前缀）
     * @param imgBase64 图片 base64（含 data:image/jpeg;base64, 前缀），无图传 null
     * @param userId    中台用户 ID，透传给 AI 做多用户隔离
     * @param sessionId 中台会话 ID，作为 thread_id（全局唯一，不同用户的 session 不重复）
     */
    public void streamChat(String text, String imgBase64, Long userId, Long sessionId,
                           Consumer<String> onContent, Runnable onComplete) {
        if (mockEnabled) {
            streamMock(text, onContent, onComplete);
            return;
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("user_id", userId.toString());
            body.put("thread_id", sessionId.toString());
            body.put("text", text != null ? text : "");
            if (imgBase64 != null && !imgBase64.isBlank()) {
                body.put("image_url", imgBase64);
            }
            // 无图时不传 image_url 字段（队友要求）

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(aiBaseUrl + "/api/v1/chat/stream"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .timeout(Duration.ofMinutes(3))
                    .build();

            HttpResponse<Stream<String>> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofLines());

            response.body().forEach(line -> {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) return;

                // event: 开头的行（如 event: status）直接跳过
                if (trimmed.startsWith("event:")) return;

                // data: 开头的行
                if (trimmed.startsWith("data:")) {
                    String dataStr = trimmed.substring(5).trim();
                    if (dataStr.isEmpty()) return;

                    try {
                        JsonNode node = objectMapper.readTree(dataStr);
                        // {} 是结束标志，不处理内容
                        if (node.isEmpty()) return;

                        if (node.has("content")) {
                            String content = node.get("content").asText();
                            if (!content.isEmpty()) {
                                onContent.accept(content);
                            }
                        }
                    } catch (Exception e) {
                        log.warn("解析AI响应行失败: {}", trimmed);
                    }
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
    private void streamMock(String text, Consumer<String> onContent, Runnable onComplete) {
        log.info("[MOCK AI] 触发 mock 响应");
        boolean withPlan = text != null && (text.contains("计划") || text.contains("plan"));
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
