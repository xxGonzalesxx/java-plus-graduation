package ru.practicum.comments.dto;

public record PostCommentParam(
        Long author,
        Long event,
        String comment
) {
}
