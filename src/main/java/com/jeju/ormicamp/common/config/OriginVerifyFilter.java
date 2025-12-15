package com.jeju.ormicamp.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class OriginVerifyFilter extends OncePerRequestFilter {

    @Value("${origin.verify-secret}")
    private String originVerifySecret;

    private static final String X_ORIGIN_VERIFY_HEADER = "X-Origin-Verify";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String headerValue = request.getHeader(X_ORIGIN_VERIFY_HEADER);

        // /actuator/** 경로는 인증 제외 (헬스 체크, 프로메테우스 등)
        // 그 외, CloudFront를 통과한 요청은 X-Origin-Verify 헤더를 검증
        if (request.getRequestURI().startsWith("/actuator/") || originVerifySecret.equals(headerValue)) {
            filterChain.doFilter(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden: Invalid X-Origin-Verify header");
        }
    }
}
