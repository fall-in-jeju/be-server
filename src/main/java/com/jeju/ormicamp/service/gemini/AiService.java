package com.jeju.ormicamp.service.gemini;

public interface AiService {

    String invoke(String conversationId, String payload);
}
