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
            return objectMapper.writeValueAsString(
                    Map.of(
                            "conversationId", conversationId,
                            "travelInfo", info,
                            "userMessage", userMessage
                    )
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 직렬화 실패", e);
        }
    }

}
