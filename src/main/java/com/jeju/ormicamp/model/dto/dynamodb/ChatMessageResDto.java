package com.jeju.ormicamp.model.dto.dynamodb;

import com.jeju.ormicamp.model.code.ChatRole;
import com.jeju.ormicamp.model.domain.ChatEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageResDto {

    private ChatRole role;
    private String content;
    private String timestamp;

    public static ChatMessageResDto from(ChatEntity entity) {
        return ChatMessageResDto.builder()
                .role(entity.getRole())
                .content(entity.getPrompt())
                .timestamp(entity.getTimestamp())
                .build();
    }
}