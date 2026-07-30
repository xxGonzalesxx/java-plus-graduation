package ru.practicum.event.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.EventRequestStatusUpdateRequest;
import ru.practicum.dto.EventRequestStatusUpdateResult;
import ru.practicum.dto.ParticipationRequestDto;

import java.util.List;
import java.util.Map;

@FeignClient(name = "request-service")
public interface RequestClient {

    @GetMapping("/internal/requests/confirmed-count")
    Map<Long, Long> getConfirmedRequestsCount(@RequestParam List<Long> eventIds);

    @GetMapping("/internal/requests/confirmed-count/{eventId}")
    Long getConfirmedRequestsCountByEvent(@PathVariable Long eventId);

    @GetMapping("/internal/requests/event/{eventId}")
    List<ParticipationRequestDto> getRequestsByEventId(@PathVariable Long eventId);

    @PatchMapping("/internal/requests")
    EventRequestStatusUpdateResult updateRequests(@RequestBody EventRequestStatusUpdateRequest request);

}