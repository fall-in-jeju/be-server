package com.jeju.ormicamp.model.dto.dynamodb;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatReqDto {
    private String conversationId;
    private String chatTitle;
    private String content;
}