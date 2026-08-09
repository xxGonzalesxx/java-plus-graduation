package ru.practicum.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.analyzer.model.EventSimilarity;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventSimilarityRepository extends JpaRepository<EventSimilarity, Long> {

    @Query("SELECT es FROM EventSimilarity es WHERE es.eventA = :eventId OR es.eventB = :eventId")
    List<EventSimilarity> findSimilarEvents(@Param("eventId") Long eventId);

    Optional<EventSimilarity> findByEventAAndEventB(Long eventA, Long eventB);

    List<EventSimilarity> findByEventAInOrEventBIn(List<Long> eventAIds, List<Long> eventBIds);
}