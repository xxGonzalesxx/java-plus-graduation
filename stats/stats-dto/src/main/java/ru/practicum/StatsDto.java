package ru.practicum;

public record StatsDto(
        String app,
        String uri,
        Long hits) {
}
