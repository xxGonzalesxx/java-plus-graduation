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
import ru.practicum.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.model.ParticipationRequest;
import ru.practicum.request.model.ParticipationStatus;
import ru.practicum.request.repository.ParticipationRequestRepository;
import ru.practicum.request.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request.mapper.ParticipationRequestMapper;

import java.util.ArrayList;
import java.util.List;

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

        if (event.participantLimit() != 0 && event.participantLimit() <= confirmedRequests) {
            throw new ConflictException("The participant limit for this event has been reached");
        }

        ParticipationRequest request = new ParticipationRequest();
        request.setRequesterId(userId);
        request.setEventId(eventId);

        if (!event.requestModeration() || event.participantLimit() == 0) {
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

            if ("REJECTED".equals(requestUpdate.status())) {
                request.setStatus(ParticipationStatus.REJECTED);
                rejected.add(requestMapper.mapToRequestDto(request));
            } else if ("CONFIRMED".equals(requestUpdate.status())) {
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

    private void checkUserExists(Long userId) {
        try {
            userClient.getUserById(userId);
        } catch (Exception e) {
            throw new NotFoundException("User with id=" + userId + " was not found");
        }
    }

    private EventInfo getEventOrThrow(Long eventId) {
        try {
            return eventClient.getEventById(eventId);
        } catch (Exception e) {
            throw new NotFoundException("Event with id=" + eventId + " was not found");
        }
    }
}