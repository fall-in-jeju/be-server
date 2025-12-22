package com.jeju.ormicamp.service.Bedrock;

import com.jeju.ormicamp.common.config.bedrock.AwsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class BedRockAgentService {

    private final AwsProperties awsProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    public CompletableFuture<String> sendDataToAgent(String agentSessionId, String jsonData) {
        CompletableFuture<String> resultFuture = new CompletableFuture<>();

        try {
            String url = awsProperties.getAgentApiGatewayUrl();

            // 요청 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 요청 body 생성 (팀원이 제공한 API 스펙에 맞게 수정 필요)
            Map<String, Object> requestBody = Map.of(
                    "sessionId", agentSessionId,
                    "inputText", jsonData
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // API Gateway로 HTTP POST 요청
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) 
                    restTemplate.postForEntity(url, request, Map.class);

            // 응답 파싱 (팀원이 제공한 응답 형식에 맞게 수정 필요)
            Map<String, Object> responseBody = response.getBody();
            String result = responseBody != null && responseBody.containsKey("output") 
                    ? (String) responseBody.get("output")
                    : responseBody != null ? responseBody.toString() : "";

            resultFuture.complete(result);
        } catch (Exception e) {
            log.error("Agent API Gateway 호출 실패: {}", e.getMessage(), e);
            resultFuture.completeExceptionally(e);
        }

        return resultFuture;
    }
}
