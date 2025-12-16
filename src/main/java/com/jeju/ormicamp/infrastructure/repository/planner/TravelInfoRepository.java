package com.jeju.ormicamp.infrastructure.repository.planner;

import com.jeju.ormicamp.model.domain.TravelInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TravelInfoRepository extends JpaRepository<TravelInfo, Long> {
}
