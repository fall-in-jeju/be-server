package com.jeju.ormicamp.model.dynamodb;

import com.jeju.ormicamp.model.dynamodb.PlaceItemDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlannerReqDto {
    private String sessionId;   // 어떤 여행 세션인지 (PK용)
    private String date;        // 여행 날짜 (SK용, 예: "2025-12-25")
    private List<PlaceItemDto> places; // 장소 목록
}