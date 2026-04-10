package com.example.demo.service;

import com.example.demo.entity.ChatMessage;
import com.example.demo.entity.ChatSession;
import com.example.demo.entity.HealthAlert;
import com.example.demo.mapper.ChatMessageMapper;
import com.example.demo.mapper.ChatSessionMapper;
import com.example.demo.mapper.HealthAlertMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemTriggerService {

    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final HealthAlertMapper healthAlertMapper;
    private final AiService aiService;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * 触发机制 1：档案变更 → AI 摘要更新。
     * health_profile 变更后包装成对话，AI 收到后用 conversation_summary skill 更新摘要。
     */
    public void triggerSummaryUpdate(Long userId, String profileSnapshot) {
        executor.execute(() -> {
            try {
                ChatSession session = createSystemSession(userId, "system_summary", "档案变更通知");

                String text = "【系统通知-档案变更】用户健康档案已更新，当前完整档案如下：\n"
                        + profileSnapshot
                        + "\n请根据最新档案信息更新健康摘要。";
                saveMessage(session.getId(), "user", text);
                callAiAndSaveResponse(userId, session, text);

                log.info("触发AI摘要更新完成: userId={}, sessionId={}", userId, session.getId());
            } catch (Exception e) {
                log.warn("触发AI摘要更新失败: userId={}", userId, e);
            }
        });
    }

    /**
     * 触发机制 2：异常告警 → AI 主动对话。
     * severity > 1 时创建对话会话，用户端显示为 AI 主动发起的聊天。
     */
    public void triggerAlertChat(Long userId, HealthAlert alert) {
        executor.execute(() -> {
            try {
                ChatSession session = createSystemSession(userId, "system_alert", "健康异常提醒");
                session.setIsRead(0);
                chatSessionMapper.updateById(session);

                String text = "【系统通知-健康异常】" + alert.getMessage()
                        + "\n严重程度: " + alert.getSeverity() + "/3"
                        + "\n请分析该异常数据，给出健康建议，并主动关心用户。";
                saveMessage(session.getId(), "user", text);
                callAiAndSaveResponse(userId, session, text);

                alert.setTriggeredSessionId(session.getId());
                healthAlertMapper.updateById(alert);

                log.info("触发AI异常对话完成: userId={}, alertId={}, sessionId={}",
                        userId, alert.getId(), session.getId());
            } catch (Exception e) {
                log.warn("触发AI异常对话失败: userId={}, alertId={}", userId,
                        alert != null ? alert.getId() : null, e);
            }
        });
    }

    private ChatSession createSystemSession(Long userId, String source, String title) {
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setSource(source);
        session.setTitle(title);
        session.setIsRead(1);
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        chatSessionMapper.insert(session);
        return session;
    }

    private void saveMessage(Long sessionId, String role, String content) {
        ChatMessage msg = new ChatMessage();
        msg.setSessionId(sessionId);
        msg.setRole(role);
        msg.setMsgType("text");
        msg.setContent(content);
        msg.setCreatedAt(LocalDateTime.now());
        chatMessageMapper.insert(msg);
    }

    private void callAiAndSaveResponse(Long userId, ChatSession session, String text) {
        StringBuilder fullResponse = new StringBuilder();
        aiService.streamChat(text, null, userId, session.getId(),
                fullResponse::append,
                () -> {
                    if (!fullResponse.isEmpty()) {
                        saveMessage(session.getId(), "assistant", fullResponse.toString());
                    }
                    session.setUpdatedAt(LocalDateTime.now());
                    chatSessionMapper.updateById(session);
                });
    }
}
