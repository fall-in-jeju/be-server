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


    public Long saveDate(TravelDateReqDto dto) {

        if(dto.getEndDate().isBefore(dto.getStartDate())){
            // TODO : 에러 CustomException 도입 시 변경
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }

        TravelDate travelDate = TravelDate.builder()
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .build();
        TravelDate result = travelDateRepository.save(travelDate);

        return result.getId();
    }

    public void updateDate(Long travelDateId, TravelDateReqDto dto) {

        // TODO : 에러 가시화
        TravelDate update = travelDateRepository.findById(travelDateId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        update.updateDate(dto.getStartDate(), dto.getEndDate());

        // TODO : 변경된 TravleDate 값 반환

    }
}
