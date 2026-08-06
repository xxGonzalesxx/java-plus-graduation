package ru.practicum.request.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.exception.ValidationException;
import ru.practicum.request.client.RecommendationClient;
import ru.practicum.request.dto.NewRequestDto;
import ru.practicum.dto.ParticipationRequestDto;
import ru.practicum.request.service.ParticipationRequestService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
@Slf4j
public class ParticipationRequestController {

    private final ParticipationRequestService requestService;
    private final RecommendationClient recommendationClient;

    @PostMapping("/{userId}/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto addRequest(
            @PathVariable Long userId,
            @RequestParam(required = false) Long eventId,
            @RequestBody(required = false) NewRequestDto requestDto) {

        if (eventId == null && requestDto != null) {
            eventId = requestDto.eventId();
        }

        log.info("POST /users/{}/requests with eventId={}", userId, eventId);

        if (eventId == null) {
            throw new ValidationException("eventId is required");
        }

        recommendationClient.sendRegister(userId, eventId);

        return requestService.addRequest(userId, eventId);
    }

    @GetMapping("/{userId}/requests")
    public List<ParticipationRequestDto> getRequestByUserId(@PathVariable Long userId) {
        log.info("GET /users/{}/requests", userId);
        return requestService.getRequestByUserId(userId);
    }

    @PostMapping("/{userId}/events/{eventId}/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto addRequestDirect(@PathVariable Long userId,
                                                    @PathVariable Long eventId) {
        log.info("POST /users/{}/events/{}/requests", userId, eventId);

        recommendationClient.sendRegister(userId, eventId);

        return requestService.addRequest(userId, eventId);
    }

    @PatchMapping("/{userId}/requests/{requestId}/cancel")
    public ParticipationRequestDto cancelRequest(@PathVariable Long userId,
                                                 @PathVariable Long requestId) {
        log.info("PATCH /users/{}/requests/{}/cancel", userId, requestId);
        return requestService.cancelRequest(userId, requestId);
    }
}