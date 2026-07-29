package ru.practicum.category.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "event-service")
public interface EventClient {

    @GetMapping("/internal/events/exists-by-category")
    boolean existsByCategoryId(@RequestParam("categoryId") Long categoryId);
}