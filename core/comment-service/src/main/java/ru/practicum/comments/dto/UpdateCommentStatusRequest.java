package ru.practicum.comments.dto;

import jakarta.validation.constraints.NotNull;
import ru.practicum.comments.model.CommentStatus;

public record UpdateCommentStatusRequest(
        @NotNull
        CommentStatus status
) {
}