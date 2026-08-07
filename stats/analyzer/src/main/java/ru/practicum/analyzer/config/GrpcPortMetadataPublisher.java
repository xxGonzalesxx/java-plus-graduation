package ru.practicum.analyzer.config;

import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaRegistration;
import org.springframework.context.event.EventListener;
import org.springframework.grpc.server.lifecycle.GrpcServerStartedEvent;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GrpcPortMetadataPublisher {

    private final Registration registration;

    public GrpcPortMetadataPublisher(Registration registration) {
        this.registration = registration;
    }

    @EventListener(GrpcServerStartedEvent.class)
    public void onGrpcServerStarted(GrpcServerStartedEvent event) {
        int port = event.getPort();
        if (registration instanceof EurekaRegistration eurekaRegistration) {
            eurekaRegistration.getApplicationInfoManager()
                    .registerAppMetadata(Map.of(
                            "gRPC.port", String.valueOf(port),
                            "grpcPort", String.valueOf(port)
                    ));
        }
    }
}