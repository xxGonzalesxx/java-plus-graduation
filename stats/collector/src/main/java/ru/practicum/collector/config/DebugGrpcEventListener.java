package ru.practicum.collector.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DebugGrpcEventListener {

    @EventListener
    public void onAnyEvent(Object event) {
        String className = event.getClass().getName();
        if (className.toLowerCase().contains("grpc")) {
            log.info(">>> DEBUG GRPC EVENT: {}", className);
        }
    }
}