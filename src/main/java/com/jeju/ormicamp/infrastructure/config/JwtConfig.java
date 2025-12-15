package com.jeju.ormicamp.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
public class JwtConfig {

    @Value("${jwt.secret}")
    private String secretKey;

    @Bean
    public JwtDecoder cognitoJwtDecoder() {
        return NimbusJwtDecoder
                .withJwkSetUri("https://cognito-idp.ap-northeast-2.amazonaws.com/${cognito.user-pool-id}/.well-known/jwks.json")
                .build();
    }
}
