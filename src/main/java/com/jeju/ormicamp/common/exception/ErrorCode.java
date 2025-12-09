package com.jeju.ormicamp.common.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 양식
 * <p>
 * 1. 이름: 도메인_상황
 * 2. 내용: http 상태코드, 분류코드(이름_상태코드), 사용자 친화적 메시지
 * 각 도메인 별로 ErrorCode를 분리해주세요.
 */
@Getter
public enum ErrorCode {
    /*
    GlobalExceptionHandler에서 사용
     */
    @Schema(description = "처리되지 않은 서버 오류입니다.")
    UNKNOWN_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "500", "처리되지 않은 서버 오류입니다."),

    @Schema(description = "비활성화된 계정입니다.")
    MEMBER_DEACTIVATED(HttpStatus.UNAUTHORIZED, "401", "비활성화된 계정입니다.");

    private final HttpStatus httpStatus;
    @Schema(description = "에러 코드", example = "UNKNOWN_ERROR_500", implementation = ErrorCode.class)
    private final String code;

    @Schema(description = "사용자 친화적 메시지", example = "처리되지 않은 서버 오류입니다.")
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
