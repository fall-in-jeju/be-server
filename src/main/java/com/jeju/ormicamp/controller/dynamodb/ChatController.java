package com.jeju.ormicamp.controller.dynamodb;

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
    @PostMapping("/{sessionId}")
    public ResponseEntity<String> saveMessage(
            @PathVariable String sessionId,
            @RequestBody ChatReqDto request) { // 1. ReqDto로 받음
        System.out.println("🚩 [Controller 도착] 요청 받음! sessionId: " + sessionId);
        // DTO에서 데이터를 꺼내서 서비스로 넘김
        // [임시 기능] 클라이언트가 "new"라고 보내면 서버가 랜덤 ID 생성
        if ("new".equalsIgnoreCase(sessionId)) {
            sessionId = UUID.randomUUID().toString();
            System.out.println("✨ [새 세션 생성] 임시 ID 발급: " + sessionId);
        }
        chatService.saveChatMessage(sessionId, request.getRole(), request.getContent());
        return ResponseEntity.ok(sessionId);
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<List<ChatResDto>> getChatHistory(@PathVariable String sessionId) { // 2. ResDto 리스트로 반환

        List<ChatResDto> history = chatService.getChatHistory(sessionId);
        return ResponseEntity.ok(history);
    }

    // (내부 클래스) 요청 받을 때 쓸 DTO
    @Data
    public static class ChatRequestDto {
        private String role;    // "USER" or "AI"
        private String content; // 메시지 내용
    }
}
