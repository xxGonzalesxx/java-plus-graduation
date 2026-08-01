package ru.practicum.event.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventInfoDto;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.event.service.EventService;

@Slf4j
@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
public class InternalEventController {

    private final EventRepository eventRepository;
    private final EventService eventService;

    @GetMapping("/exists-by-category")
    public boolean existsByCategoryId(@RequestParam Long categoryId) {
        log.info("Checking if category {} has events", categoryId);
        return eventRepository.existsByCategoryId(categoryId);
    }

    @GetMapping("/{eventId}")
    public EventFullDto getEventByIdForMicroservice(@PathVariable Long eventId) {
        log.info("Internal API: get event by id = {}", eventId);
        return eventService.getEventByIdForMicroservice(eventId);
    }

    @GetMapping("/info/{eventId}")
    public EventInfoDto getEventInfoById(@PathVariable Long eventId) {
        log.info("Internal API: get event info by id = {}", eventId);
        return eventService.getEventInfoById(eventId);
    }
}