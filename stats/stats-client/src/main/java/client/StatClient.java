package client;

import ewm.HitDto;
import ewm.ParamDto;
import ewm.StatsDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class StatClient {

    private static final String STATS_SERVICE_ID = "stats-server";

    private final DiscoveryClient discoveryClient;
    private final RestClient restClient;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public StatClient(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
        this.restClient = RestClient.builder().build();
    }

    private String resolveBaseUrl() {
        List<ServiceInstance> instances = discoveryClient.getInstances(STATS_SERVICE_ID);
        if (instances.isEmpty()) {
            throw new IllegalStateException("Сервис статистики не найден в Eureka: " + STATS_SERVICE_ID);
        }
        ServiceInstance instance = instances.get(0);
        return "http://" + instance.getHost() + ":" + instance.getPort();
    }

    public void hit(HitDto hitDto) {
        try {
            restClient.post()
                    .uri(resolveBaseUrl() + "/hit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(hitDto)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.info("Failed to connect to stat service");
        }
    }

    public List<StatsDto> get(ParamDto paramDto) {
        List<StatsDto> stats;

        Optional<List<String>> uris = (paramDto.uris() == null || paramDto.uris().isEmpty())
                ? Optional.empty()
                : Optional.of(paramDto.uris());

        try {
            URI uri = UriComponentsBuilder.fromUriString(resolveBaseUrl() + "/stats")
                    .queryParam("start", paramDto.start().format(formatter))
                    .queryParam("end", paramDto.end().format(formatter))
                    .queryParamIfPresent("uris", uris)
                    .queryParamIfPresent("unique", Optional.ofNullable(paramDto.unique()))
                    .build()
                    .toUri();

            stats = restClient.get()
                    .uri(uri)
                    .header("Content-Type", "application/json")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
        } catch (Exception e) {
            log.info("Failed to connect to stat service");
            stats = List.of(new StatsDto("ewm-main-service", "/fake-uri", 0L));
        }
        return stats;
    }
}