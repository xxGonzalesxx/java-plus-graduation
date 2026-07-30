package ru.practicum.event.service;

import jakarta.servlet.http.HttpServletRequest;
import ru.practicum.event.dto.*;
import ru.practicum.event.model.Event;
import ru.practicum.dto.EventRequestStatusUpdateRequest;
import ru.practicum.dto.EventRequestStatusUpdateResult;
import ru.practicum.dto.ParticipationRequestDto;

import java.util.List;
import java.util.Map;

public interface EventService {

    List<EventShortDto> getEventsPrivate(Long userId, Integer from, Integer size);

    EventFullDto addEventPrivate(Long userId, NewEventDto newEventDto);

    EventFullDto getEventByIdPrivate(Long userId, Long eventId, String url);

    EventFullDto updateEventPrivate(Long userId, Long eventId, UpdateEventUserRequest updateRequest);

    List<EventShortDto> getEventsPublic(PublicEventParamDto paramDto, HttpServletRequest request);

    EventFullDto getEventByIdPublic(Long id, HttpServletRequest request);

    List<EventFullDto> searchEventsAdmin(AdminEventSearchFilter filter);

    EventFullDto updateEventAdmin(Long eventId, UpdateEventAdminRequest dto);

    Map<Long, Long> getViewsMap(List<Event> events, boolean unique);

    Event existsEvent(Long eventId);

    EventFullDto getEventByIdForMicroservice(Long eventId);

    EventInfoDto getEventInfoById(Long eventId);

    List<ParticipationRequestDto> getRequestsOfEvent(Long userId, Long eventId);

    // ✅ ДОБАВИТЬ ЭТОТ МЕТОД!
    EventRequestStatusUpdateResult patchRequestsStatusOfEvent(Long userId, Long eventId, EventRequestStatusUpdateRequest request);
}