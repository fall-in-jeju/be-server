package com.jeju.ormicamp.service.dynamodb;

import com.jeju.ormicamp.common.exception.CustomException;
import com.jeju.ormicamp.common.exception.ErrorCode;
import com.jeju.ormicamp.model.dynamodb.ChatEntity;
import com.jeju.ormicamp.model.dynamodb.PlaceItemDto;
import com.jeju.ormicamp.model.dynamodb.PlannerReqDto;
import com.jeju.ormicamp.model.dynamodb.PlannerResDto;
import com.jeju.ormicamp.infrastructure.repository.dynamoDB.PlannerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlannerService {

    private final PlannerRepository plannerRepository;

    /**
     * 플래너 생성 및 수정 (Upsert)
     * AI가 짜준 코스나, 사용자가 변경한 코스를 저장
     */
    public void updatePlanner(PlannerReqDto reqDto) {
        if (reqDto.getSessionId() == null || reqDto.getSessionId().isEmpty()) {
            throw new CustomException(ErrorCode.PLAN_SESSION_MISSING);
        }
        if (reqDto.getDate() == null) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }

        ChatEntity entity = new ChatEntity();

        // 1. Key 생성 로직 (Single Table Design의 핵심)
        entity.setPk("SESSION#" + reqDto.getSessionId());
        entity.setSk("PLAN#" + reqDto.getDate()); // 날짜별 식별

        // 2. 메타 데이터 설정
        entity.setType("PLAN");
        entity.setDate(reqDto.getDate());

        // 3. 장소 리스트 매핑 (DTO -> Entity Inner Class)
        List<ChatEntity.PlaceItem> placeItems = reqDto.getPlaces().stream()
                .map(this::convertToEntityItem)
                .collect(Collectors.toList());

        entity.setPlaces(placeItems);

        // 4. 저장
        plannerRepository.savePlan(entity);
    }

    /**
     * 특정 날짜 플래너 조회
     */
    public PlannerResDto getPlanner(String sessionId, String date) {
        if (sessionId == null) throw new CustomException(ErrorCode.PLAN_SESSION_MISSING);
        ChatEntity entity = plannerRepository.findPlanBySessionAndDate(sessionId, date);

        if (entity == null) {
            throw new CustomException(ErrorCode.PLAN_NOT_FOUND);
        }

        return convertToResDto(entity);
    }

    /**
     * 세션의 전체 일정 조회
     */
    public List<PlannerResDto> getAllPlanners(String sessionId) {
        if (sessionId == null) throw new CustomException(ErrorCode.PLAN_SESSION_MISSING);
        List<ChatEntity> entities = plannerRepository.findAllPlansBySession(sessionId);

        return entities.stream()
                .map(this::convertToResDto)
                .collect(Collectors.toList());
    }

    /**
     * 플래너 삭제 (특정 날짜 일정 삭제)
     */
    public void deletePlanner(String sessionId, String date) {
        if (sessionId == null) throw new CustomException(ErrorCode.PLAN_SESSION_MISSING);
        plannerRepository.deletePlan(sessionId, date);
    }

    // --- Helper Methods (매핑) ---

    private ChatEntity.PlaceItem convertToEntityItem(PlaceItemDto dto) {
        ChatEntity.PlaceItem item = new ChatEntity.PlaceItem();
        item.setOrder(dto.getOrder());
        item.setPlaceName(dto.getPlaceName());
        item.setTime(dto.getTime());
        item.setCategory(dto.getCategory());
        item.setLat(dto.getLat());
        item.setLng(dto.getLng());
        item.setAddress(dto.getAddress());
        item.setMapId(dto.getMapId());
        return item;
    }

    private PlaceItemDto convertToDtoItem(ChatEntity.PlaceItem item) {
        return PlaceItemDto.builder()
                .order(item.getOrder())
                .placeName(item.getPlaceName())
                .time(item.getTime())
                .category(item.getCategory())
                .lat(item.getLat())
                .lng(item.getLng())
                .address(item.getAddress())
                .mapId(item.getMapId())
                .build();
    }

    private PlannerResDto convertToResDto(ChatEntity entity) {
        // PK에서 "SESSION#" 제거하고 ID만 추출하는 로직이 필요할 수도 있음
        String rawSessionId = entity.getPk().replace("SESSION#", "");

        return PlannerResDto.builder()
                .sessionId(rawSessionId)
                .date(entity.getDate())
                .places(entity.getPlaces() != null ?
                        entity.getPlaces().stream().map(this::convertToDtoItem).collect(Collectors.toList())
                        : Collections.emptyList())
                .build();
    }
}