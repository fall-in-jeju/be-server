package com.jeju.ormicamp.service.gemini;

import com.jeju.ormicamp.common.customUserDetail.UserPrincipal;
import com.jeju.ormicamp.common.dto.BaseResponse;
import com.jeju.ormicamp.model.dto.dynamodb.ChatConversationResDto;
import com.jeju.ormicamp.model.dto.dynamodb.ChatReqDto;
import com.jeju.ormicamp.model.dto.dynamodb.ChatResDto;
import com.jeju.ormicamp.model.dto.dynamodb.PlanDayResDto;
import com.jeju.ormicamp.service.dynamodb.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatGeminiController {

    private final ChatGeminiService chatGeminiService;

    @PostMapping("/sessions/gemini")
    public ResponseEntity<BaseResponse<ChatResDto>> startChat(
            @RequestBody ChatReqDto req,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        Long userId = user.userId();
        ChatResDto result = chatGeminiService.startChat(req, userId);
        return ResponseEntity.ok()
                .body(BaseResponse.success("대화 시작", result));
    }

    @PostMapping("/sessions/{conversationId}/gemini/messages")
    public ResponseEntity<BaseResponse<ChatResDto>> chatMessage(
            @PathVariable String conversationId,
            @RequestBody ChatReqDto req,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        Long userId = user.userId();
        String content = req.getContent();
        ChatResDto result = chatGeminiService.sendMessage(conversationId, content, userId);
        return ResponseEntity.ok()
                .body(BaseResponse.success("소통중..", result));
    }

    // 채팅방 조회
    @GetMapping("/sessions/gemini/{conversationId}")
    public ResponseEntity<BaseResponse<ChatConversationResDto>> getConversation(
            @PathVariable String conversationId
    ) {
        return ResponseEntity.ok(
                BaseResponse.success(
                        "채팅 조회 성공",
                        chatGeminiService.getConversation(conversationId)
                )
        );
    }

    // 날짜별 플래너 조회
    @GetMapping("/sessions/gemini/{conversationId}/plans/{date}")
    public ResponseEntity<BaseResponse<List<PlanDayResDto>>> getPlansByDate(
            @PathVariable String conversationId,
            @PathVariable String date
    ) {
        return ResponseEntity.ok(
                BaseResponse.success(
                        "날짜별 플래너 조회 성공",
                        chatGeminiService.getPlansByDate(conversationId, date)
                )
        );
    }
}

