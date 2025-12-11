package com.jeju.ormicamp.common.config;

import com.jeju.ormicamp.common.config.bedrock.AwsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;


/**
 * 해당 환경은 배포 시 각 서비스의 role을 사용한다 생각하여
 * ACCESS_KEY와 SECRET_KEY를 사용하지 않는다는 가정
 */
@Configuration
// 해당 어노테이션으로 개발환경과 배포 Config를 구분
@Profile("prod")
@RequiredArgsConstructor
public class AwsProdConfig {

    private final AwsProperties properties;

    @Bean
    public DynamoDbClient dynamoDbClient() {

        return DynamoDbClient.builder()
                .region(Region.of(properties.getDynamoRegion()))
                .build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }

    @Bean
    public BedrockAgentRuntimeAsyncClient bedrockAgentRuntimeAsyncClient() {

        return BedrockAgentRuntimeAsyncClient.builder()
                .region(Region.of(properties.getBedRockRegion()))
                .build();
    }
}
