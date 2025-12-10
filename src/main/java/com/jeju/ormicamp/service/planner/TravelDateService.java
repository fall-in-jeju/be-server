package com.jeju.ormicamp.service.planner;

import com.jeju.ormicamp.common.exception.CustomException;
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


    public TravelDate saveDate(TravelDateReqDto dto) {

        if(!dto.getStartDate().isBefore(dto.getEndDate())) {
            throw new CustomException(ErrorCode.INVALID_DATE_RANGE);
        }

        TravelDate travelDate = TravelDate.builder()
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .build();
        return travelDateRepository.save(travelDate);
    }

    public TravelDate updateDate(Long travelDateId, TravelDateReqDto dto) {

        TravelDate update = travelDateRepository.findById(travelDateId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        update.updateDate(dto.getStartDate(), dto.getEndDate());

        return travelDateRepository.save(update);
    }
}
