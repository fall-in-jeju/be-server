package com.jeju.ormicamp.model.dto.planner;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;


@Getter
@Setter
public class TravelDateReqDto {

    private LocalDate startDate;

    private LocalDate endDate;
}
