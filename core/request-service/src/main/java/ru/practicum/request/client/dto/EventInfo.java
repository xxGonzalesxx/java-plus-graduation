package ru.practicum.request.client.dto;

import ru.practicum.model.EventState;

public record EventInfo(
        Long id,
        Initiator initiator,
        EventState state,
        Integer participantLimit,
        Boolean requestModeration
) {
    public record Initiator(Long id) {
    }
}