package com.jeju.ormicamp.infrastructure.repository.planner;

import com.jeju.ormicamp.model.domain.TravelDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TravelDateRepository extends JpaRepository<TravelDate, Long> {
}
