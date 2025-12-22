package com.jeju.ormicamp.service.gemini;

import com.jeju.ormicamp.common.exception.CustomException;
import com.jeju.ormicamp.common.exception.ErrorCode;
import com.jeju.ormicamp.infrastructure.repository.dynamoDB.ChatDynamoRepository;
import com.jeju.ormicamp.infrastructure.repository.planner.TravelInfoRepository;
import com.jeju.ormicamp.model.code.ChatRole;
import com.jeju.ormicamp.model.code.ChatType;
import com.jeju.ormicamp.model.code.TravelInfoSnapshot;
import com.jeju.ormicamp.model.domain.ChatEntity;
import com.jeju.ormicamp.model.domain.TravelInfo;
import com.jeju.ormicamp.model.dto.dynamodb.ChatConversationResDto;
import com.jeju.ormicamp.model.dto.dynamodb.ChatMessageResDto;
import com.jeju.ormicamp.model.dto.dynamodb.ChatReqDto;
import com.jeju.ormicamp.model.dto.dynamodb.ChatResDto;
import com.jeju.ormicamp.model.dto.dynamodb.PlanDayResDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jeju.ormicamp.service.Bedrock.MakeJsonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static java.time.LocalTime.now;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatGeminiService {

    private final TravelInfoRepository travelInfoRepository;
    private final ChatDynamoRepository chatRepository;
    private final MakeJsonService makeJsonService;
    private final AiService aiService;
    private final ObjectMapper objectMapper;

    public ChatResDto startChat(ChatReqDto req, Long userId) {

        String conversationId = UUID.randomUUID().toString();

        // 지금 이건 애초에 meta에 저장하기 위해서 한번 필요함\
        // 왜냐 userid기준으로 조회한 값이기 때문
        TravelInfo travelInfo = travelInfoRepository
                .findByUserId(userId).orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        // 대화방 메타 데이터 초기 데이터 조회용
        // 초기 title은 임시값, 나중에 Agent가 생성한 title로 업데이트됨
        ChatEntity meta = ChatEntity.builder()
                .conversationId(conversationId)
                .type(ChatType.PLAN_META)
                .chatTitle("새 여행 계획")  // 임시 제목, Agent 응답으로 업데이트됨
                .travelInfo(TravelInfoSnapshot.toSnapshot(travelInfo))
                .build();

        chatRepository.save(meta);

        chatRepository.save(
                ChatEntity.builder()
                        .conversationId(conversationId)
                        .type(ChatType.CHAT)
                        .role(ChatRole.USER)
                        .prompt(req.getContent())
                        .timestamp(now().toString())
                        .build()
        );

        String payload = makeJsonService.createJsonPayload(
                conversationId,
                req.getContent(),
                meta.getTravelInfo()
        );

        String agentResponse = aiService.invoke(conversationId, payload);

        // Agent 응답 파싱 (JSON 형식: {"title": "...", "content": "...", "summary": "...", "plans": [...]})
        String content = agentResponse;
        String summary = null;
        String title = null;
        JsonNode plansArray = null;

        try {
            JsonNode node = objectMapper.readTree(agentResponse);
            content = node.has("content") ? node.get("content").asText() : agentResponse;
            summary = node.has("summary") ? node.get("summary").asText() : null;
            title = node.has("title") ? node.get("title").asText() : null;
            
            if (node.has("plans") && node.get("plans").isArray()) {
                plansArray = node.get("plans");
            }
        } catch (Exception e) {
            log.warn("Agent 응답 JSON 파싱 실패, 원본 저장: {}", e.getMessage());
        }

        // Title이 있으면 META 업데이트
        if (title != null && !title.isEmpty()) {
            meta.setChatTitle(title);
            chatRepository.save(meta);
        }

        // AI 응답 저장 (CHAT 타입)
        chatRepository.save(
                ChatEntity.builder()
                        .conversationId(conversationId)
                        .type(ChatType.CHAT)
                        .role(ChatRole.AI)
                        .prompt(content)
                        .summary(summary)
                        .timestamp(now().toString())
                        .build()
        );

        // 날짜별 플래너 저장 (PLAN_DAY 타입)
        if (plansArray != null && plansArray.isArray()) {
            for (JsonNode planNode : plansArray) {
                if (planNode.has("date") && planNode.has("content")) {
                    String planDate = planNode.get("date").asText();
                    String planContent = planNode.get("content").asText();
                    
                    chatRepository.save(
                            ChatEntity.builder()
                                    .conversationId(conversationId)
                                    .type(ChatType.PLAN_DAY)
                                    .role(ChatRole.AI)
                                    .prompt(planContent)
                                    .planDate(planDate)
                                    .timestamp(now().toString())
                                    .build()
                    );
                }
            }
        }

        return ChatResDto.builder()
                .conversationId(conversationId)
                .message(content)
                .summary(summary)
                .title(title)
                .build();
    }

    public ChatResDto sendMessage(String conversationId,String content, Long userId) {

        ChatEntity meta = chatRepository.findMeta(conversationId);

        chatRepository.save(
                ChatEntity.builder()
                        .conversationId(conversationId)
                        .type(ChatType.CHAT)
                        .role(ChatRole.USER)
                        .prompt(content)
                        .timestamp(now().toString())
                        .build()
        );

        String payload = makeJsonService.createJsonPayload(
                conversationId,
                content,
                meta.getTravelInfo()
        );

        String agentResponse = aiService.invoke(conversationId, payload);

        // Agent 응답 파싱 (JSON 형식: {"title": "...", "content": "...", "summary": "...", "plans": [...]})
        String msgContent = agentResponse;
        String summary = null;
        String title = null;
        JsonNode plansArray = null;

        try {
            JsonNode node = objectMapper.readTree(agentResponse);
            msgContent = node.has("content") ? node.get("content").asText() : agentResponse;
            summary = node.has("summary") ? node.get("summary").asText() : null;
            title = node.has("title") ? node.get("title").asText() : null;
            
            if (node.has("plans") && node.get("plans").isArray()) {
                plansArray = node.get("plans");
            }
        } catch (Exception e) {
            log.warn("Agent 응답 JSON 파싱 실패, 원본 저장: {}", e.getMessage());
        }

        // Title이 있으면 META 업데이트
        if (title != null && !title.isEmpty()) {
            meta.setChatTitle(title);
            chatRepository.save(meta);
        }

        // AI 응답 저장 (CHAT 타입)
        chatRepository.save(
                ChatEntity.builder()
                        .conversationId(conversationId)
                        .type(ChatType.CHAT)
                        .role(ChatRole.AI)
                        .prompt(msgContent)
                        .summary(summary)
                        .timestamp(now().toString())
                        .build()
        );

        // 날짜별 플래너 저장 (PLAN_DAY 타입)
        if (plansArray != null && plansArray.isArray()) {
            for (JsonNode planNode : plansArray) {
                if (planNode.has("date") && planNode.has("content")) {
                    String planDate = planNode.get("date").asText();
                    String planContent = planNode.get("content").asText();
                    
                    chatRepository.save(
                            ChatEntity.builder()
                                    .conversationId(conversationId)
                                    .type(ChatType.PLAN_DAY)
                                    .role(ChatRole.AI)
                                    .prompt(planContent)
                                    .planDate(planDate)
                                    .timestamp(now().toString())
                                    .build()
                    );
                }
            }
        }

        return ChatResDto.builder()
                .conversationId(conversationId)
                .message(msgContent)
                .summary(summary)
                .title(title)
                .build();
    }

    // 채팅방 정보 반환
    public ChatConversationResDto getConversation(String conversationId) {

        List<ChatEntity> items =
                chatRepository.findByConversationId(conversationId);

        if (items.isEmpty()) {
            throw new CustomException(ErrorCode.NOT_FOUND);
        }

        ChatEntity meta = items.stream()
                .filter(i -> i.getType() == ChatType.PLAN_META)
                .findFirst()
                .orElseThrow();

        List<ChatMessageResDto> messages = items.stream()
                .filter(i -> i.getType() == ChatType.CHAT)
                .sorted(Comparator.comparing(ChatEntity::getTimestamp))
                .map(ChatMessageResDto::from)
                .toList();

        return ChatConversationResDto.builder()
                .conversationId(conversationId)
                .chatTitle(meta.getChatTitle())
                .travelInfo(meta.getTravelInfo())
                .messages(messages)
                .build();
    }

    /**
     * 날짜별 플래너 조회
     * @param conversationId 대화 ID
     * @param date 날짜 (YYYY-MM-DD)
     * @return 해당 날짜의 플래너 목록
     */
    public List<PlanDayResDto> getPlansByDate(String conversationId, String date) {
        List<ChatEntity> plans = chatRepository.findPlansByDate(conversationId, date);
        return PlanDayResDto.fromList(plans);
    }

}