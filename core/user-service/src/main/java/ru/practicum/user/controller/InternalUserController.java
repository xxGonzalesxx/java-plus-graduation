package ru.practicum.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.UserShortDto;
import ru.practicum.user.service.UserService;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {
    private final UserService userService;

    @GetMapping("/{id}")
    public UserShortDto getUserShort(@PathVariable Long id) {
        var user = userService.findById(id);
        return new UserShortDto(user.getId(), user.getName());
    }
}