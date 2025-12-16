package com.jeju.ormicamp.common.config;

import com.jeju.ormicamp.common.jwt.JwtAuthorizationFilter;
import com.jeju.ormicamp.common.jwt.util.JWTUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JWTUtil jwtUtil;
    private final OriginVerifyFilter originVerifyFilter;

    public SecurityConfig(JWTUtil jwtUtil, OriginVerifyFilter originVerifyFilter) {
        this.jwtUtil = jwtUtil;
        this.originVerifyFilter = originVerifyFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(sess
                        -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/test","/api/test/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll() // Health Check 엔드포인트 허용
                        // TODO : 로그인 구현 시 삭제 예정
                        .requestMatchers("/api/planner/date","/api/planner/update/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(originVerifyFilter, UsernamePasswordAuthenticationFilter.class) // X-Origin-Verify 필터 추가
                .addFilterBefore(
                        new JwtAuthorizationFilter(jwtUtil),
                        UsernamePasswordAuthenticationFilter.class
                );
        return http.build();
    }
}