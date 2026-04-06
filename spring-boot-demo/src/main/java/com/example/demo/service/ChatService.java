package com.example.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.common.BusinessException;
import com.example.demo.dto.chat.ChatSendRequest;
import com.example.demo.entity.*;
import com.example.demo.mapper.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final AiHealthSummaryMapper aiHealthSummaryMapper;
    private final HealthProfileMapper healthProfileMapper;
    private final HealthPlanMapper healthPlanMapper;
    private final AiService aiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ExecutorService executor = Executors.newCachedThreadPool();

    private static final String ACTIONS_MARKER = "---ACTIONS---";

    // ==================== 会话管理 ====================

    public ChatSession createSession(Long userId) {
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setSource("user");
        session.setIsRead(1);
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        chatSessionMapper.insert(session);
        return session;
    }

    public List<ChatSession> listSessions(Long userId) {
        return chatSessionMapper.selectList(
                new LambdaQueryWrapper<ChatSession>()
                        .eq(ChatSession::getUserId, userId)
                        .orderByDesc(ChatSession::getUpdatedAt));
    }

    public List<ChatMessage> getMessages(Long userId, Long sessionId) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BusinessException("会话不存在");
        }
        return chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .orderByAsc(ChatMessage::getCreatedAt));
    }

    // ==================== 发送消息 + 流式响应 ====================

    public void sendMessageAsync(Long userId, Long sessionId,
                                 ChatSendRequest req, SseEmitter emitter) {
        executor.execute(() -> {
            try {
                doSendMessage(userId, sessionId, req, emitter);
            } catch (Exception e) {
                log.error("发送消息失败", e);
                try {
                    emitter.send(SseEmitter.event().name("error").data("AI 服务暂时不可用"));
                } catch (IOException ignored) {}
                emitter.completeWithError(e);
            }
        });
    }

    private void doSendMessage(Long userId, Long sessionId,
                               ChatSendRequest req, SseEmitter emitter) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BusinessException("会话不存在");
        }

        // 1. 存用户消息
        ChatMessage userMsg = new ChatMessage();
        userMsg.setSessionId(sessionId);
        userMsg.setRole("user");
        userMsg.setMsgType(req.getImgBase64() != null ? "image_text" : "text");
        userMsg.setContent(req.getText() != null ? req.getText() : "[图片]");
        userMsg.setCreatedAt(LocalDateTime.now());
        chatMessageMapper.insert(userMsg);

        // 2. 组装 prompt
        String prompt = buildPrompt(userId, sessionId, req.getText());

        // 3. 流式调用 AI 并透传给手机
        StringBuilder fullResponse = new StringBuilder();

        aiService.streamChat(prompt, req.getImgBase64(),
                content -> {
                    fullResponse.append(content);
                    try {
                        emitter.send(SseEmitter.event().data(content));
                    } catch (IOException e) {
                        log.warn("SSE 发送失败，客户端可能已断开");
                    }
                },
                () -> {
                    // 4. 流结束，处理完整响应
                    String response = fullResponse.toString();
                    String visibleText = response;
                    String actionsJson = null;

                    int actionsIdx = response.indexOf(ACTIONS_MARKER);
                    if (actionsIdx >= 0) {
                        visibleText = response.substring(0, actionsIdx).trim();
                        actionsJson = response.substring(actionsIdx + ACTIONS_MARKER.length()).trim();
                    }

                    // 存 AI 回复（只存可见文本）
                    ChatMessage aiMsg = new ChatMessage();
                    aiMsg.setSessionId(sessionId);
                    aiMsg.setRole("assistant");
                    aiMsg.setMsgType("text");
                    aiMsg.setContent(visibleText);
                    aiMsg.setCreatedAt(LocalDateTime.now());
                    chatMessageMapper.insert(aiMsg);

                    // 更新会话时间和标题
                    session.setUpdatedAt(LocalDateTime.now());
                    if (session.getTitle() == null || session.getTitle().isBlank()) {
                        String title = req.getText();
                        if (title != null && title.length() > 30) {
                            title = title.substring(0, 30) + "...";
                        }
                        session.setTitle(title);
                    }
                    chatSessionMapper.updateById(session);

                    // 执行 ACTIONS
                    if (actionsJson != null) {
                        processActions(userId, sessionId, actionsJson);
                    }

                    emitter.complete();
                });
    }

    // ==================== Prompt 组装 ====================

    private String buildPrompt(Long userId, Long sessionId, String userMessage) {
        StringBuilder sb = new StringBuilder();

        // 系统指令
        sb.append("你是一位专业的个人健康管家AI助手。请基于用户的健康数据和对话内容，提供个性化的健康分析和建议。\n\n");
        sb.append("规则：\n");
        sb.append("1. 回答应基于用户的实际健康数据，不要凭空编造数据\n");
        sb.append("2. 涉及严重健康问题时，建议用户及时就医，不要替代医生诊断\n");
        sb.append("3. 语气亲切但专业\n");
        sb.append("4. 如果你认为需要为用户创建健康计划，请在回复末尾附上结构化指令\n");
        sb.append("5. 如果用户发送了医疗报告图片，请提取其中的关键信息并结构化整理\n\n");
        sb.append("当你需要创建健康计划时，在回复的可见文本之后，另起一行以 ---ACTIONS--- 开头，用 JSON 数组描述操作：\n");
        sb.append("---ACTIONS---\n");
        sb.append("[{\"action\": \"create_plan\", \"data\": {\"title\": \"计划标题\", \"items\": [{\"desc\": \"具体事项\", \"category\": \"分类\"}], \"days\": 7}}]\n\n");

        // 健康摘要
        AiHealthSummary summary = aiHealthSummaryMapper.selectOne(
                new LambdaQueryWrapper<AiHealthSummary>()
                        .eq(AiHealthSummary::getUserId, userId));
        if (summary != null && summary.getSummaryJson() != null) {
            sb.append("## 当前用户健康概况\n");
            sb.append(summary.getSummaryJson()).append("\n\n");
        }

        // 基本档案
        HealthProfile profile = healthProfileMapper.selectOne(
                new LambdaQueryWrapper<HealthProfile>()
                        .eq(HealthProfile::getUserId, userId));
        if (profile != null) {
            sb.append("## 用户基本信息\n");
            if (profile.getGender() != null) {
                sb.append("性别: ").append(profile.getGender() == 1 ? "男" : "女").append("\n");
            }
            if (profile.getHeightCm() != null) sb.append("身高: ").append(profile.getHeightCm()).append("cm\n");
            if (profile.getWeightKg() != null) sb.append("体重: ").append(profile.getWeightKg()).append("kg\n");
            if (profile.getMedicalHistory() != null) sb.append("病史: ").append(profile.getMedicalHistory()).append("\n");
            if (profile.getAllergyInfo() != null) sb.append("过敏: ").append(profile.getAllergyInfo()).append("\n");
            sb.append("\n");
        }

        // 对话历史（最近10条）
        List<ChatMessage> history = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .orderByDesc(ChatMessage::getCreatedAt)
                        .last("LIMIT 10"));
        if (!history.isEmpty()) {
            sb.append("## 对话历史\n");
            for (int i = history.size() - 1; i >= 0; i--) {
                ChatMessage msg = history.get(i);
                String roleLabel = "user".equals(msg.getRole()) ? "用户" : "助手";
                sb.append(roleLabel).append(": ").append(msg.getContent()).append("\n");
            }
            sb.append("\n");
        }

        // 本次用户消息
        sb.append("## 用户消息\n");
        sb.append(userMessage != null ? userMessage : "");

        return sb.toString();
    }

    // ==================== ACTIONS 处理 ====================

    private void processActions(Long userId, Long sessionId, String actionsJson) {
        try {
            List<Map<String, Object>> actions = objectMapper.readValue(
                    actionsJson, new TypeReference<>() {});

            for (Map<String, Object> action : actions) {
                String type = (String) action.get("action");
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) action.get("data");

                if ("create_plan".equals(type) && data != null) {
                    handleCreatePlan(userId, sessionId, data);
                }
            }
        } catch (Exception e) {
            log.warn("解析 ACTIONS 失败: {}", actionsJson, e);
        }
    }

    private void handleCreatePlan(Long userId, Long sessionId, Map<String, Object> data) {
        try {
            HealthPlan plan = new HealthPlan();
            plan.setUserId(userId);
            plan.setTitle((String) data.getOrDefault("title", "健康计划"));
            plan.setItemsJson(objectMapper.writeValueAsString(data.get("items")));
            plan.setSource("ai");
            plan.setChatSessionId(sessionId);
            plan.setStatus(1);
            plan.setStartDate(LocalDate.now());

            Object days = data.get("days");
            if (days instanceof Number) {
                plan.setEndDate(LocalDate.now().plusDays(((Number) days).longValue()));
            }

            plan.setCreatedAt(LocalDateTime.now());
            healthPlanMapper.insert(plan);
            log.info("AI 创建健康计划: userId={}, planId={}", userId, plan.getId());
        } catch (Exception e) {
            log.warn("创建健康计划失败", e);
        }
    }
}
