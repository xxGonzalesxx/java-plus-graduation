package ru.practicum.event.model;

import jakarta.persistence.*;
import ru.practicum.category.model.Category;
import lombok.Data;
import org.apache.catalina.User;
import ru.practicum.event.compilation.model.Compilation;
import java.time.LocalDateTime;
import java.util.List;


@Entity
@Table(name = "events")
@Data
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String annotation;
    private String description;

    @Column(name = "event_date")
    private LocalDateTime eventDate;

    @Column(name = "created_on")
    private LocalDateTime createdOn;

    @Column(name = "published_on")
    private LocalDateTime publishedOn;

    private Boolean paid;
    private Integer participantLimit;
    private Boolean requestModeration;

    @Enumerated(EnumType.STRING)
    @Column(name = "state")
    private EventState state;
    private String title;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne
    @JoinColumn(name = "initiator_id")
    private User initiator;

    @Embedded
    private Location location;

    @ManyToMany(mappedBy = "events")
    private List<Compilation> compilations;
}