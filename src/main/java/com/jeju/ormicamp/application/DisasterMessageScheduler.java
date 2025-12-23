package com.jeju.ormicamp.application;

import com.jeju.ormicamp.service.disasterMessage.DisasterMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DisasterMessageScheduler {

    private final DisasterMessageService disasterMessageService;

    /*
    * 1분 후 재실행
     */
    @Scheduled(fixedDelay = 600_000)
    public void fetchDisasterMessages() {
        System.out.println("스케줄러 실행!");
        disasterMessageService.fetchAndSaveDisasterMessages();
    }
}
