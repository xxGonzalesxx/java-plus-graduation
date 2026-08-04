package ru.practicum.request.client;

import client.CollectorClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationClient {

    private final CollectorClient collectorClient;

    public void sendRegister(long userId, long eventId) {
        collectorClient.sendRegister(userId, eventId);
        log.debug("Register sent to Collector: userId={}, eventId={}", userId, eventId);
    }
}