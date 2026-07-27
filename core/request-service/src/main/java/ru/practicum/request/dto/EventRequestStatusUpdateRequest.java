package ru.practicum.request.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import ru.practicum.request.model.ParticipationStatus;

import java.util.List;

public record EventRequestStatusUpdateRequest(
        @NotEmpty(message = "Request IDs cannot be empty")
        List<Long> requestIds,

        @NotNull(message = "Status cannot be null")
        ParticipationStatus status
) {}