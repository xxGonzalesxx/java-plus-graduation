package ru.practicum.request.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.request.client.EventClient;
import ru.practicum.request.client.UserClient;
import ru.practicum.request.client.dto.EventInfo;
import ru.practicum.dto.EventRequestStatusUpdateRequest;
import ru.practicum.dto.EventRequestStatusUpdateResult;
import ru.practicum.dto.ParticipationRequestDto;
import ru.practicum.request.model.ParticipationRequest;
import ru.practicum.model.ParticipationStatus;
import ru.practicum.request.repository.ParticipationRequestRepository;
import ru.practicum.request.mapper.ParticipationRequestMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ParticipationRequestServiceImpl implements ParticipationRequestService {

    private final UserClient userClient;
    private final EventClient eventClient;
    private final ParticipationRequestRepository requestRepository;
    private final ParticipationRequestMapper requestMapper;

    @Override
    public List<ParticipationRequestDto> getRequestByUserId(Long userId) {
        checkUserExists(userId);

        List<ParticipationRequest> requests = requestRepository.findByRequesterId(userId);

        log.info("Получен список заявок на участия в событиях пользователя с id = {}", userId);
        return requests.stream()
                .map(requestMapper::mapToRequestDto)
                .toList();
    }

    @Override
    @Transactional
    public ParticipationRequestDto addRequest(Long userId, Long eventId) {
        checkUserExists(userId);

        EventInfo event = getEventOrThrow(eventId);

        long confirmedRequests = requestRepository.countByEventIdAndStatus(eventId, ParticipationStatus.CONFIRMED);

        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new ConflictException("Participation request already exists");
        }

        if (event.initiator().id().equals(userId)) {
            throw new ConflictException("The initiator of the event cannot add a request to participate in their own event");
        }

        if (!"PUBLISHED".equals(event.state())) {
            throw new ConflictException("The event has not been published yet");
        }

        if (event.participantLimit() > 0 && confirmedRequests >= event.participantLimit()) {
            throw new ConflictException("The participant limit for this event has been reached");
        }

        ParticipationRequest request = new ParticipationRequest();
        request.setRequesterId(userId);
        request.setEventId(eventId);

        if (event.participantLimit() == 0 || !event.requestModeration()) {
            request.setStatus(ParticipationStatus.CONFIRMED);
        } else {
            request.setStatus(ParticipationStatus.PENDING);
        }

        ParticipationRequest saved = requestRepository.save(request);
        log.info("Запрос на участие в событии добавлен");

        return requestMapper.mapToRequestDto(saved);
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        checkUserExists(userId);

        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request с id=" + requestId + " was not found"));

        if (!request.getRequesterId().equals(userId)) {
            throw new ValidationException("You can only cancel your own request");
        }

        if (request.getStatus() != ParticipationStatus.PENDING) {
            throw new ConflictException("Only PENDING requests can be cancelled");
        }

        request.setStatus(ParticipationStatus.CANCELED);
        requestRepository.save(request);

        log.info("Заявка на событие отменена");

        return requestMapper.mapToRequestDto(request);
    }

    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        checkUserExists(userId);

        EventInfo event = getEventOrThrow(eventId);

        if (!event.initiator().id().equals(userId)) {
            throw new NotFoundException("Event with id=" + eventId + " not found for user with id=" + userId);
        }

        List<ParticipationRequest> requests = requestRepository.findByEventId(eventId);

        log.info("Получен список заявок на участие в событии с id = {}", eventId);
        return requests.stream()
                .map(requestMapper::mapToRequestDto)
                .toList();
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest requestUpdate) {
        checkUserExists(userId);

        EventInfo event = getEventOrThrow(eventId);

        if (!event.initiator().id().equals(userId)) {
            throw new NotFoundException("Event with id=" + eventId + " not found for user with id=" + userId);
        }

        List<ParticipationRequest> requests = requestRepository.findAllById(requestUpdate.requestIds());

        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();

        long confirmedRequests = requestRepository.countByEventIdAndStatus(eventId, ParticipationStatus.CONFIRMED);

        for (ParticipationRequest request : requests) {
            if (!request.getEventId().equals(eventId)) {
                throw new ValidationException("Request does not belong to this event");
            }
            if (request.getStatus() != ParticipationStatus.PENDING) {
                throw new ConflictException("Request status must be PENDING");
            }

            if (requestUpdate.status() == ParticipationStatus.REJECTED) {
                request.setStatus(ParticipationStatus.REJECTED);
                rejected.add(requestMapper.mapToRequestDto(request));
            } else if (requestUpdate.status() == ParticipationStatus.CONFIRMED) {
                if (event.participantLimit() == 0 || confirmedRequests < event.participantLimit()) {
                    request.setStatus(ParticipationStatus.CONFIRMED);
                    confirmed.add(requestMapper.mapToRequestDto(request));
                    confirmedRequests++;
                } else {
                    request.setStatus(ParticipationStatus.REJECTED);
                    rejected.add(requestMapper.mapToRequestDto(request));
                }
            }
        }

        requestRepository.saveAll(requests);
        log.info("Обновлён статус заявок на участие в событии с id = {}", eventId);

        return new EventRequestStatusUpdateResult(confirmed, rejected);
    }

    @Override
    public Map<Long, Long> getConfirmedRequestsCountMap(List<Long> eventIds) {
        return eventIds.stream()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> requestRepository.countByEventIdAndStatus(id, ParticipationStatus.CONFIRMED)
                ));
    }

    @Override
    public Long getConfirmedRequestsCountForEvent(Long eventId) {
        return requestRepository.countByEventIdAndStatus(eventId, ParticipationStatus.CONFIRMED);
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequests(EventRequestStatusUpdateRequest request) {
        log.info("Updating requests statuses: {}", request);

        List<ParticipationRequest> requests = requestRepository.findAllById(request.requestIds());

        if (requests.size() != request.requestIds().size()) {
            throw new NotFoundException("Some requests not found");
        }

        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();

        for (ParticipationRequest req : requests) {
            if (req.getStatus() != ParticipationStatus.PENDING) {
                throw new ConflictException("Request " + req.getId() + " is not in PENDING status");
            }

            if (request.status() == ParticipationStatus.CONFIRMED) {
                req.setStatus(ParticipationStatus.CONFIRMED);
                confirmed.add(requestMapper.mapToRequestDto(req));
            } else if (request.status() == ParticipationStatus.REJECTED) {
                req.setStatus(ParticipationStatus.REJECTED);
                rejected.add(requestMapper.mapToRequestDto(req));
            }

            requestRepository.save(req);
        }

        log.info("Updated {} confirmed and {} rejected requests", confirmed.size(), rejected.size());
        return new EventRequestStatusUpdateResult(confirmed, rejected);
    }

    @Override
    public List<ParticipationRequestDto> getRequestsByEventId(Long eventId) {
        log.info("Getting all requests for event id={}", eventId);

        List<ParticipationRequest> requests = requestRepository.findByEventId(eventId);

        if (requests.isEmpty()) {
            return List.of();
        }

        return requests.stream()
                .map(requestMapper::mapToRequestDto)
                .collect(Collectors.toList());
    }

    private void checkUserExists(Long userId) {
        try {
            userClient.getUserById(userId);
        } catch (Exception e) {
            throw new NotFoundException("User with id=" + userId + " was not found");
        }
    }

    private EventInfo getEventOrThrow(Long eventId) {
        try {
            EventInfo event = eventClient.getEventByIdForMicroservice(eventId);
            if (event == null) {
                log.error("Event with id {} is null", eventId);
                throw new NotFoundException("Event with id=" + eventId + " was not found");
            }
            return event;
        } catch (Exception e) {
            log.error("Error getting event {}: {}", eventId, e.getMessage(), e);
            throw new NotFoundException("Event with id=" + eventId + " was not found");
        }
    }
}