package ru.practicum.request.client.dto;

public record EventInfo(
        Long id,
        Initiator initiator,
        String state,
        Integer participantLimit,
        Boolean requestModeration
) {
    public record Initiator(Long id) {
    }
}