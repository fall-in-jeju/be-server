package com.jeju.ormicamp.service.dynamodb;

import com.jeju.ormicamp.infrastructure.repository.dynamoDB.ChatRepository;
import com.jeju.ormicamp.model.dynamodb.ChatEntity;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatService {

    private final ChatRepository repository;

    public ChatService(ChatRepository repository) {
        this.repository = repository;
    }

    // 시나리오 1: 새로운 채팅 메시지 저장
    public void saveChatMessage(String sessionId, String role, String content) {
        ChatEntity chat = new ChatEntity();

        // 핵심: 여기서 PK/SK 규칙을 적용합니다!
        chat.setPk("SESSION#" + sessionId);
        chat.setSk("MSG#" + LocalDateTime.now()); // 시간순 정렬

        chat.setType("CHAT");
        chat.setRole(role);
        chat.setContent(content);

        repository.save(chat);
    }

    // 시나리오 2: 내 여행 목록 가져오기
    public List<ChatEntity> getMySessions(String userId) {
        // PK 조립은 Repository가 하거나 여기서 넘겨주거나 선택
        return repository.findSessionsByUserId(userId);
    }

    // 시나리오 3: 채팅방 입장 (채팅+플래너 다 가져오기)
    public List<ChatEntity> enterChatRoom(String sessionId) {
        return repository.findAllInSession(sessionId);
    }
}