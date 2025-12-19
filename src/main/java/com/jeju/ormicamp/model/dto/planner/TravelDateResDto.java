package com.jeju.ormicamp.model.dto.planner;

import com.jeju.ormicamp.model.code.Region;
import com.jeju.ormicamp.model.domain.TravelInfo;
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
    private Region region;  // 여행 지역

    public static TravelDateResDto from(TravelInfo travelInfo) {
        return new TravelDateResDto(
                travelInfo.getId(),
                travelInfo.getStartDate(),
                travelInfo.getEndDate(),
                travelInfo.getRegion()
        );
    }


}
