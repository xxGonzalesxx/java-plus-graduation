package client;

import ewm.HitDto;
import ewm.ParamDto;
import ewm.StatsDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.MaxAttemptsRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
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
    private final RetryTemplate retryTemplate;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public StatClient(
            DiscoveryClient discoveryClient,
            @Value("${stats-client.retry.backoff-period:3000}") long backOffPeriod,
            @Value("${stats-client.retry.max-attempts:3}") int maxAttempts) {
        this.discoveryClient = discoveryClient;
        this.restClient = RestClient.builder().build();
        this.retryTemplate = createRetryTemplate(backOffPeriod, maxAttempts);
    }

    private static RetryTemplate createRetryTemplate(long backOffPeriod, int maxAttempts) {
        RetryTemplate retryTemplate = new RetryTemplate();

        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(backOffPeriod);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        MaxAttemptsRetryPolicy retryPolicy = new MaxAttemptsRetryPolicy();
        retryPolicy.setMaxAttempts(maxAttempts);
        retryTemplate.setRetryPolicy(retryPolicy);

        return retryTemplate;
    }

    private String resolveBaseUrl() {
        return retryTemplate.execute(ctx -> {
            List<ServiceInstance> instances = discoveryClient.getInstances(STATS_SERVICE_ID);
            if (instances.isEmpty()) {
                throw new IllegalStateException(
                        "Сервис статистики не найден в Eureka: " + STATS_SERVICE_ID);
            }
            ServiceInstance instance = instances.getFirst();
            return "http://" + instance.getHost() + ":" + instance.getPort();
        });
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
            log.warn("Не удалось отправить hit в сервис статистики", e);
        }
    }

    public List<StatsDto> get(ParamDto paramDto) {
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

            return restClient.get()
                    .uri(uri)
                    .header("Content-Type", "application/json")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
        } catch (Exception e) {
            log.warn("Не удалось получить статистику из сервиса статистики", e);
            return List.of();
        }
    }
}