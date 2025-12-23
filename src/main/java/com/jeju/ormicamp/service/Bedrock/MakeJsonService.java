package com.jeju.ormicamp.service.Bedrock;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jeju.ormicamp.model.code.TravelInfoSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class MakeJsonService {

    private final ObjectMapper objectMapper;

    public String createJsonPayload(
            String conversationId,
            String userMessage,
            TravelInfoSnapshot info
    ) {
        try {
            // travelInfo 객체 생성 (budget, people, startDate, endDate만 포함)
            Map<String, Object> travelInfoMap = Map.of(
                    "budget", info.getMoney() != null ? info.getMoney() : 0,
                    "people", info.getCapacity() != null ? info.getCapacity() : 1,
                    "startDate", info.getStartDate().toString(),
                    "endDate", info.getEndDate().toString()
            );
            
            // 최종 payload 생성
            return objectMapper.writeValueAsString(
                    Map.of(
                            "conversationId", conversationId,
                            "language", info.getLanguage() != null ? info.getLanguage().name().toLowerCase() : "ko",
                            "theme", info.getThemes() != null ? info.getThemes().stream()
                                    .map(theme -> {
                                        // Theme Enum을 한글 이름으로 변환
                                        return switch (theme) {
                                            case NATURE -> "자연";
                                            case HEALING -> "힐링";
                                            case CULTURE -> "문화";
                                            case FOOD -> "맛집";
                                            case ACTIVITY -> "액티비티";
                                            case PHOTO -> "사진";
                                            case FAMILY -> "가족";
                                            case COUPLE -> "커플";
                                            case FRIEND -> "친구";
                                        };
                                    })
                                    .toList() : java.util.List.of(),
                            "content", userMessage,
                            "travelInfo", travelInfoMap
                    )
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 직렬화 실패", e);
        }
    }

}
