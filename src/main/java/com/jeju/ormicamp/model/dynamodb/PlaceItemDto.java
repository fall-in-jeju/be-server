package com.jeju.ormicamp.model.dynamodb;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaceItemDto {
    private Long order;       // 순서
    private String placeName; // 장소명
    private String time;      // 시간
    private String category;  // 카테고리
    private Double lat;       // 위도
    private Double lng;       // 경도
    private String address;   // 주소
    private String mapId;     // 지도 ID
}