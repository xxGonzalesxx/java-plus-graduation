package ru.practicum.request.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.service.ParticipationRequestService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class ParticipationRequestController {
    private final ParticipationRequestService requestService;

    @GetMapping("/{userId}/requests")
    public List<ParticipationRequestDto> getRequestByUserId(@PathVariable Long userId) {
        return requestService.getRequestByUserId(userId);
    }

    @PostMapping("/{userId}/events/{eventId}/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto addRequest(@PathVariable Long userId,
                                              @PathVariable Long eventId) {  // ← eventId из PATH!
        return requestService.addRequest(userId, eventId);
    }

    @PatchMapping("/{userId}/requests/{requestId}/cancel")
    public ParticipationRequestDto cancelRequest(@PathVariable Long userId,
                                                 @PathVariable Long requestId) {
        return requestService.cancelRequest(userId, requestId);
    }

    @GetMapping("/{userId}/events/{eventId}/requests")
    public List<ParticipationRequestDto> getEventRequests(@PathVariable Long userId,
                                                          @PathVariable Long eventId) {
        return requestService.getEventRequests(userId, eventId);
    }

    @PatchMapping("/{userId}/events/{eventId}/requests")
    public EventRequestStatusUpdateResult updateRequestStatus(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @Valid @RequestBody EventRequestStatusUpdateRequest requestUpdate) {
        return requestService.updateRequestStatus(userId, eventId, requestUpdate);
    }
}