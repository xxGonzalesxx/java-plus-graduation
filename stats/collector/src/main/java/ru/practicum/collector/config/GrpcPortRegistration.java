package ru.practicum.collector.config;

import com.netflix.appinfo.ApplicationInfoManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.grpc.server.lifecycle.GrpcServerStartedEvent;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class GrpcPortRegistration {

    private final ApplicationInfoManager applicationInfoManager;

    @EventListener
    public void onGrpcServerStarted(GrpcServerStartedEvent event) {
        int grpcPort = event.getPort();
        log.info("Updating Eureka metadata with gRPC port: {}", grpcPort);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("grpcPort", String.valueOf(grpcPort));
        applicationInfoManager.registerAppMetadata(metadata);
    }
}