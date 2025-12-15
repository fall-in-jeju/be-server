package com.jeju.ormicamp.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
public class JwtConfig {

    @Value("${jwt.secret}")  // Spring 속성 읽기 (application.yml에서 정의)
    private String secretKey;

    @Value("${cognito.jwks-url}")  // Spring 속성 읽기 (application.yml에서 정의)
    private String jwksUrl;

    @Bean
    public JwtDecoder cognitoJwtDecoder() {
        return NimbusJwtDecoder
                .withJwkSetUri(jwksUrl)
                .build();
    }
}
