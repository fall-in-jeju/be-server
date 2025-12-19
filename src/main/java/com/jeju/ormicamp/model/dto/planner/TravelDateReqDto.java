package com.jeju.ormicamp.model.dto.planner;

import com.jeju.ormicamp.model.code.Region;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;


@Getter
@Setter
public class TravelDateReqDto {

    private LocalDate startDate;

    private LocalDate endDate;

    private Region region;  // 여행 지역
}
