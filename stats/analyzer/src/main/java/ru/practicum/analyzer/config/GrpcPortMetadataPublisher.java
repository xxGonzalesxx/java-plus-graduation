package ru.practicum.analyzer.config;

import com.netflix.appinfo.ApplicationInfoManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class GrpcPortMetadataPublisher {

    private final ApplicationInfoManager applicationInfoManager;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        // Получаем порт из пропертей, а не из события
        int grpcPort = 9096; // или из пропертей
        log.info("Analyzer: Updating Eureka metadata with gRPC port: {}", grpcPort);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("gRPC.port", String.valueOf(grpcPort));
        metadata.put("grpcPort", String.valueOf(grpcPort));
        applicationInfoManager.registerAppMetadata(metadata);
    }
}