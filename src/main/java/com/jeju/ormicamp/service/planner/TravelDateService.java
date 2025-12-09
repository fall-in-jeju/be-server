package com.jeju.ormicamp.service.planner;

import com.jeju.ormicamp.common.exception.ErrorCode;
import com.jeju.ormicamp.infrastructure.repository.planner.TravelDateRepository;
import com.jeju.ormicamp.model.domain.TravelDate;
import com.jeju.ormicamp.model.dto.planner.TravelDateReqDto;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class TravelDateService {

    private final TravelDateRepository travelDateRepository;


    public Boolean saveDate(TravelDateReqDto dto) {

        // TODO : user 예외처리 확인

        // TODO : endDate > startDate 검증
        TravelDate travelDate = TravelDate.builder()
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .build();

        travelDateRepository.save(travelDate);

        return true;
    }
}
