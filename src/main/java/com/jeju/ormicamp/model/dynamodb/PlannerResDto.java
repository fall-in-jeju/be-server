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
public class PlannerResDto {
    private String sessionId;
    private String date;
    private List<PlaceItemDto> places;
}