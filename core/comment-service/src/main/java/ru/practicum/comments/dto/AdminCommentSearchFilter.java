package ru.practicum.comments.dto;

import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import ru.practicum.comments.model.CommentStatus;
import java.time.LocalDateTime;
import java.util.List;

public record AdminCommentSearchFilter(
        String text,
        List<Long> users,
        Long eventId,
        CommentStatus status,

        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime rangeStart,

        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime rangeEnd,

        @Min(0) Integer from,
        @Min(1) Integer size
) {
    public AdminCommentSearchFilter {
        if (from == null) from = 0;
        if (size == null) size = 10;
    }
}