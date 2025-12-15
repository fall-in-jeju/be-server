package com.jeju.ormicamp.model.dto.user;

public record CognitoUserInfo(
        String sub,
        String email,
        String name
) {
}
