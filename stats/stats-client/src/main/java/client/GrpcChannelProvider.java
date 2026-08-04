package client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class GrpcChannelProvider {

    private static final String GRPC_PORT_METADATA_KEY = "grpcPort";

    private final DiscoveryClient discoveryClient;
    private final Map<String, ManagedChannel> channels = new ConcurrentHashMap<>();

    public GrpcChannelProvider(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    /**
     * Возвращает закэшированный канал к сервису, создавая его при первом обращении.
     *
     * @param serviceId идентификатор сервиса, под которым он зарегистрирован в Eureka
     *                  (обычно совпадает со spring.application.name, регистр не важен)
     */
    public ManagedChannel getChannel(String serviceId) {
        return channels.computeIfAbsent(serviceId, this::createChannel);
    }

    /**
     * Сбрасывает закэшированный канал (например, если сервис перезапустился
     * и получил новый случайный gRPC-порт).
     */
    public void invalidate(String serviceId) {
        ManagedChannel channel = channels.remove(serviceId);
        if (channel != null) {
            channel.shutdown();
        }
    }

    private ManagedChannel createChannel(String serviceId) {
        ServiceInstance instance = resolveInstance(serviceId);
        int grpcPort = extractGrpcPort(instance, serviceId);
        log.info("Creating gRPC channel to '{}' at {}:{}", serviceId, instance.getHost(), grpcPort);
        return ManagedChannelBuilder.forAddress(instance.getHost(), grpcPort)
                .usePlaintext()
                .build();
    }

    private ServiceInstance resolveInstance(String serviceId) {
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
        if (instances.isEmpty()) {
            throw new IllegalStateException(
                    "No instances found in Eureka for service '" + serviceId
                            + "'. Is it registered and healthy?");
        }
        return instances.get(0);
    }

    private int extractGrpcPort(ServiceInstance instance, String serviceId) {
        String grpcPort = instance.getMetadata().get(GRPC_PORT_METADATA_KEY);
        if (grpcPort == null || grpcPort.isBlank() || "0".equals(grpcPort)) {
            throw new IllegalStateException(
                    "'" + GRPC_PORT_METADATA_KEY + "' metadata is missing or zero for service '" + serviceId
                            + "' (instance " + instance.getInstanceId() + "). "
                            + "Make sure GrpcPortMetadataPublisher is registered on that service.");
        }
        return Integer.parseInt(grpcPort);
    }
}