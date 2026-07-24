package ru.practicum.event.request.repository;

import org.apache.catalina.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.event.model.Event;
import ru.practicum.event.request.model.ConfirmedRequestCount;
import ru.practicum.event.request.model.ParticipationRequest;
import ru.practicum.event.request.model.ParticipationStatus;

import java.util.List;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {
    // Список своих заявок на участия в событиях
    List<ParticipationRequest> findByRequester(User requester);

    // Проверяем наличие такого запроса
    boolean existsByRequesterAndEvent(User requester, Event event);

    // Количество заявок на событие
    Long countByEventAndStatus(Event event, ParticipationStatus status);

    // Количество заявок для событий
    @Query("""
            SELECT new ewm.request.model.ConfirmedRequestCount(r.event.id, COUNT(r.id))
            FROM ParticipationRequest AS r
            WHERE r.event.id IN :eventIds AND r.status = 'CONFIRMED'
            GROUP BY r.event.id
            """)
    List<ConfirmedRequestCount> findAllConfirmedRequests(List<Long> eventIds);

    List<ParticipationRequest> findByEvent(Event event);
}
