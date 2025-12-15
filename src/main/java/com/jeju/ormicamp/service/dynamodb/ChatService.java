package com.jeju.ormicamp.service.dynamodb;

import com.jeju.ormicamp.common.exception.CustomException;
import com.jeju.ormicamp.common.exception.ErrorCode;
import com.jeju.ormicamp.infrastructure.repository.dynamoDB.ChatRepository;
import com.jeju.ormicamp.model.dynamodb.ChatEntity;
import com.jeju.ormicamp.model.dynamodb.ChatResDto;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
public class ChatService {

    private final ChatRepository repository;

    public ChatService(ChatRepository repository) {
        this.repository = repository;
    }

    /* 시나리오 0: 새로운 채팅방 만들기 (목록에 추가)
     * @param userId 방을 만드는 사람 ID (로그인한 유저)
     * @return 새로 생성된 방 번호 (sessionId)
     */
    public String createSession(String userId) {
        // 1. 고유한 방 번호표(UUID) 발급 -> 추후 AI 에이전트 세션 ID 가져오는 것으로 변경
        String newSessionId = UUID.randomUUID().toString();
        String now = LocalDateTime.now().toString();

        // 2. 엔티티 생성 (목록 저장용)
        ChatEntity metaItem = new ChatEntity();

        // ⭐️ 핵심: PK는 유저 ID, SK는 생성 시간 (목록 정렬용)
        metaItem.setPk("USER#" + userId);
        metaItem.setSk("SESSION#" + now);

        // 3. 메타 데이터 채우기
        metaItem.setType("META"); // 이건 채팅(CHAT)이 아니라 목록(META) 정보임
        metaItem.setSessionId(newSessionId); // 이 방의 진짜 주소를 저장해둠
        metaItem.setSessionTitle("새로운 제주 여행 " + now.substring(0, 10)); // 제목 (나중에 수정 가능)

        // 4. 저장!
        repository.save(metaItem);

        // 5. 방 번호를 리턴해줘야 프론트가 그 방으로 이동(Redirect)할 수 있음
        return newSessionId;
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

        if (userId == null) {
            throw new CustomException(ErrorCode.CHAT_SESSION_MISSING);
        }
        // PK 조립은 Repository가 하거나 여기서 넘겨주거나 선택
        return repository.findSessionsByUserId(userId);
    }

    // 시나리오 3: 채팅 내역 가져오기
    // DTO 리스트로 리턴
    public List<ChatResDto> getChatHistory(String sessionId) {

        // 1. DB에서 일단 다 가져옵니다. (여기까진 Entity 상태)
        List<ChatEntity> entities = repository.findAllInSession(sessionId);

        if (entities.isEmpty()) {
            throw new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND);
        }
        // 2. 변환 작업(Mapping)을 수행
        return entities.stream()
                // 혹시 플래너 데이터(PLAN)가 섞여 있을 수 있으니 "CHAT"만 거릅니다.
                .filter(item -> "CHAT".equals(item.getType()))
                // ChatResDto의 from 메서드를 통해 변환합니다.
                .map(ChatResDto::from)
                .collect(Collectors.toList());
    }
}