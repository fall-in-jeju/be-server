package com.jeju.ormicamp.model.dto.dynamodb;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatResDto {
    private String conversationId;
    private String message;
}
