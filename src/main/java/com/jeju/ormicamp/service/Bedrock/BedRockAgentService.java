package com.jeju.ormicamp.service.Bedrock;

import com.jeju.ormicamp.common.config.bedrock.AwsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockagentruntime.model.*;

import java.util.concurrent.CompletableFuture;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
//@Service
@RequiredArgsConstructor
public class BedRockAgentService {

    private final BedrockAgentRuntimeAsyncClient agentClient;
    private final AwsProperties awsProperties;

    public CompletableFuture<String> sendDataToAgent(String agentSessionId, String jsonData){

        CompletableFuture<String> resultFuture = new CompletableFuture<>();
        StringBuilder finalText = new StringBuilder();

        InvokeAgentRequest request = InvokeAgentRequest.builder()
                .agentId(awsProperties.getAgentId())
                .agentAliasId(awsProperties.getAliasId())
                // 얘는 실제 seesionId아니고 그 역할하는 Id 값
                .sessionId(agentSessionId)
                .inputText(jsonData)
                .enableTrace(true)
                .build();

        InvokeAgentResponseHandler handler = new InvokeAgentResponseHandler() {

            // 여기의 결과값
            @Override
            public void responseReceived(InvokeAgentResponse response) {
            }

            /**
             * 시나리오
             * 1. 실제 응답 = ResponseStream이고
             * 2. instanceof 에서 PayloadPart(=chunk)타입만 저장
             * @param stream Agent 응답 조각
             */
            // 또 여기 결과값
            @Override
            public void onEventStream(SdkPublisher<ResponseStream> stream) {
                stream.subscribe(event -> {
                    if (event instanceof PayloadPart payload) {
                        SdkBytes bytes = payload.bytes();
                        String text = new String(bytes.asByteArray(), UTF_8);
                        finalText.append(text);
                    }
                });
            }

            // 또 여기 결과값
            @Override
            public void exceptionOccurred(Throwable throwable) {
                resultFuture.completeExceptionally(throwable);
            }

            // 을 최종적으로 모아서 답변
            @Override
            public void complete() {
                resultFuture.complete(finalText.toString());
            }
        };

        agentClient.invokeAgent(request, handler);
        return resultFuture;
    }
}
