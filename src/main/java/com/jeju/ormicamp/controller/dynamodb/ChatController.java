package com.jeju.ormicamp.controller.dynamodb;

import com.jeju.ormicamp.common.customUserDetail.UserPrincipal;
import com.jeju.ormicamp.common.dto.BaseResponse;
import com.jeju.ormicamp.model.dto.dynamodb.ChatReqDto;
import com.jeju.ormicamp.model.dto.dynamodb.ChatResDto;
import com.jeju.ormicamp.service.dynamodb.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/sessions")
    public ResponseEntity<BaseResponse<ChatResDto>> startChat(
            @RequestBody ChatReqDto req,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        Long userId = user.userId();
        ChatResDto result = chatService.startChat(req,userId);
        return ResponseEntity.ok()
                .body(BaseResponse.success("대화 시작", result));
    }

    @PostMapping("/messages")
    public ResponseEntity<BaseResponse<ChatResDto>> chatMessage(
            @RequestBody ChatReqDto req,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        Long userId = user.userId();
        ChatResDto result = chatService.sendMessage(req,userId);
        return ResponseEntity.ok()
                .body(BaseResponse.success("소통중..", result));
    }
}
