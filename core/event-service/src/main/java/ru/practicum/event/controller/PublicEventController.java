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

        // 1. Получаем событие
        EventFullDto event = eventService.getEventByIdPublic(id, request);

        // 2. Получаем userId из заголовка (если есть)
        Long userId = getUserIdFromRequest(request);

        // 3. Если пользователь авторизован — отправляем просмотр в Collector
        if (userId != null) {
            recommendationClient.sendView(userId, id);
        }

        // 4. Получаем рейтинг из Analyzer и устанавливаем в DTO
        Double rating = recommendationClient.getEventRating(id);
        event.setRating(rating);

        return event;
    }

    /**
     * GET /events/recommendations — рекомендации для пользователя
     */
    @GetMapping("/recommendations")
    public List<EventShortDto> getRecommendations(
            @RequestHeader("X-EWM-USER-ID") Long userId,
            @RequestParam(defaultValue = "10") int limit) {

        log.info("Getting recommendations for user: {}", userId);

        // 1. Получаем рекомендации из Analyzer
        List<RecommendedEventProto> recommendations = recommendationClient
                .getRecommendations(userId, limit);

        // 2. Получаем события из БД по ID
        List<Long> eventIds = recommendations.stream()
                .map(RecommendedEventProto::getEventId)
                .toList();

        return eventService.getEventsByIds(eventIds);
    }

    /**
     * PUT /events/{eventId}/like — лайк мероприятия
     */
    @PutMapping("/{eventId}/like")
    public void likeEvent(
            @PathVariable Long eventId,
            @RequestHeader("X-EWM-USER-ID") Long userId) {

        log.info("User {} liked event {}", userId, eventId);

        // 1. Проверяем, что пользователь участвовал в мероприятии
        eventService.validateUserParticipation(userId, eventId);

        // 2. Отправляем лайк в Collector
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