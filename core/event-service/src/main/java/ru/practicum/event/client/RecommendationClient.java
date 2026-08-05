package ru.practicum.event.client;

import client.AnalyzerClient;
import client.CollectorClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;

import java.util.List;
import java.util.Map;

@Slf4j
@Component("eventRecommendationClient")
@RequiredArgsConstructor
public class RecommendationClient {

    private final CollectorClient collectorClient;
    private final AnalyzerClient analyzerClient;

    public void sendView(long userId, long eventId) {
        collectorClient.sendView(userId, eventId);
        log.debug("View sent to Collector: userId={}, eventId={}", userId, eventId);
    }

    public void sendLike(long userId, long eventId) {
        collectorClient.sendLike(userId, eventId);
        log.debug("Like sent to Collector: userId={}, eventId={}", userId, eventId);
    }

    public double getEventRating(long eventId) {
        return analyzerClient.getEventRating(eventId);
    }

    // НОВОЕ: батч-рейтинг для списков событий
    public Map<Long, Double> getEventsRating(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }
        return analyzerClient.getEventsRating(eventIds);
    }

    public List<RecommendedEventProto> getRecommendations(long userId, int limit) {
        return analyzerClient.getRecommendationsForUser(userId, limit);
    }
}