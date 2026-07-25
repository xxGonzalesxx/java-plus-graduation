package ru.practicum.event.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "request-service")
public interface RequestClient {

    @GetMapping("/internal/requests/confirmed-count")
    Map<Long, Long> getConfirmedRequestsCount(@RequestParam List<Long> eventIds);

    @GetMapping("/internal/requests/confirmed-count/{eventId}")
    Long getConfirmedRequestsCountByEvent(@PathVariable Long eventId);
}