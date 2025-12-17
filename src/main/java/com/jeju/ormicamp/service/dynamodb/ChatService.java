package com.jeju.ormicamp.service.dynamodb;

import com.jeju.ormicamp.common.exception.CustomException;
import com.jeju.ormicamp.common.exception.ErrorCode;
import com.jeju.ormicamp.infrastructure.repository.dynamoDB.ChatDynamoRepository;
import com.jeju.ormicamp.infrastructure.repository.planner.TravelInfoRepository;
import com.jeju.ormicamp.model.code.ChatRole;
import com.jeju.ormicamp.model.code.ChatType;
import com.jeju.ormicamp.model.code.TravelInfoSnapshot;
import com.jeju.ormicamp.model.domain.ChatEntity;
import com.jeju.ormicamp.model.domain.TravelInfo;
import com.jeju.ormicamp.model.dto.dynamodb.ChatReqDto;
import com.jeju.ormicamp.model.dto.dynamodb.ChatResDto;
//import com.jeju.ormicamp.service.Bedrock.BedRockAgentService;
import com.jeju.ormicamp.service.Bedrock.MakeJsonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static java.time.LocalTime.now;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final TravelInfoRepository travelInfoRepository;
    private final ChatDynamoRepository chatRepository;
    //private final BedRockAgentService agentService;
    private final MakeJsonService makeJsonService;

    /**
     * 새로운 채팅 세션을 시작합니다.
     *
     * <p>
     * 사용자의 여행 정보를 기반으로 대화 세션(conversationId)을 생성하고,
     * 해당 세션의 메타 데이터(META)를 DynamoDB에 저장합니다.
     * </p>
     *
     * <h3>처리 흐름</h3>
     * <ol>
     *   <li>conversationId(UUID) 생성</li>
     *   <li>사용자의 여행 정보(TravelInfo) 조회</li>
     *   <li>META 데이터 저장 (대화 제목, 여행 스냅샷)</li>
     *   <li>사용자 첫 메시지 저장</li>
     *   <li>AI 응답 생성 및 저장</li>
     * </ol>
     *
     * <h3>META 데이터란?</h3>
     * <p>
     * META는 대화 세션 전체에서 공통으로 사용되는 고정 정보이며,
     * 다음과 같은 데이터를 포함합니다.
     * </p>
     *
     * <ul>
     *   <li>conversationId</li>
     *   <li>채팅 제목(chatTitle)</li>
     *   <li>여행 정보 스냅샷(TravelInfoSnapshot)</li>
     * </ul>
     *
     * <p>
     * META 데이터는 대화 시작 시 한 번만 저장되며,
     * 이후 메시지 전송 시 기준 정보로 활용됩니다.
     * </p>
     *
     * @param req    채팅 시작 시 입력된 요청 정보 (제목, 첫 메시지)
     * @param userId 현재 로그인한 사용자 ID
     * @return AI 응답과 conversationId를 포함한 채팅 응답 DTO
     */
    public ChatResDto startChat(ChatReqDto req, Long userId) {

        String conversationId = UUID.randomUUID().toString();

        // 지금 이건 애초에 meta에 저장하기 위해서 한번 필요함\
        // 왜냐 userid기준으로 조회한 값이기 때문
        TravelInfo travelInfo = travelInfoRepository
                .findByUserId(userId).orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        // 대화방 메타 데이터 초기 데이터 조회용
        ChatEntity meta = ChatEntity.builder()
                .conversationId(conversationId)
                .type(ChatType.PLAN_META)
                .chatTitle(req.getChatTitle())
                .travelInfo(TravelInfoSnapshot.toSnapshot(travelInfo))
                .build();

        TravelInfoSnapshot response = TravelInfoSnapshot.toSnapshot(travelInfo);

        // TODO : 예외처리
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

        // TODO : agent 연결 시 주석 해제
        // String agentResponse = agentService.sendDataToAgent(conversationId, payload).join();

        // 테스트용 더미
        String agentResponse = """
                [TEST MODE]
                제주 여행 일정 예시입니다.
                - 1일차: 성산일출봉
                - 2일차: 한라산
                - 3일차: 카페 투어
                """;

        chatRepository.save(
                ChatEntity.builder()
                        .conversationId(conversationId)
                        .type(ChatType.CHAT)
                        .role(ChatRole.AI)
                        .prompt(agentResponse)
                        .timestamp(now().toString())
                        .build()
        );

        return ChatResDto.builder()
                .conversationId(conversationId)
                .message(agentResponse)
                .build();
    }

    /**
     * 기존 채팅 세션에 메시지를 추가합니다.
     *
     * <p>
     * 전달받은 conversationId를 기준으로 META 데이터를 조회한 후,
     * 사용자의 메시지와 AI 응답을 DynamoDB에 저장합니다.
     * </p>
     *
     * <h3>처리 흐름</h3>
     * <ol>
     *   <li>conversationId로 META 데이터 조회</li>
     *   <li>사용자 메시지 저장</li>
     *   <li>AI 요청용 JSON Payload 생성</li>
     *   <li>AI 응답 저장</li>
     * </ol>
     *
     * <p>
     * META 데이터가 존재하지 않는 경우,
     * 유효하지 않은 대화 세션으로 판단하여 예외를 발생시킵니다.
     * </p>
     *
     * @param req    사용자가 입력한 메시지 및 conversationId
     * @param userId 현재 로그인한 사용자 ID
     * @return AI 응답 메시지를 포함한 채팅 응답 DTO
     */
    public ChatResDto sendMessage(ChatReqDto req, Long userId) {

        String conversationId = req.getConversationId();

        ChatEntity meta = chatRepository.findMeta(conversationId);

        ChatEntity chat = ChatEntity.builder()
                .conversationId(conversationId)
                .type(ChatType.CHAT)
                .role(ChatRole.USER)
                .prompt(req.getContent())
                .timestamp(now().toString())
                .build();

        chatRepository.save(chat);

        String payload = makeJsonService.createJsonPayload(
                conversationId,
                req.getContent(),
                chat.getTravelInfo()
        );

        // TODO : agent 연결 시 주석 해제
        // String agentResponse = agentService.sendDataToAgent(conversationId, payload).join();

        // 테스트용 더미
        String agentResponse = """
                [TEST MODE]
                제주 여행 일정 예시입니다.
                - 1일차: 성산일출봉
                - 2일차: 한라산
                - 3일차: 카페 투어
                """;

        chatRepository.save(
                ChatEntity.builder()
                        .conversationId(conversationId)
                        .type(ChatType.CHAT)
                        .role(ChatRole.AI)
                        .prompt(agentResponse)
                        .timestamp(now().toString())
                        .build()
        );

        return ChatResDto.builder()
                .conversationId(conversationId)
                .message(agentResponse)
                .build();
    }
}