package ru.practicum.event.dto;

public record EventInfoDto(
        Long id,
        Initiator initiator,
        String state,
        Integer participantLimit,
        Boolean requestModeration
) {
    public record Initiator(Long id) {
    }
}