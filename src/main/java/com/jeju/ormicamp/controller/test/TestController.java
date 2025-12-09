package com.jeju.ormicamp.controller.test;

import com.jeju.ormicamp.common.dto.BaseResponse;
import com.jeju.ormicamp.common.exception.ErrorCode;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping
    public ResponseEntity<BaseResponse<Boolean>> test() {

        return ResponseEntity.ok()
                .body(BaseResponse.success("테스트성공",Boolean.TRUE));
    }
    @GetMapping("/error")
    public ResponseEntity<BaseResponse<Void>> testError() {

        BaseResponse<Void> response = BaseResponse.error(ErrorCode.UNKNOWN_ERROR);

        return ResponseEntity.ok()
                .body(response);
    }
}
