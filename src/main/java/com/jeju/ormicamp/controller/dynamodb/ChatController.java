package com.jeju.ormicamp.controller.dynamodb;

import com.jeju.ormicamp.common.dto.BaseResponse;
import com.jeju.ormicamp.model.dynamodb.ChatReqDto;
import com.jeju.ormicamp.model.dynamodb.ChatResDto;
import com.jeju.ormicamp.service.dynamodb.ChatService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // --- [API 1] 메시지 저장 ---
    // 프론트엔드에서 유저가 메시지를 보냈을 때,
    // 혹은 AI API에서 응답을 받았을 때 호출
    @PostMapping
    public ResponseEntity<BaseResponse<String>> saveMessage(
            @RequestBody ChatReqDto request) { // 1. ReqDto로 받음
        String sessionId = request.getSessionId();
        // DTO에서 데이터를 꺼내서 서비스로 넘김
        // 프론트에서 ID를 안 보냈다면? (첫 채팅) -> 서버가 새로 생성
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
            System.out.println(" [새 세션 시작] ID 발급: " + sessionId);
        } else {
            System.out.println(" [대화 이어하기] ID: " + sessionId);
        }
        chatService.saveChatMessage(sessionId, request.getRole(), request.getContent());
        return ResponseEntity.ok(
                BaseResponse.success("대화 시작", sessionId)
        );
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<BaseResponse<List<ChatResDto>>> getChatHistory(@PathVariable String sessionId) { // 2. ResDto 리스트로 반환

        List<ChatResDto> history = chatService.getChatHistory(sessionId);
        return ResponseEntity.ok(
                BaseResponse.success("채팅 내역 조회 성공", history)
        );
    }

    // (내부 클래스) 요청 받을 때 쓸 DTO
    @Data
    public static class ChatRequestDto {
        private String sessionId;
        private String role;    // "USER" or "AI"
        private String content; // 메시지 내용
    }
}
