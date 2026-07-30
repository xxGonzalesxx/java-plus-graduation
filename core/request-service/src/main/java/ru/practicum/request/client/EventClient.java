package ru.practicum.request.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.request.client.dto.EventInfo;

@FeignClient(name = "event-service")
public interface EventClient {
    @GetMapping("/events/internal/{eventId}")
    EventInfo getEventByIdForMicroservice(@PathVariable Long eventId);
}