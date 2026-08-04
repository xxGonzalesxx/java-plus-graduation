package ru.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.analyzer.model.EventSimilarity;
import ru.practicum.analyzer.model.UserAction;
import ru.practicum.analyzer.repository.EventSimilarityRepository;
import ru.practicum.analyzer.repository.UserActionRepository;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final UserActionRepository userActionRepository;
    private final EventSimilarityRepository eventSimilarityRepository;

    private static final double VIEW_WEIGHT = 0.4;
    private static final double REGISTER_WEIGHT = 0.8;
    private static final double LIKE_WEIGHT = 1.0;

    @KafkaListener(
            topics = "${app.kafka.topics.user-actions}",
            containerFactory = "userActionKafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeUserAction(UserActionAvro message) {
        log.info("Received user action: userId={}, eventId={}, actionType={}",
                message.getUserId(), message.getEventId(), message.getActionType());

        try {
            UserAction userAction = UserAction.builder()
                    .userId(message.getUserId())
                    .eventId(message.getEventId())
                    .actionType(message.getActionType().toString())
                    .weight(getWeight(message.getActionType()))
                    .timestamp(message.getTimestamp())
                    .build();

            userActionRepository.save(userAction);
            log.info("User action saved to database");

        } catch (Exception e) {
            log.error("Error saving user action: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(
            topics = "${app.kafka.topics.events-similarity}",
            containerFactory = "eventSimilarityKafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeEventSimilarity(EventSimilarityAvro message) {
        log.info("Received event similarity: eventA={}, eventB={}, score={}",
                message.getEventA(), message.getEventB(), message.getScore());

        try {
            EventSimilarity similarity = eventSimilarityRepository
                    .findByEventAAndEventB(message.getEventA(), message.getEventB())
                    .orElse(new EventSimilarity());

            similarity.setEventA(message.getEventA());
            similarity.setEventB(message.getEventB());
            similarity.setScore(message.getScore());
            similarity.setUpdatedAt(message.getTimestamp());

            eventSimilarityRepository.save(similarity);
            log.info("Event similarity saved to database");

        } catch (Exception e) {
            log.error("Error saving event similarity: {}", e.getMessage(), e);
        }
    }

    private double getWeight(ru.practicum.ewm.stats.avro.ActionTypeAvro actionType) {
        if (actionType == null) return VIEW_WEIGHT;
        return switch (actionType) {
            case VIEW -> VIEW_WEIGHT;
            case REGISTER -> REGISTER_WEIGHT;
            case LIKE -> LIKE_WEIGHT;
        };
    }
}