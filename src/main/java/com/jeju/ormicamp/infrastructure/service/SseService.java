package com.jeju.ormicamp.infrastructure.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SseService {

    private final Map<Long, SseEmitter> sseEmitters = new ConcurrentHashMap<>();

    public SseEmitter connect(Long userId) {
        SseEmitter emitter = new SseEmitter(0L);
        sseEmitters.put(userId, emitter);

        emitter.onCompletion(() -> sseEmitters.remove(userId));
        emitter.onTimeout(() -> sseEmitters.remove(userId));

        return emitter;
    }

    public void sendNewDisasterEvent(Long disasterMessageId) {
        sseEmitters.forEach((userId, emitter) -> {
            try {
                emitter.send(
                        SseEmitter.event()
                                .name("disaster")
                                .data(disasterMessageId)
                );
            } catch (IOException e) {
                sseEmitters.remove(userId);
            }
        });
    }
}
