package com.jeju.ormicamp.common.config.bedrock;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class AwsProperties {

    @Value("${aws.bedrock.region}")
    private String bedRockRegion;

    @Value("${aws.region}")
    private String DynamoRegion;

    @Value("${aws.bedrock.agent-id}")
    private String gentId;

    @Value("${aws.bedrock.alias-id}")
    private String aliasId;

    // Key값은 배포 시 Role을 사용한다면
    // 사용 x
    @Value("${aws.access-key}")
    private String accessKey;
    @Value("${aws.secret-key}")
    private String secretKey;
}
