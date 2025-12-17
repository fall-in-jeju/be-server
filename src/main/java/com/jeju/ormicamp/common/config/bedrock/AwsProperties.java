package com.jeju.ormicamp.common.config.bedrock;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class AwsProperties {

//    @Value("${aws.bedrock.region}")
//    private String bedRockRegion;

    @Value("${aws.region}")
    private String DynamoRegion;

    @Value("${aws.dynamodb.table-name}")
    private String DynamoTableName;

    @Value("${aws.dynamodb.credentials.access-key}")
    private String accessKey;

    @Value("${aws.dynamodb.credentials.secret-key}")
    private String secretKey;

//    @Value("${aws.bedrock.agent-id}")
//    private String agentId;

//    @Value("${aws.bedrock.alias-id}")
//    private String aliasId;

}
