package ru.practicum.event.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.client.RecommendationClient;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.dto.PublicEventParamDto;
import ru.practicum.event.service.EventService;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class PublicEventController {

    private final EventService eventService;
    private final RecommendationClient recommendationClient;
    private static final String USER_ID_HEADER = "X-EWM-USER-ID";

    @GetMapping
    public List<EventShortDto> getPublicEvents(@Valid PublicEventParamDto param,
                                               HttpServletRequest request) {
        log.info("GET /event: {}", param);
        return eventService.getEventsPublic(param, request);
    }

    @GetMapping("/{id}")
    public EventFullDto getPublicEventById(@PathVariable Long id,
                                           HttpServletRequest request) {
        log.info("GET /event/{id}: id={}", id);

        EventFullDto event = eventService.getEventByIdPublic(id, request);
        Long userId = getUserIdFromRequest(request);

        if (userId != null) {
            recommendationClient.sendView(userId, id);
        }

        Double rating = recommendationClient.getEventRating(id);
        event.setRating(rating);

        return event;
    }

    @GetMapping("/recommendations")
    public List<EventShortDto> getRecommendations(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @RequestParam(defaultValue = "10") int limit) {

        log.info("Getting recommendations for user: {}", userId);

        List<RecommendedEventProto> recommendations = recommendationClient
                .getRecommendations(userId, limit);

        List<Long> eventIds = recommendations.stream()
                .map(RecommendedEventProto::getEventId)
                .toList();

        return eventService.getEventsByIds(eventIds);
    }

    @PutMapping("/{eventId}/like")
    public void likeEvent(
            @PathVariable Long eventId,
            @RequestHeader(USER_ID_HEADER) Long userId) {
        log.info("User {} liked event {}", userId, eventId);

        eventService.validateUserParticipation(userId, eventId);
        recommendationClient.sendLike(userId, eventId);
    }

    @GetMapping("/internal/{eventId}")
    public EventFullDto getEventByIdForMicroservice(@PathVariable Long eventId) {
        log.info("Internal API: get event by id = {}", eventId);
        return eventService.getEventByIdForMicroservice(eventId);
    }

    private Long getUserIdFromRequest(HttpServletRequest request) {
        String userIdHeader = request.getHeader(USER_ID_HEADER);
        if (userIdHeader == null || userIdHeader.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(userIdHeader);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}