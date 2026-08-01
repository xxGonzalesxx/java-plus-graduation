package ru.practicum.event.dto;

import jakarta.validation.constraints.NotNull;

public record LocationDto(
        @NotNull Float lat,
        @NotNull Float lon
) {}