package com.jeju.ormicamp.model.dynamodb;

import com.jeju.ormicamp.model.dynamodb.ChatEntity; // 우리가 만든 엔티티
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ChatResDto {

    private String role;      // "USER"
    private String content;   // "제주도 맛집 알려줘"
    private String timestamp; // "2025-12-12T10:00:00" (화면에 보여줄 시간)

    // ⭐️ 핵심: Entity -> DTO 변환기 (Factory Method)
    public static ChatResDto from(ChatEntity item) {
        return ChatResDto.builder()
                .role(item.getRole())
                .content(item.getContent())
                // SK가 "MSG#2025-..." 형태니까 앞의 "MSG#" 4글자를 잘라내고 날짜만 줍니다.
                .timestamp(item.getSk().substring(4))
                .build();
    }
}