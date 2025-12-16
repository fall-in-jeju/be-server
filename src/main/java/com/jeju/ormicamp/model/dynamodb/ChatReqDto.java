package com.jeju.ormicamp.model.dynamodb;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor // 기본 생성자 (JSON 파싱할 때 필요)
public class ChatReqDto {

    private String sessionId;
    private String role;    // "USER" 또는 "AI"
    private String content; // "제주도 맛집 알려줘"

    // (참고) sessionId는 주소창(URL)에 있어서 여기엔 안 넣어도 됩니다.
    // (참고) timestamp는 서버가 만들 거라 여기엔 없습니다.
}