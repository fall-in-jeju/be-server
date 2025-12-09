package com.jeju.ormicamp.controller.planner;

import com.jeju.ormicamp.common.dto.BaseResponse;
import com.jeju.ormicamp.model.dto.planner.TravelDateReqDto;
import com.jeju.ormicamp.service.planner.TravelDateService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/planner")
@AllArgsConstructor
public class TravelDateController {

    private final TravelDateService travelDateService;

    @PostMapping("/date")
    public ResponseEntity<BaseResponse<Boolean>> date(
            @RequestBody TravelDateReqDto dto) {

        travelDateService.saveDate(dto);

        return ResponseEntity.ok()
                .body(BaseResponse.success("날짜 저장성공",Boolean.TRUE));
    }
}
