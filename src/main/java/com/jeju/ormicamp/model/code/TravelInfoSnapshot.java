package com.jeju.ormicamp.model.code;

import com.jeju.ormicamp.model.domain.TravelInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class TravelInfoSnapshot {

    private LocalDate startDate;
    private LocalDate endDate;
    private Long capacity;
    private Long money;
    private Region region;  // 여행 지역

    public static TravelInfoSnapshot toSnapshot(TravelInfo travelInfo) {
        return new TravelInfoSnapshot(
                travelInfo.getStartDate(),
                travelInfo.getEndDate(),
                travelInfo.getCapacity(),
                travelInfo.getMoney(),
                travelInfo.getRegion()
        );
    }
}