package ru.practicum.request.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.EventRequestStatusUpdateRequest;
import ru.practicum.dto.EventRequestStatusUpdateResult;
import ru.practicum.dto.ParticipationRequestDto;
import ru.practicum.request.service.ParticipationRequestService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/requests")
@RequiredArgsConstructor
public class InternalRequestController {

    private final ParticipationRequestService requestService;

    @GetMapping("/confirmed-count")
    public Map<Long, Long> getConfirmedRequestsCount(@RequestParam List<Long> eventIds) {
        return requestService.getConfirmedRequestsCountMap(eventIds);
    }

    @GetMapping("/confirmed-count/{eventId}")
    public Long getConfirmedRequestsCountByEvent(@PathVariable Long eventId) {
        return requestService.getConfirmedRequestsCountForEvent(eventId);
    }

    @GetMapping("/event/{eventId}")
    public List<ParticipationRequestDto> getRequestsByEventId(@PathVariable Long eventId) {
        // Возвращаем все запросы для события (для внутреннего использования)
        return requestService.getRequestsByEventId(eventId);
    }

    @PostMapping
    public EventRequestStatusUpdateResult updateRequests(
            @RequestBody EventRequestStatusUpdateRequest request) {
        // Обновляем статусы запросов (для внутреннего использования)
        return requestService.updateRequests(request);
    }
}