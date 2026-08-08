package ru.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.analyzer.model.EventSimilarity;
import ru.practicum.analyzer.model.UserAction;
import ru.practicum.analyzer.repository.EventSimilarityRepository;
import ru.practicum.analyzer.repository.UserActionRepository;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final UserActionRepository userActionRepository;
    private final EventSimilarityRepository eventSimilarityRepository;

    private static final double VIEW_WEIGHT = 0.4;
    private static final double REGISTER_WEIGHT = 0.8;
    private static final double LIKE_WEIGHT = 1.0;

    @Transactional(readOnly = true)
    public List<RecommendedEventProto> getRecommendationsForUser(long userId, int maxResults) {
        log.info("Getting recommendations for user: {}, maxResults: {}", userId, maxResults);

        List<UserAction> userActions = userActionRepository
                .findByUserIdOrderByTimestampDesc(userId)
                .stream()
                .limit(20)
                .toList();

        if (userActions.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> userEventIds = userActions.stream()
                .map(UserAction::getEventId)
                .collect(Collectors.toSet());

        List<Long> eventIds = userActions.stream()
                .map(UserAction::getEventId)
                .distinct()
                .toList();

        List<EventSimilarity> allSimilarities = eventSimilarityRepository
                .findByEventAInOrEventBIn(eventIds, eventIds);

        Map<Long, List<EventSimilarity>> similarityMap = new HashMap<>();
        for (EventSimilarity sim : allSimilarities) {
            similarityMap.computeIfAbsent(sim.getEventA(), k -> new ArrayList<>()).add(sim);
            similarityMap.computeIfAbsent(sim.getEventB(), k -> new ArrayList<>()).add(sim);
        }

        Map<Long, Double> similarityScores = new HashMap<>();

        for (UserAction action : userActions) {
            List<EventSimilarity> similarities = similarityMap.getOrDefault(action.getEventId(), Collections.emptyList());

            for (EventSimilarity sim : similarities) {
                Long similarEventId = sim.getEventA().equals(action.getEventId())
                        ? sim.getEventB() : sim.getEventA();

                if (!userEventIds.contains(similarEventId)) {
                    similarityScores.merge(similarEventId, sim.getScore(), Double::sum);
                }
            }
        }

        return similarityScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(maxResults)
                .map(entry -> RecommendedEventProto.newBuilder()
                        .setEventId(entry.getKey())
                        .setScore(entry.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RecommendedEventProto> getSimilarEvents(long eventId, long userId, int maxResults) {
        log.info("Getting similar events: eventId={}, userId={}, maxResults={}",
                eventId, userId, maxResults);

        List<EventSimilarity> similarities = eventSimilarityRepository.findSimilarEvents(eventId);

        if (similarities.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> userEventIds = userActionRepository
                .findByUserId(userId)
                .stream()
                .map(UserAction::getEventId)
                .collect(Collectors.toSet());

        return similarities.stream()
                .map(sim -> {
                    Long similarEventId = sim.getEventA() == eventId
                            ? sim.getEventB() : sim.getEventA();
                    return Map.entry(similarEventId, sim.getScore());
                })
                .filter(entry -> !userEventIds.contains(entry.getKey()))
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(maxResults)
                .map(entry -> RecommendedEventProto.newBuilder()
                        .setEventId(entry.getKey())
                        .setScore(entry.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RecommendedEventProto> getInteractionsCount(List<Long> eventIds) {
        log.info("Getting interactions count for {} events", eventIds.size());

        List<UserAction> allActions = userActionRepository.findByEventIdIn(eventIds);

        Map<Long, List<UserAction>> actionsByEvent = allActions.stream()
                .collect(Collectors.groupingBy(UserAction::getEventId));

        Map<Long, Double> result = new HashMap<>();

        for (Long eventId : eventIds) {
            List<UserAction> actions = actionsByEvent.getOrDefault(eventId, Collections.emptyList());

            Map<Long, Double> userMaxWeights = new HashMap<>();
            for (UserAction action : actions) {
                double weight = getWeight(action.getActionType());
                userMaxWeights.merge(action.getUserId(), weight, Double::max);
            }

            double totalWeight = userMaxWeights.values().stream()
                    .mapToDouble(Double::doubleValue)
                    .sum();

            result.put(eventId, totalWeight);
        }

        return result.entrySet().stream()
                .map(entry -> RecommendedEventProto.newBuilder()
                        .setEventId(entry.getKey())
                        .setScore(entry.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    private double getWeight(String actionType) {
        return switch (actionType) {
            case "VIEW" -> VIEW_WEIGHT;
            case "REGISTER" -> REGISTER_WEIGHT;
            case "LIKE" -> LIKE_WEIGHT;
            default -> VIEW_WEIGHT;
        };
    }
}