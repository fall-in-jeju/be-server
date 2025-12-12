package com.jeju.ormicamp.controller.dynamodb;

import com.jeju.ormicamp.model.dynamodb.PlannerReqDto;
import com.jeju.ormicamp.model.dynamodb.PlannerResDto;
import com.jeju.ormicamp.model.dynamodb.PlannerResDto.*;
import com.jeju.ormicamp.model.dynamodb.PlaceItemDto.*;
import com.jeju.ormicamp.model.dynamodb.PlannerReqDto.*;
import com.jeju.ormicamp.service.dynamodb.PlannerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/planner") // 기본 URL: http://localhost:8080/api/planner
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // 모든 출처 허용 (개발용. 배포시에는 프론트 주소만 적는 게 좋음)
public class PlannerController {

    private final PlannerService plannerService;

    // 1. 플래너 저장 및 수정 (Upsert)
    // POST /api/planner
    @PostMapping
    public ResponseEntity<String> savePlanner(@RequestBody PlannerReqDto reqDto) {
        log.info("플래너 저장 요청: SessionId={}, Date={}", reqDto.getSessionId(), reqDto.getDate());
        plannerService.updatePlanner(reqDto);
        return ResponseEntity.ok("플래너가 성공적으로 저장되었습니다.");
    }

    // 2. 특정 날짜의 플래너 조회
    // GET /api/planner/{sessionId}/{date}
    // 예: /api/planner/user123/2025-12-25
    @GetMapping("/{sessionId}/{date}")
    public ResponseEntity<PlannerResDto> getPlanner(
            @PathVariable String sessionId,
            @PathVariable String date) {
        log.info("플래너 단건 조회: SessionId={}, Date={}", sessionId, date);
        PlannerResDto resDto = plannerService.getPlanner(sessionId, date);

        if (resDto == null) {
            return ResponseEntity.notFound().build(); // 404 Not Found
        }
        return ResponseEntity.ok(resDto);
    }

    // 3. 해당 세션의 전체 여행 일정 조회
    // GET /api/planner/{sessionId}
    @GetMapping("/{sessionId}")
    public ResponseEntity<List<PlannerResDto>> getAllPlanners(@PathVariable String sessionId) {
        log.info("전체 일정 조회: SessionId={}", sessionId);
        List<PlannerResDto> list = plannerService.getAllPlanners(sessionId);
        return ResponseEntity.ok(list);
    }

    // 4. 특정 날짜 일정 삭제
    // DELETE /api/planner/{sessionId}/{date}
    @DeleteMapping("/{sessionId}/{date}")
    public ResponseEntity<String> deletePlanner(
            @PathVariable String sessionId,
            @PathVariable String date) {
        log.info("플래너 삭제: SessionId={}, Date={}", sessionId, date);
        plannerService.deletePlanner(sessionId, date);
        return ResponseEntity.ok("일정이 삭제되었습니다.");
    }
}