package com.jeju.ormicamp;

import com.jeju.ormicamp.infrastructure.repository.dynamoDB.ChatRepository; // 아까 만든 리포지토리
import com.jeju.ormicamp.model.dynamodb.ChatResDto;
import com.jeju.ormicamp.service.dynamodb.ChatService;       // 아까 만든 서비스
import com.jeju.ormicamp.model.dynamodb.ChatEntity;           // 아까 만든 엔티티
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest // 스프링 컨텍스트를 전부 로드 (AWS 설정 포함)
class DynamoDBTest {

    @Autowired
    private ChatService travelService;

    @Autowired
    private ChatRepository travelRepository;

    @Test
    @DisplayName("시나리오: 채팅방을 만들고, 메시지를 보내고, 조회한다.")
    void testFullScenario() {
        // --- 1. 데이터 준비 ---
        String userId = "tester_001"; // 테스트용 유저 ID
        String sessionId = UUID.randomUUID().toString(); // 랜덤한 세션 ID 생성
        String now = LocalDateTime.now().toString();

        System.out.println("====== [TEST START] ======");
        System.out.println("User ID: " + userId);
        System.out.println("Session ID: " + sessionId);

        // --- 2. [Write] 유저의 '내 여행 목록'에 방 추가하기 ---
        // (서비스에 아직 이 메소드가 없어서 리포지토리 직접 사용)
        ChatEntity metaItem = new ChatEntity();
        metaItem.setPk("USER#" + userId);        // PK: 유저 기준
        metaItem.setSk("SESSION#" + now);        // SK: 시간 역순 정렬용
        metaItem.setType("META");
        metaItem.setSessionTitle("제주도 먹방 여행 테스트");
        metaItem.setSessionId(sessionId);        // 나중에 이걸로 채팅방 찾아감

        travelRepository.save(metaItem);
        System.out.println("✅ 1. 여행 목록(Meta) 저장 완료");


        // --- 3. [Write] 채팅 메시지 보내기 (Service 이용) ---
        travelService.saveChatMessage(sessionId, "USER", "안녕, 맛집 추천해줘!");
        try { Thread.sleep(100); } catch (InterruptedException e) {} // 시간차를 둠
        travelService.saveChatMessage(sessionId, "AI", "안녕하세요! 어떤 음식을 좋아하세요?");

        System.out.println("✅ 2. 채팅 메시지 2건 저장 완료");


        // --- 4. [Read] 잘 들어갔는지 검증하기 ---

        // 4-A. 유저의 여행 목록 조회
        List<ChatEntity> mySessions = travelService.getMySessions(userId);

        assertThat(mySessions).isNotEmpty(); // 목록이 비어있으면 안됨
        assertThat(mySessions.get(0).getSessionTitle()).isEqualTo("제주도 먹방 여행 테스트");
        System.out.println("👀 조회된 목록 개수: " + mySessions.size());

        // 4-B. 채팅방 메시지 조회
        List<ChatResDto> chatHistory = travelService.getChatHistory(sessionId);

        assertThat(chatHistory).hasSize(2); // 메시지가 2개여야 함
        assertThat(chatHistory.get(0).getContent()).contains("안녕"); // 내용 확인

        System.out.println("👀 조회된 채팅 개수: " + chatHistory.size());
        for (ChatResDto chat : chatHistory) {
            System.out.println("   -> [" + chat.getRole() + "] " + chat.getContent());
        }

        System.out.println("====== [TEST SUCCESS] ======");
    }
}