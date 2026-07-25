package ru.practicum.comments.dto;

public record PostCommentParam(
        Long authorId,
        Long eventId,
        String comment
) {
}