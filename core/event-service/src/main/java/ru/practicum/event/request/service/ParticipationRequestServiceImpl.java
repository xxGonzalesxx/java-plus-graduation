package ru.practicum.event.request.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.event.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.event.request.dto.ParticipationRequestDto;
import ru.practicum.event.request.mapper.ParticipationRequestMapper;
import ru.practicum.event.request.model.ParticipationRequest;
import ru.practicum.event.request.model.ParticipationStatus;
import ru.practicum.event.request.repository.ParticipationRequestRepository;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.user.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ParticipationRequestServiceImpl implements ParticipationRequestService {
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final ParticipationRequestRepository requestRepository;
    private final ParticipationRequestMapper requestMapper;

    @Override
    public List<ParticipationRequestDto> getRequestByUserId(Long userId) {
        User requester = findUserById(userId);

        List<ParticipationRequest> requests = requestRepository.findByRequester(requester);

        log.info("Получен список заявок на участия в событиях пользователя с id = {}", userId);
        return requests.stream()
                .map(requestMapper::mapToRequestDto)
                .toList();
    }

    @Override
    @Transactional
    public ParticipationRequestDto addRequest(Long userId, Long eventId) {
        User requester = findUserById(userId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        ParticipationRequest request = new ParticipationRequest();
        Long confirmedRequests = requestRepository.countByEventAndStatus(event, ParticipationStatus.CONFIRMED);

        if (requestRepository.existsByRequesterAndEvent(requester, event)) {
            throw new ConflictException("Participation request already exists");
        }

        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("The initiator of the event cannot add a request to participate in their own event");
        }

        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("The event has not been published yet");
        }

        if (event.getParticipantLimit() != 0 && event.getParticipantLimit() <= confirmedRequests) {
            throw new ConflictException("The participant limit for this event has been reached");
        }

        if (event.getRequestModeration() == false || event.getParticipantLimit() == 0) {
            request.setStatus(ParticipationStatus.CONFIRMED);
        } else {
            request.setStatus(ParticipationStatus.PENDING);
        }

        request.setRequester(requester);
        request.setEvent(event);

        ParticipationRequest saveRequest = requestRepository.save(request);

        log.info("Запрос на участие в событии добавлен");

        return requestMapper.mapToRequestDto(saveRequest);
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        findUserById(userId);

        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request с id=" + requestId + " was not found"));

        if (!request.getRequester().getId().equals(userId)) {
            throw new ValidationException("You can only cancel your own request");
        }

        request.setStatus(ParticipationStatus.CANCELED);
        requestRepository.save(request);

        log.info("Заявка на событие отменена");

        return requestMapper.mapToRequestDto(request);
    }

    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        // Проверяем, что пользователь существует
        findUserById(userId);

        // Проверяем, что событие существует и принадлежит пользователю
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Event with id=" + eventId + " not found for user with id=" + userId);
        }

        List<ParticipationRequest> requests = requestRepository.findByEvent(event);

        log.info("Получен список заявок на участие в событии с id = {}", eventId);
        return requests.stream()
                .map(requestMapper::mapToRequestDto)
                .toList();
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId, EventRequestStatusUpdateRequest requestUpdate) {
        // Проверяем пользователя и событие
        findUserById(userId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Event with id=" + eventId + " not found for user with id=" + userId);
        }

        // Получаем список заявок
        List<ParticipationRequest> requests = requestRepository.findAllById(requestUpdate.requestIds());

        // Создаём списки для результатов
        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();

        // Текущее количество подтверждённых заявок
        long confirmedRequests = requestRepository.countByEventAndStatus(event, ParticipationStatus.CONFIRMED);

        if (confirmedRequests >= event.getParticipantLimit()) {
            throw new ConflictException("The participant limit for this event has been reached");
        }

        for (ParticipationRequest request : requests) {
            if (!request.getEvent().getId().equals(eventId)) {
                throw new ValidationException("Request does not belong to this event");
            }
            if (request.getStatus() != ParticipationStatus.PENDING) {
                throw new ConflictException("Request status must be PENDING");
            }

            if ("REJECTED".equals(requestUpdate.status())) {
                request.setStatus(ParticipationStatus.REJECTED);
                rejected.add(requestMapper.mapToRequestDto(request));
            } else if ("CONFIRMED".equals(requestUpdate.status())) {
                if (event.getParticipantLimit() == 0 || confirmedRequests < event.getParticipantLimit()) {
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

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
    }
}
