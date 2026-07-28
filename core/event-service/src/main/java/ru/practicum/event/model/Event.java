package ru.practicum.event.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "events")
@Getter
@Setter
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2000)
    private String annotation;

    @Column(nullable = false, length = 7000)
    private String description;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @Column(name = "created_on", nullable = false)
    private LocalDateTime createdOn;

    @Column(name = "published_on")
    private LocalDateTime publishedOn;

    @Column(nullable = false)
    private Boolean paid;

    @Column(name = "participant_limit", nullable = false)
    private Integer participantLimit;

    @Column(name = "request_moderation", nullable = false)
    private Boolean requestModeration;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventState state;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "initiator_id", nullable = false)
    private Long initiatorId;

    @Embedded
    private Location location;

    @PrePersist
    public void prePersist() {
        if (createdOn == null) createdOn = LocalDateTime.now();
        if (eventDate == null) eventDate = LocalDateTime.now().plusDays(1);
        if (paid == null) paid = false;
        if (participantLimit == null) participantLimit = 0;
        if (requestModeration == null) requestModeration = true;
        if (state == null) state = EventState.PENDING;
    }
}