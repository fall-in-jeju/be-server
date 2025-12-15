package com.jeju.ormicamp.common.config;

import com.jeju.ormicamp.common.config.bedrock.AwsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Configuration
// 해당 어노테이션으로 개발환경과 배포 Config 구분
@Profile("local")
@RequiredArgsConstructor
public class AwsLocalConfig {

    private final AwsProperties properties;

    @Bean
    public DynamoDbClient dynamoDbClient() {
        AwsBasicCredentials creds = AwsBasicCredentials.create(
                properties.getAccessKey(),
                properties.getSecretKey()
        );

        return DynamoDbClient.builder()
                .region(Region.of(properties.getDynamoRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(creds))
                .build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }

//    @Bean
//    public BedrockAgentRuntimeAsyncClient bedrockAgentRuntimeAsyncClient() {
//        AwsBasicCredentials creds = AwsBasicCredentials.create(
//                properties.getAccessKey(),
//                properties.getSecretKey()
//        );
//
//        return BedrockAgentRuntimeAsyncClient.builder()
//                .region(Region.of(properties.getBedRockRegion()))
//                .credentialsProvider(StaticCredentialsProvider.create(creds))
//                .build();
//    }
}
