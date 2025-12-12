package com.jeju.ormicamp.controller.dynamodb;

import com.jeju.ormicamp.model.dynamodb.ChatEntity;
import com.jeju.ormicamp.service.dynamodb.ChatService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // --- [API 1] 메시지 저장 ---
    // 프론트엔드에서 유저가 메시지를 보냈을 때,
    // 혹은 AI API에서 응답을 받았을 때 호출
    @PostMapping("/{sessionId}")
    public ResponseEntity<String> saveChatMessage(
            @PathVariable String sessionId,
            @RequestBody ChatRequestDto request) {

        chatService.saveChatMessage(sessionId, request.getRole(), request.getContent());
        return ResponseEntity.ok("Saved successfully");
    }

    // --- [API 2] 채팅 내역 조회 ---
    // 채팅방에 처음 들어왔을 때 호출
    @GetMapping("/{sessionId}")
    public ResponseEntity<List<ChatEntity>> enterChatRoom(@PathVariable String sessionId) {
        List<ChatEntity> history = chatService.enterChatRoom(sessionId);
        return ResponseEntity.ok(history);
    }

    // (내부 클래스) 요청 받을 때 쓸 DTO
    @Data
    public static class ChatRequestDto {
        private String role;    // "USER" or "AI"
        private String content; // 메시지 내용
    }
}
