package ru.practicum.aggregator.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.aggregator.model.EventPair;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class AggregatorService {

    private final ConcurrentMap<Long, ConcurrentMap<Long, Double>> eventUserWeights = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, Double> sumSquares = new ConcurrentHashMap<>();
    private final ConcurrentMap<EventPair, Double> dotProducts = new ConcurrentHashMap<>();

    private final KafkaTemplate<Long, EventSimilarityAvro> kafkaTemplate;  // ← Long!
    private final String outTopic;

    private static final double VIEW_WEIGHT = 0.4;
    private static final double REGISTER_WEIGHT = 0.8;
    private static final double LIKE_WEIGHT = 1.0;

    public AggregatorService(KafkaTemplate<Long, EventSimilarityAvro> kafkaTemplate,
                             @Value("${app.kafka.topics.events-similarity}") String outTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.outTopic = outTopic;
    }

    @KafkaListener(topics = "${app.kafka.topics.user-actions}", containerFactory = "kafkaListenerContainerFactory")
    public void onUserAction(UserActionAvro msg) {
        if (msg == null) return;
        long eventId = msg.getEventId();
        long userId = msg.getUserId();
        double weight = weightFor(msg.getActionType());

        ConcurrentMap<Long, Double> userMap = eventUserWeights.computeIfAbsent(eventId, k -> new ConcurrentHashMap<>());

        Double old = userMap.put(userId, weight);
        double oldVal = old == null ? 0.0 : old;
        double delta = weight - oldVal;
        if (Math.abs(delta) < 1e-9) {
            return;
        }

        double prevSumSq = sumSquares.getOrDefault(eventId, 0.0);
        double newSumSq = prevSumSq + weight * weight - oldVal * oldVal;
        sumSquares.put(eventId, newSumSq);

        Set<Long> otherEvents = eventUserWeights.keySet();
        for (Long otherEventId : otherEvents) {
            if (otherEventId == eventId) continue;
            ConcurrentMap<Long, Double> otherUserMap = eventUserWeights.get(otherEventId);
            if (otherUserMap == null) continue;
            Double otherWeight = otherUserMap.get(userId);
            if (otherWeight == null) continue;

            EventPair pair = new EventPair(eventId, otherEventId);
            double deltaDot = delta * otherWeight;
            dotProducts.merge(pair, deltaDot, Double::sum);

            double dot = dotProducts.getOrDefault(pair, 0.0);
            double sqA = sumSquares.getOrDefault(pair.getA(), 0.0);
            double sqB = sumSquares.getOrDefault(pair.getB(), 0.0);
            double denom = Math.sqrt(sqA) * Math.sqrt(sqB);
            double score = denom == 0.0 ? 0.0 : dot / denom;

            EventSimilarityAvro similarity = EventSimilarityAvro.newBuilder()
                    .setEventA(pair.getA())
                    .setEventB(pair.getB())
                    .setScore(score)
                    .setTimestamp(Instant.ofEpochMilli(System.currentTimeMillis()))
                    .build();

            kafkaTemplate.send(outTopic, pair.getA(), similarity);
        }
    }

    private double weightFor(ActionTypeAvro type) {
        if (type == null) return VIEW_WEIGHT;
        return switch (type) {
            case VIEW -> VIEW_WEIGHT;
            case REGISTER -> REGISTER_WEIGHT;
            case LIKE -> LIKE_WEIGHT;
            default -> VIEW_WEIGHT;
        };
    }
}