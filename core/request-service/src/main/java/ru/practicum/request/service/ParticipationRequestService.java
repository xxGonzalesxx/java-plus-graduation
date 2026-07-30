package ru.practicum.request.service;

import ru.practicum.dto.EventRequestStatusUpdateRequest;
import ru.practicum.dto.EventRequestStatusUpdateResult;
import ru.practicum.dto.ParticipationRequestDto;

import java.util.List;
import java.util.Map;

public interface ParticipationRequestService {
    List<ParticipationRequestDto> getRequestByUserId(Long userId);

    List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId);

    ParticipationRequestDto addRequest(Long userId, Long eventId);

    ParticipationRequestDto cancelRequest(Long userId, Long requestId);

    EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId, EventRequestStatusUpdateRequest requestUpdate);

    // ---- internal, для Feign-вызовов из других сервисов ----
    Map<Long, Long> getConfirmedRequestsCountMap(List<Long> eventIds);

    Long getConfirmedRequestsCountForEvent(Long eventId);

    List<ParticipationRequestDto> getRequestsByEventId(Long eventId);

    EventRequestStatusUpdateResult updateRequests(EventRequestStatusUpdateRequest request);

}
