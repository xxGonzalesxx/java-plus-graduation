package ru.practicum.aggregator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.practicum.aggregator.client.AggregatorClient;
import ru.practicum.aggregator.dto.Topics;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregationStarter {
    private final AggregatorClient client;

    // используем Long вместо Integer
    private final Map<Long, Map<Long, Double>> eventUserWeights = new ConcurrentHashMap<>();
    private final Map<Long, Double> eventWeightsSum = new ConcurrentHashMap<>();
    private final Map<Long, Map<Long, Double>> minWeightsSums = new ConcurrentHashMap<>();

    public void start() {
        Consumer<String, UserActionAvro> consumer = client.getConsumer();
        Producer<String, SpecificRecordBase> producer = client.getProducer();

        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));

        try {
            log.info("Aggregator: подписываемся на топик: {}", Topics.STATS_USER_ACTIONS_V1);
            consumer.subscribe(List.of(Topics.STATS_USER_ACTIONS_V1));

            while (!Thread.currentThread().isInterrupted()) {
                ConsumerRecords<String, UserActionAvro> records = consumer.poll(Duration.ofMillis(1000));

                for (ConsumerRecord<String, UserActionAvro> record : records) {
                    UserActionAvro userAvro = record.value();

                    log.info("Aggregator: получено событие из Kafka: User={}, Event={}, Type={}",
                            userAvro.getUserId(), userAvro.getEventId(), userAvro.getActionType());

                    List<EventSimilarityAvro> similarities = calculateEventSimilarity(userAvro);

                    for (EventSimilarityAvro similarity : similarities) {
                        ProducerRecord<String, SpecificRecordBase> producerRecord = new ProducerRecord<>(
                                Topics.STATS_EVENTS_SIMILARITY_V1,
                                similarity.getEventA() + "_" + similarity.getEventB(),
                                similarity
                        );

                        producer.send(producerRecord);
                        log.info("Aggregator: Отправлено сходство: {}-{}, score={}",
                                similarity.getEventA(), similarity.getEventB(), similarity.getScore());
                    }
                }

                consumer.commitSync();
            }
        } catch (WakeupException ignored) {
        } catch (Exception e) {
            log.error("Ошибка во время обработки событий", e);
        } finally {
            try {
                log.info("Сброс данных в буфере у продюсера");
                producer.flush();
                log.info("Фиксация смещений консьюмера");
                consumer.commitSync();
            } finally {
                log.info("Закрываем консьюмер");
                consumer.close();
                log.info("Закрываем продюсер");
                producer.close();
            }
        }
    }

    private List<EventSimilarityAvro> calculateEventSimilarity(UserActionAvro userAvro) {
        long userId = userAvro.getUserId();   // ← Long
        long eventA = userAvro.getEventId();  // ← Long

        List<EventSimilarityAvro> results = new ArrayList<>();

        double actionWeight = switch (userAvro.getActionType()) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
        };

        eventUserWeights.putIfAbsent(eventA, new ConcurrentHashMap<>());
        Map<Long, Double> userWeightsForEventA = eventUserWeights.get(eventA);

        double oldWeight = userWeightsForEventA.getOrDefault(userId, 0.0);

        if (actionWeight <= oldWeight) {
            return results;
        }

        userWeightsForEventA.put(userId, actionWeight);

        double deltaWeight = actionWeight - oldWeight;
        eventWeightsSum.merge(eventA, deltaWeight, Double::sum);

        for (Map.Entry<Long, Map<Long, Double>> entry : eventUserWeights.entrySet()) {
            long eventB = entry.getKey();

            if (eventA == eventB) {
                continue;
            }

            Map<Long, Double> userWeightsForEventB = entry.getValue();

            if (userWeightsForEventB.containsKey(userId)) {
                double weightInB = userWeightsForEventB.get(userId);

                double oldMinContribution = Math.min(oldWeight, weightInB);
                double newMinContribution = Math.min(actionWeight, weightInB);
                double deltaSMin = newMinContribution - oldMinContribution;

                long first = Math.min(eventA, eventB);
                long second = Math.max(eventA, eventB);

                minWeightsSums.putIfAbsent(first, new ConcurrentHashMap<>());
                minWeightsSums.get(first).merge(second, deltaSMin, Double::sum);

                double sMin = minWeightsSums.get(first).get(second);
                double sumA = eventWeightsSum.getOrDefault(eventA, 0.0);
                double sumB = eventWeightsSum.getOrDefault(eventB, 0.0);

                double score = 0.0;
                if (sumA > 0 && sumB > 0) {
                    score = sMin / (Math.sqrt(sumA) * Math.sqrt(sumB));
                }

                results.add(EventSimilarityAvro.newBuilder()
                        .setEventA(first)
                        .setEventB(second)
                        .setScore(score)
                        .setTimestamp(userAvro.getTimestamp())
                        .build());
            }
        }

        return results;
    }
}