package ru.practicum.event.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public record NewEventDto(
        @NotBlank
        @Size(min = 20, max = 2000)
        String annotation,

        @NotNull
        Long category,

        @NotBlank
        @Size(min = 20, max = 7000)
        String description,

        @NotNull
        @FutureOrPresent
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime eventDate,

        @NotNull
        LocationDto location,

        Boolean paid,

        @PositiveOrZero
        Integer participantLimit,

        Boolean requestModeration,

        @NotBlank
        @Size(min = 3, max = 120)
        String title
) {
        public NewEventDto {
                if (paid == null) paid = false;
                if (participantLimit == null) participantLimit = 0;
                if (requestModeration == null) requestModeration = true;
        }
}