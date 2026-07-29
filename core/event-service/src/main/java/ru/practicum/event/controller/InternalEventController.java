// event-service/src/main/java/ru/practicum/event/controller/InternalEventController.java
package ru.practicum.event.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.event.repository.EventRepository;

@Slf4j
@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
public class InternalEventController {

    private final EventRepository eventRepository;

    @GetMapping("/exists-by-category")
    public boolean existsByCategoryId(@RequestParam Long categoryId) {
        log.info("Checking if category {} has events", categoryId);
        return eventRepository.existsByCategoryId(categoryId);
    }
}