package com.jeju.ormicamp.common.config.bedrock;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "aws.bedrock")
@Getter
@Setter
public class BedRockProperties {

    private String region;
    private String gentId;
    private String aliasId;
}
