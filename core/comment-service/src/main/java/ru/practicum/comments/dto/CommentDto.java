package ru.practicum.comments.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import ru.practicum.comments.model.CommentStatus;
import java.time.LocalDateTime;

public record CommentDto(
        Long id,
        String comment,
        CommentStatus status,
        Long eventId,
        Long authorId,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createdOn,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime editedOn
) {
}