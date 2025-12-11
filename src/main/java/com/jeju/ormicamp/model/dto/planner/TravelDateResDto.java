package com.jeju.ormicamp.model.dto.planner;

import com.jeju.ormicamp.model.domain.TravelDate;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class TravelDateResDto {

    // travelDate_id
    private Long id;
    private LocalDate startDate;
    private LocalDate endDate;

    public static TravelDateResDto from(TravelDate travelDate) {
        return new TravelDateResDto(
                travelDate.getId(),
                travelDate.getStartDate(),
                travelDate.getEndDate()
        );
    }


}
