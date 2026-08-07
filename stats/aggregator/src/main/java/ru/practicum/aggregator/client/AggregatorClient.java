package ru.practicum.aggregator.client;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.practicum.aggregator.serializer.EventSimilarityAvroSerializer;
import ru.practicum.aggregator.serializer.UserActionAvroDeserializer;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import jakarta.annotation.PreDestroy;
import java.util.Properties;

@Slf4j
@Component
public class AggregatorClient {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:aggregator-service}")
    private String groupId;

    private Consumer<String, UserActionAvro> consumer;
    private Producer<String, SpecificRecordBase> producer;

    public Consumer<String, UserActionAvro> getConsumer() {
        if (consumer == null) {
            consumer = createConsumer();
        }
        return consumer;
    }

    public Producer<String, SpecificRecordBase> getProducer() {
        if (producer == null) {
            producer = createProducer();
        }
        return producer;
    }

    private Consumer<String, UserActionAvro> createConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, UserActionAvroDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        return new org.apache.kafka.clients.consumer.KafkaConsumer<>(props);
    }

    private Producer<String, SpecificRecordBase> createProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, EventSimilarityAvroSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);

        return new org.apache.kafka.clients.producer.KafkaProducer<>(props);
    }

    @PreDestroy
    public void destroy() {
        if (consumer != null) {
            consumer.close();
        }
        if (producer != null) {
            producer.close();
        }
    }
}