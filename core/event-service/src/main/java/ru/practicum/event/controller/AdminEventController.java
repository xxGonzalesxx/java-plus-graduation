package ru.practicum.event.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.dto.AdminEventSearchFilter;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.UpdateEventAdminRequest;
import ru.practicum.event.service.EventService;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin/events")
public class AdminEventController {
    private final EventService adminEventService;

    @GetMapping
    public List<EventFullDto> getEvents(@Valid AdminEventSearchFilter filter) {
        log.info("GET /admin/events: filter={}", filter);
        return adminEventService.searchEventsAdmin(filter);
    }

    @PatchMapping("/{eventId}")
    public EventFullDto updateEvent(@PathVariable Long eventId,
                                    @Valid @RequestBody UpdateEventAdminRequest updateRequest) {
        log.info("PATCH /admin/events/{}: {}", eventId, updateRequest);
        return adminEventService.updateEventAdmin(eventId, updateRequest);
    }
}