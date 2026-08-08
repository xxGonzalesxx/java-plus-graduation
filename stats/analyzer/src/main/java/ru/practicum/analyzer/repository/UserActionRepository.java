package ru.practicum.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.practicum.analyzer.model.UserAction;

import java.util.List;

@Repository
public interface UserActionRepository extends JpaRepository<UserAction, Long> {

    List<UserAction> findByUserId(Long userId);

    List<UserAction> findByUserIdOrderByTimestampDesc(Long userId);

    List<UserAction> findByEventId(Long eventId);

    List<UserAction> findByEventIdIn(List<Long> eventIds);
}