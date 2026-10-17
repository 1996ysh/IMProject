package com.shanyangcode.model.dto;

import lombok.Data;

@Data
public class ChatRequest {
    public Long userId;
    public String prompt;
    public Long sessionId;
}
