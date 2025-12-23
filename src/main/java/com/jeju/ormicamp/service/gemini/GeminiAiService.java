package com.jeju.ormicamp.service.gemini;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Profile("local") // ⭐ 지금은 local에서 Gemini
@RequiredArgsConstructor
public class GeminiAiService implements AiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String invoke(String conversationId, String payload) {

        // ✅ 최신 Gemini HTTP 엔드포인트 (v1, gemini-1.5-flash-latest)
        // 공식 문서 기준: POST https://generativelanguage.googleapis.com/v1/models/gemini-1.5-flash-latest:generateContent
        String url =
             "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key="
            + apiKey;

        Map<String, Object> body = Map.of(
            "contents", List.of(
                Map.of(
                    "parts", List.of(
                        Map.of("text", payload)
                    )
                )
            )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response =
                    restTemplate.postForEntity(url, request, Map.class);

            // ✅ Gemini 응답 파싱 (안정 버전)
            Map<String, Object> responseBody = response.getBody();

            List<Map<String, Object>> candidates =
                    (List<Map<String, Object>>) responseBody.get("candidates");

            Map<String, Object> content =
                    (Map<String, Object>) candidates.get(0).get("content");

            List<Map<String, Object>> parts =
                    (List<Map<String, Object>>) content.get("parts");

            return (String) parts.get(0).get("text");
        } catch (Exception e) {
            log.error("Gemini 호출 오류 - url={}, message={}", url, e.getMessage(), e);
            throw e;
        }
    }
}