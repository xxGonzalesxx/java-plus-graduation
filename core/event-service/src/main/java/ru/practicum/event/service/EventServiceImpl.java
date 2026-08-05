package ru.practicum.event.service;

import client.StatClient;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.HitDto;
import ru.practicum.dto.CategoryDto;
import ru.practicum.dto.ParticipationRequestDto;
import ru.practicum.dto.UserShortDto;
import ru.practicum.event.client.CategoryClient;
import ru.practicum.event.client.RecommendationClient;
import ru.practicum.event.client.RequestClient;
import ru.practicum.event.client.UserClient;
import ru.practicum.event.dto.*;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.Location;
import ru.practicum.event.model.QEvent;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.dto.EventRequestStatusUpdateRequest;
import ru.practicum.dto.EventRequestStatusUpdateResult;
import ru.practicum.model.EventState;
import ru.practicum.model.ParticipationStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final StatClient statClient;
    private final CategoryClient categoryClient;
    private final UserClient userClient;
    private final RequestClient requestClient;
    private final RecommendationClient recommendationClient; // НОВОЕ

    @Override
    public List<EventShortDto> getEventsPrivate(Long userId, Integer from, Integer size) {
        log.info("Getting events for user id={}, from={}, size={}", userId, from, size);
        checkUserExists(userId);

        Pageable pageable = PageRequest.of(from / size, size, Sort.by("id"));
        List<Event> events = eventRepository.findByInitiatorId(userId, pageable);

        if (events.isEmpty()) {
            return List.of();
        }
        return buildShortDtoList(events);
    }

    @Override
    @Transactional
    public EventFullDto addEventPrivate(Long userId, NewEventDto dto) {
        log.info("Adding event for user id={}", userId);

        UserShortDto user = checkUserExists(userId);

        if (dto.eventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ConflictException("Event date must be at least 2 hours from now");
        }

        CategoryDto category = getCategoryOrThrow(dto.category());

        Event event = eventMapper.toEvent(dto);
        event.setInitiatorId(user.id());
        event.setState(EventState.PENDING);
        event.setCreatedOn(LocalDateTime.now());

        Event saved = eventRepository.save(event);
        log.info("Event created successfully: id={}", saved.getId());

        return enrichFullDto(saved, category, user);
    }

    @Override
    public EventFullDto getEventByIdPrivate(Long userId, Long eventId, String url) {
        log.info("Getting event id={} for user id={}", eventId, userId);

        checkUserExists(userId);
        Event event = getEventOrThrow(eventId);

        if (!event.getInitiatorId().equals(userId)) {
            throw new ConflictException("Event does not belong to user");
        }

        EventFullDto fullDto = eventMapper.toFullDto(event);
        fullDto.setConfirmedRequests(getConfirmedRequestsCount(eventId));
        fullDto.setRating(recommendationClient.getEventRating(eventId)); // было: getViews(paramDto)
        setCategoryAndInitiator(fullDto, event);

        return fullDto;
    }

    @Override
    @Transactional
    public EventFullDto updateEventPrivate(Long userId, Long eventId, UpdateEventUserRequest dto) {
        log.info("Updating event id={} for user id={}", eventId, userId);

        checkUserExists(userId);
        Event event = getEventOrThrow(eventId);

        if (!event.getInitiatorId().equals(userId)) {
            throw new ConflictException("Event does not belong to user");
        }

        if (event.getState() != EventState.PENDING && event.getState() != EventState.CANCELED) {
            throw new ConflictException("Only pending or canceled events can be changed");
        }

        if (dto.eventDate() != null &&
                dto.eventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ConflictException("Event date must be at least 2 hours from now");
        }

        eventMapper.updateEventMap(dto, event);

        if (dto.category() != null) {
            CategoryDto category = getCategoryOrThrow(dto.category());
            event.setCategoryId(category.id());
        }

        if (dto.location() != null) {
            event.setLocation(new Location(dto.location().lat(), dto.location().lon()));
        }

        if (dto.stateAction() != null) {
            switch (dto.stateAction()) {
                case SEND_TO_REVIEW -> event.setState(EventState.PENDING);
                case CANCEL_REVIEW -> event.setState(EventState.CANCELED);
            }
        }

        Event updated = eventRepository.save(event);
        log.info("Event updated successfully: id={}", updated.getId());

        EventFullDto fullDto = eventMapper.toFullDto(updated);
        setCategoryAndInitiator(fullDto, updated);
        return fullDto;
    }

    @Override
    public List<EventShortDto> getEventsPublic(PublicEventParamDto eventParamDto, jakarta.servlet.http.HttpServletRequest request) {
        if (eventParamDto.rangeStart() != null && eventParamDto.rangeEnd() != null &&
                eventParamDto.rangeStart().isAfter(eventParamDto.rangeEnd())) {
            throw new ValidationException("End date cannot be before start date");
        }

        // УБРАНО: saveHit(request); — по ТЗ для GET /events хит больше не нужен

        QEvent event = QEvent.event;
        BooleanBuilder paramFilter = new BooleanBuilder();

        if (eventParamDto.text() != null && !eventParamDto.text().isBlank()) {
            paramFilter.and(event.annotation.containsIgnoreCase(eventParamDto.text())
                    .or(event.description.containsIgnoreCase(eventParamDto.text())));
        }

        if (eventParamDto.category() != null && !eventParamDto.category().isEmpty()) {
            paramFilter.and(event.categoryId.in(eventParamDto.category()));
        }

        if (eventParamDto.paid() != null) {
            paramFilter.and(event.paid.eq(eventParamDto.paid()));
        }

        LocalDateTime start = eventParamDto.rangeStart() != null ? eventParamDto.rangeStart() : LocalDateTime.now();
        paramFilter.and(event.eventDate.goe(start));

        if (eventParamDto.rangeEnd() != null) {
            paramFilter.and(event.eventDate.loe(eventParamDto.rangeEnd()));
        }

        paramFilter.and(event.state.eq(EventState.PUBLISHED));

        Sort sortEventDate = Sort.unsorted();
        if (eventParamDto.sort() != null && eventParamDto.sort().equalsIgnoreCase("EVENT_DATE")) {
            sortEventDate = Sort.by("eventDate").ascending();
        }

        Pageable pageable = PageRequest.of(eventParamDto.from() / eventParamDto.size(),
                eventParamDto.size(), sortEventDate);

        List<Event> events = eventRepository.findAll(paramFilter, pageable).getContent();

        if (events.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> confirmedRequestsMap = getConfirmedRequestsMap(events);

        List<Event> filteredEvents = events;
        if (eventParamDto.onlyAvailable()) {
            filteredEvents = events.stream()
                    .filter(e -> e.getParticipantLimit() == 0
                            || e.getParticipantLimit() > confirmedRequestsMap.getOrDefault(e.getId(), 0L))
                    .toList();
        }

        Map<Long, Double> ratingMap = getRatingMap(filteredEvents); // было: getViewsMap

        List<EventShortDto> shortsDto = buildShortDtoListWithMaps(filteredEvents, confirmedRequestsMap, ratingMap);

        if (eventParamDto.sort() != null && eventParamDto.sort().equalsIgnoreCase("VIEWS")) {
            shortsDto.sort(java.util.Comparator.comparing(EventShortDto::getRating).reversed());
        }

        log.info("Получен список запросов по указанным фильтрам");
        return shortsDto;
    }

    @Override
    public EventFullDto getEventByIdPublic(Long id, jakarta.servlet.http.HttpServletRequest request) {
        // saveHit оставлен: по ТЗ отдельный GET /events/{id} обязан фиксировать просмотр
        saveHit(request);

        Event event = getEventOrThrow(id);

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Event must be published");
        }

        EventFullDto fullDto = eventMapper.toFullDto(event);
        fullDto.setConfirmedRequests(getConfirmedRequestsCount(id));
        fullDto.setRating(recommendationClient.getEventRating(id)); // было: getViews(paramDto)
        setCategoryAndInitiator(fullDto, event);

        log.info("Получено событие с id = {}", id);
        return fullDto;
    }

    @Override
    public List<EventFullDto> searchEventsAdmin(AdminEventSearchFilter filter) {
        log.info("Search events with filters: {}", filter);

        if (filter.rangeStart() != null && filter.rangeEnd() != null &&
                filter.rangeStart().isAfter(filter.rangeEnd())) {
            throw new ValidationException("rangeEnd не может быть раньше rangeStart");
        }

        QEvent event = QEvent.event;
        BooleanBuilder predicate = new BooleanBuilder();

        if (filter.users() != null && !filter.users().isEmpty()) {
            predicate.and(event.initiatorId.in(filter.users()));
        }
        if (filter.states() != null && !filter.states().isEmpty()) {
            predicate.and(event.state.in(filter.states()));
        }
        if (filter.categories() != null && !filter.categories().isEmpty()) {
            predicate.and(event.categoryId.in(filter.categories()));
        }
        if (filter.rangeStart() != null) {
            predicate.and(event.eventDate.goe(filter.rangeStart()));
        }
        if (filter.rangeEnd() != null) {
            predicate.and(event.eventDate.loe(filter.rangeEnd()));
        }

        Pageable pageable = PageRequest.of(filter.from() / filter.size(), filter.size());
        List<Event> events = eventRepository.findAll(predicate, pageable).getContent();

        if (events.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> confirmedRequestsMap = getConfirmedRequestsMap(events);
        Map<Long, Double> ratingMap = getRatingMap(events); // было: getViewsMap
        Map<Long, CategoryDto> categoryMap = getCategoryMap(events);
        Map<Long, UserShortDto> userMap = getUserMap(events);

        return events.stream()
                .map(e -> {
                    EventFullDto fullDto = eventMapper.toFullDto(e);
                    fullDto.setConfirmedRequests(confirmedRequestsMap.getOrDefault(e.getId(), 0L));
                    fullDto.setRating(ratingMap.getOrDefault(e.getId(), 0.0));
                    fullDto.setCategory(categoryMap.get(e.getCategoryId()));
                    fullDto.setInitiator(userMap.get(e.getInitiatorId()));
                    return fullDto;
                })
                .toList();
    }

    @Override
    @Transactional
    public EventFullDto updateEventAdmin(Long eventId, UpdateEventAdminRequest dto) {
        log.info("Update event with ID: {}", eventId);

        Event event = existsEvent(eventId);

        if (dto.eventDate() != null && dto.eventDate().isBefore(LocalDateTime.now().plusHours(1))) {
            throw new ValidationException("Дата события должна быть не раньше, чем через час");
        }

        if (dto.annotation() != null) event.setAnnotation(dto.annotation());
        if (dto.description() != null) event.setDescription(dto.description());
        if (dto.eventDate() != null) event.setEventDate(dto.eventDate());
        if (dto.paid() != null) event.setPaid(dto.paid());
        if (dto.participantLimit() != null) event.setParticipantLimit(dto.participantLimit());
        if (dto.requestModeration() != null) event.setRequestModeration(dto.requestModeration());
        if (dto.title() != null) event.setTitle(dto.title());

        if (dto.location() != null) {
            event.setLocation(new Location(dto.location().lat(), dto.location().lon()));
        }

        if (dto.category() != null) {
            CategoryDto category = getCategoryOrThrow(dto.category());
            event.setCategoryId(category.id());
        }

        if (dto.stateAction() != null) {
            switch (dto.stateAction()) {
                case PUBLISH_EVENT -> {
                    if (event.getState() != EventState.PENDING) {
                        throw new ConflictException(
                                "An event cannot be published unless it is in the required status (PENDING): "
                                        + event.getState());
                    }
                    event.setState(EventState.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());
                    log.info("Event с id={} успешно опубликовано", eventId);
                }
                case REJECT_EVENT -> {
                    if (event.getState() == EventState.PUBLISHED) {
                        throw new ConflictException("Cannot reject the event because it's already published");
                    }
                    event.setState(EventState.CANCELED);
                    log.info("Event с id={} отклонено", eventId);
                }
            }
        }

        Event updated = eventRepository.save(event);
        log.info("Event c id={} успешно обновлено", updated.getId());

        EventFullDto fullDto = eventMapper.toFullDto(updated);
        setCategoryAndInitiator(fullDto, updated);
        return fullDto;
    }

    @Override
    public Event existsEvent(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
    }

    @Override
    public EventFullDto getEventByIdForMicroservice(Long eventId) {
        log.info("Internal request: getting event by id = {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        Long confirmedRequests = getConfirmedRequestsCount(eventId);
        double rating = recommendationClient.getEventRating(eventId); // было: viewsMap

        EventFullDto fullDto = eventMapper.toFullDto(event);
        fullDto.setConfirmedRequests(confirmedRequests);
        fullDto.setRating(rating);
        setCategoryAndInitiator(fullDto, event);

        log.info("Internal: event {} returned with status {}", eventId, event.getState());
        return fullDto;
    }

    // ==================== ПРИВАТНЫЕ МЕТОДЫ ====================

    private UserShortDto checkUserExists(Long userId) {
        try {
            return userClient.getUser(userId);
        } catch (Exception e) {
            throw new NotFoundException("User with id=" + userId + " was not found");
        }
    }

    private CategoryDto getCategoryOrThrow(Long categoryId) {
        try {
            return categoryClient.getCategoryById(categoryId);
        } catch (Exception e) {
            throw new NotFoundException("Category with id=" + categoryId + " was not found");
        }
    }

    private Event getEventOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
    }

    private void saveHit(jakarta.servlet.http.HttpServletRequest request) {
        HitDto hitDto = new HitDto(
                "ru.practicum-event-service",
                request.getRequestURI(),
                request.getRemoteAddr(),
                LocalDateTime.now());
        try {
            statClient.hit(hitDto);
            log.info("Статистика сохранена для URI: {}", request.getRequestURI());
        } catch (Exception e) {
            log.warn("Не удалось сохранить статистику: {}", e.getMessage());
        }
    }

    // НОВОЕ: замена getViewsMap — рейтинг из Analyzer батчем
    private Map<Long, Double> getRatingMap(List<Event> events) {
        try {
            List<Long> eventIds = events.stream().map(Event::getId).toList();
            return recommendationClient.getEventsRating(eventIds);
        } catch (Exception e) {
            log.warn("Не удалось получить рейтинг событий: {}", e.getMessage());
            return Map.of();
        }
    }

    private Long getConfirmedRequestsCount(Long eventId) {
        try {
            return requestClient.getConfirmedRequestsCountByEvent(eventId);
        } catch (Exception e) {
            log.warn("Не удалось получить количество подтверждённых заявок для события {}: {}", eventId, e.getMessage());
            return 0L;
        }
    }

    private Map<Long, Long> getConfirmedRequestsMap(List<Event> events) {
        try {
            List<Long> eventIds = events.stream().map(Event::getId).toList();
            return requestClient.getConfirmedRequestsCount(eventIds);
        } catch (Exception e) {
            log.warn("Не удалось получить статистику подтверждённых заявок: {}", e.getMessage());
            return Map.of();
        }
    }

    private Map<Long, CategoryDto> getCategoryMap(List<Event> events) {
        return events.stream()
                .map(Event::getCategoryId)
                .distinct()
                .collect(Collectors.toMap(id -> id, this::getCategoryOrThrow));
    }

    private Map<Long, UserShortDto> getUserMap(List<Event> events) {
        return events.stream()
                .map(Event::getInitiatorId)
                .distinct()
                .collect(Collectors.toMap(id -> id, this::checkUserExists));
    }

    private void setCategoryAndInitiator(EventFullDto fullDto, Event event) {
        fullDto.setCategory(getCategoryOrThrow(event.getCategoryId()));
        fullDto.setInitiator(checkUserExists(event.getInitiatorId()));
    }

    private EventFullDto enrichFullDto(Event event, CategoryDto category, UserShortDto initiator) {
        EventFullDto fullDto = eventMapper.toFullDto(event);
        fullDto.setCategory(category);
        fullDto.setInitiator(initiator);
        fullDto.setConfirmedRequests(0L);
        fullDto.setRating(0.0);
        return fullDto;
    }

    private List<EventShortDto> buildShortDtoList(List<Event> events) {
        Map<Long, Long> confirmedRequestsMap = getConfirmedRequestsMap(events);
        Map<Long, Double> ratingMap = getRatingMap(events);
        return buildShortDtoListWithMaps(events, confirmedRequestsMap, ratingMap);
    }

    private List<EventShortDto> buildShortDtoListWithMaps(List<Event> events,
                                                          Map<Long, Long> confirmedRequestsMap,
                                                          Map<Long, Double> ratingMap) {
        Map<Long, CategoryDto> categoryMap = getCategoryMap(events);
        Map<Long, UserShortDto> userMap = getUserMap(events);

        return events.stream()
                .map(e -> {
                    EventShortDto shortDto = eventMapper.toShortDto(e);
                    shortDto.setConfirmedRequests(confirmedRequestsMap.getOrDefault(e.getId(), 0L));
                    shortDto.setRating(ratingMap.getOrDefault(e.getId(), 0.0));
                    shortDto.setCategory(categoryMap.get(e.getCategoryId()));
                    shortDto.setInitiator(userMap.get(e.getInitiatorId()));
                    return shortDto;
                })
                .toList();
    }

    @Override
    public EventInfoDto getEventInfoById(Long eventId) {
        log.info("Internal: getting event info by id = {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        return new EventInfoDto(
                event.getId(),
                new EventInfoDto.Initiator(event.getInitiatorId()),
                event.getState().name(),
                event.getParticipantLimit(),
                event.getRequestModeration()
        );
    }

    @Override
    public List<ParticipationRequestDto> getRequestsOfEvent(Long userId, Long eventId) {
        log.info("Getting requests for event id={} by user id={}", eventId, userId);

        checkUserExists(userId);
        Event event = getEventOrThrow(eventId);

        if (!event.getInitiatorId().equals(userId)) {
            throw new ConflictException("User is not the initiator of this event");
        }

        try {
            return requestClient.getRequestsByEventId(eventId);
        } catch (Exception e) {
            log.error("Failed to get requests for event {}: {}", eventId, e.getMessage());
            throw new RuntimeException("Failed to get requests", e);
        }
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult patchRequestsStatusOfEvent(
            Long userId, Long eventId, EventRequestStatusUpdateRequest request) {

        log.info("Updating requests status for event id={} by user id={}", eventId, userId);

        checkUserExists(userId);
        Event event = getEventOrThrow(eventId);

        if (!event.getInitiatorId().equals(userId)) {
            throw new ConflictException("User is not the initiator of this event");
        }

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Event is not published");
        }

        if (event.getParticipantLimit() > 0) {
            Long confirmedCount = getConfirmedRequestsCount(eventId);
            if (confirmedCount >= event.getParticipantLimit()) {
                throw new ConflictException("Participant limit has been reached");
            }
        }

        try {
            return requestClient.updateRequests(request);
        } catch (Exception e) {
            log.error("Failed to update requests for event {}: {}", eventId, e.getMessage());
            throw new RuntimeException("Failed to update requests", e);
        }
    }

    @Override
    public List<EventShortDto> getEventsByIds(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return List.of();
        }

        List<Event> events = eventRepository.findAllById(eventIds);

        Map<Long, Long> confirmedRequestsMap = getConfirmedRequestsMap(events);
        Map<Long, Double> ratingMap = getRatingMap(events);
        Map<Long, CategoryDto> categoryMap = getCategoryMap(events);
        Map<Long, UserShortDto> userMap = getUserMap(events);

        return events.stream()
                .map(e -> {
                    EventShortDto shortDto = eventMapper.toShortDto(e);
                    shortDto.setConfirmedRequests(confirmedRequestsMap.getOrDefault(e.getId(), 0L));
                    shortDto.setRating(ratingMap.getOrDefault(e.getId(), 0.0));
                    shortDto.setCategory(categoryMap.get(e.getCategoryId()));
                    shortDto.setInitiator(userMap.get(e.getInitiatorId()));
                    return shortDto;
                })
                .toList();
    }

    @Override
    public void validateUserParticipation(Long userId, Long eventId) {
        try {
            List<ParticipationRequestDto> requests = requestClient.getRequestsByEventId(eventId);
            boolean participated = requests.stream()
                    .anyMatch(r -> r.requester().equals(userId) &&
                            ParticipationStatus.CONFIRMED.equals(r.status()));

            if (!participated) {
                throw new ValidationException("User must participate in event to like it");
            }
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Failed to check user participation: {}", e.getMessage());
            throw new ValidationException("User must participate in event to like it");
        }
    }

    @Override
    public Map<Long, Long> getViewsMap(List<Event> events, boolean unique) {
        // Метод устарел, используем getRatingMap()
        // Оставлен для совместимости с интерфейсом
        log.warn("getViewsMap is deprecated, use getRatingMap instead");
        return Map.of();
    }
}