package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.common.UserContext;
import com.example.demo.dto.chat.ChatSendRequest;
import com.example.demo.entity.ChatMessage;
import com.example.demo.entity.ChatSession;
import com.example.demo.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "2. AI 对话", description = "会话管理 + SSE 流式发送消息")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/sessions")
    public Result<ChatSession> createSession() {
        return Result.ok(chatService.createSession(UserContext.get()));
    }

    @GetMapping("/sessions")
    public Result<List<ChatSession>> listSessions() {
        return Result.ok(chatService.listSessions(UserContext.get()));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public Result<Void> deleteSession(@PathVariable Long sessionId) {
        chatService.deleteSession(UserContext.get(), sessionId);
        return Result.ok();
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public Result<List<ChatMessage>> getMessages(@PathVariable Long sessionId) {
        return Result.ok(chatService.getMessages(UserContext.get(), sessionId));
    }

    @PostMapping(value = "/sessions/{sessionId}/send", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendMessage(@PathVariable Long sessionId,
                                  @RequestBody ChatSendRequest req) {
        SseEmitter emitter = new SseEmitter(120_000L);
        chatService.sendMessageAsync(UserContext.get(), sessionId, req, emitter);
        return emitter;
    }
}
