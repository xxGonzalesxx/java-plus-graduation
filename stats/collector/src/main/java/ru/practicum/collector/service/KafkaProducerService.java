package ru.practicum.collector.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<Long, UserActionAvro> kafkaTemplate;

    @Value("${app.kafka.topics.user-actions:stats.user-actions.v1}")
    private String userActionsTopic;

    public void send(UserActionAvro message) {
        kafkaTemplate.send(userActionsTopic, message.getUserId(), message);  // ключ — Long
        log.info("Sent message to topic {}: userId={}, eventId={}",
                userActionsTopic, message.getUserId(), message.getEventId());
    }
}