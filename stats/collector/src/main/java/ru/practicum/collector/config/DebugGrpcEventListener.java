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
            // НОВОЕ: пытаемся вытащить порт через рефлексию, не зависим от точного типа
            try {
                var method = event.getClass().getMethod("getPort");
                Object port = method.invoke(event);
                log.info(">>> DEBUG GRPC EVENT PORT: {}", port);
            } catch (NoSuchMethodException ignored) {
                // у этого события нет getPort() — не критично
            } catch (Exception e) {
                log.warn(">>> DEBUG GRPC EVENT: не удалось получить порт: {}", e.getMessage());
            }
        }
    }
}