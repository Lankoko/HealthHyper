package com.example.demo.dto.chat;

import lombok.Data;

@Data
public class ChatSendRequest {
    private String text;
    private String imgBase64;
}
