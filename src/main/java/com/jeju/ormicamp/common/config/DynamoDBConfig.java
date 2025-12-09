package com.jeju.ormicamp.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Configuration
public class DynamoDBConfig {

    @Value("${aws.region}")
    private String region;

    @Value("${aws.access-key}") // yml에서 가져온 키
    private String accessKey;

    @Value("${aws.secret-key}") // yml에서 가져온 비밀키
    private String secretKey;

    @Bean
    public DynamoDbClient dynamoDbClient() {

        System.out.println("### 현재 적용된 리전: [" + region + "]");
        return DynamoDbClient.builder()
                .region(Region.of(region))
                // [변경] 환경변수 대신 yml 값을 직접 사용하도록 설정
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }
}