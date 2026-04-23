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
    private final ObjectMapper objectMapper;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    /** @deprecated AI 已切换为 Skill Tool 直接调中台 API，ACTIONS 仅作兼容保留 */
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
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

    public void deleteSession(Long userId, Long sessionId) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BusinessException("会话不存在");
        }
        chatMessageMapper.delete(
                new LambdaQueryWrapper<ChatMessage>().eq(ChatMessage::getSessionId, sessionId));
        chatSessionMapper.deleteById(sessionId);
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

        // 2. AI 那边自己管多用户多标签页记忆，中台只传用户原始消息
        //    健康上下文（档案/摘要）可在首轮时作为前缀注入，后续由 AI 侧记忆维护
        String textToSend = req.getText();

        // 3. 流式调用 AI 并透传给手机
        StringBuilder fullResponse = new StringBuilder();

        aiService.streamChat(textToSend, req.getImgBase64(), userId, sessionId,
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

    // ==================== 上下文辅助 ====================

    /**
     * 获取用户健康上下文摘要（供后续首轮注入或 AI 工具调用参考）。
     * 当前联调阶段 AI 侧自行维护记忆，此方法暂不在发消息流程中调用。
     */
    @SuppressWarnings("unused")
    private String buildHealthContext(Long userId) {
        StringBuilder sb = new StringBuilder();

        AiHealthSummary summary = aiHealthSummaryMapper.selectOne(
                new LambdaQueryWrapper<AiHealthSummary>()
                        .eq(AiHealthSummary::getUserId, userId));
        if (summary != null && summary.getSummaryJson() != null) {
            sb.append("## 用户健康概况\n").append(summary.getSummaryJson()).append("\n\n");
        }

        HealthProfile profile = healthProfileMapper.selectOne(
                new LambdaQueryWrapper<HealthProfile>()
                        .eq(HealthProfile::getUserId, userId));
        if (profile != null) {
            sb.append("## 基本信息\n");
            if (profile.getGender() != null)
                sb.append("性别: ").append(profile.getGender() == 1 ? "男" : "女").append("\n");
            if (profile.getHeightCm() != null)
                sb.append("身高: ").append(profile.getHeightCm()).append("cm\n");
            if (profile.getWeightKg() != null)
                sb.append("体重: ").append(profile.getWeightKg()).append("kg\n");
            if (profile.getMedicalHistory() != null)
                sb.append("病史: ").append(profile.getMedicalHistory()).append("\n");
            if (profile.getAllergyInfo() != null)
                sb.append("过敏: ").append(profile.getAllergyInfo()).append("\n");
        }

        return sb.toString();
    }

    // ==================== ACTIONS 处理（遗留兼容，AI 已切换 Skill Tool） ====================

    @Deprecated
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

    @Deprecated
    private void handleCreatePlan(Long userId, Long sessionId, Map<String, Object> data) {
        try {
            HealthPlan plan = new HealthPlan();
            plan.setUserId(userId);
            plan.setTitle((String) data.getOrDefault("title", "健康计划"));
            plan.setItemsJson(objectMapper.writeValueAsString(data.get("items")));
            plan.setSource("ai");
            plan.setChatSessionId(sessionId);
            plan.setStatus(0);
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
