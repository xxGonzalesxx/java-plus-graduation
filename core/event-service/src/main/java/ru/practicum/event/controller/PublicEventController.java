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

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Slf4j
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class PublicEventController {

    private final EventService eventService;
    private final RecommendationClient recommendationClient;

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
            @RequestHeader("X-EWM-USER-ID") Long userId,
            @RequestParam(defaultValue = "10") int limit) {

        log.info("Getting recommendations for user: {}", userId);

        List<RecommendedEventProto> recommendations = recommendationClient
                .getRecommendations(userId, limit);

        if (recommendations.isEmpty()) {
            return List.of();
        }

        List<Long> eventIds = recommendations.stream()
                .map(RecommendedEventProto::getEventId)
                .toList();

        // НОВОЕ: сохраняем порядок и score, пришедшие от Analyzer
        Map<Long, Integer> orderIndex = IntStream.range(0, eventIds.size())
                .boxed()
                .collect(java.util.stream.Collectors.toMap(eventIds::get, i -> i));

        Map<Long, Double> scoreByEventId = new java.util.HashMap<>();
        for (RecommendedEventProto r : recommendations) {
            scoreByEventId.put(r.getEventId(), (double) r.getScore());
        }

        List<EventShortDto> events = eventService.getEventsByIds(eventIds);

        events.forEach(e -> e.setRating(scoreByEventId.getOrDefault(e.getId(), 0.0)));
        events.sort(Comparator.comparingInt(e -> orderIndex.getOrDefault(e.getId(), Integer.MAX_VALUE)));

        return events;
    }

    @PutMapping("/{eventId}/like")
    public void likeEvent(
            @PathVariable Long eventId,
            @RequestHeader("X-EWM-USER-ID") Long userId) {

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
        String userIdHeader = request.getHeader("X-EWM-USER-ID");
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