package client;

import io.grpc.ManagedChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.proto.*;

import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
@Service
public class AnalyzerClient {

    private static final String SERVICE_ID = "analyzer-service";

    private final GrpcChannelProvider channelProvider;

    public AnalyzerClient(GrpcChannelProvider channelProvider) {
        this.channelProvider = channelProvider;
    }

    /**
     * Получить список рекомендованных мероприятий для пользователя
     */
    public List<RecommendedEventProto> getRecommendationsForUser(long userId, int maxResults) {
        try {
            UserPredictionsRequestProto request = UserPredictionsRequestProto.newBuilder()
                    .setUserId(userId)
                    .setMaxResults(maxResults)
                    .build();
            return toList(stub().getRecommendationsForUser(request));
        } catch (Exception e) {
            log.error("Failed to get recommendations for user {}: {}", userId, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Получить список мероприятий, похожих на указанное, с которыми пользователь ещё не взаимодействовал
     */
    public List<RecommendedEventProto> getSimilarEvents(long eventId, long userId, int maxResults) {
        try {
            SimilarEventsRequestProto request = SimilarEventsRequestProto.newBuilder()
                    .setEventId(eventId)
                    .setUserId(userId)
                    .setMaxResults(maxResults)
                    .build();
            return toList(stub().getSimilarEvents(request));
        } catch (Exception e) {
            log.error("Failed to get similar events for event {}: {}", eventId, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Получить сумму максимальных весов действий пользователей для каждого мероприятия
     * (используется для получения rating мероприятия)
     */
    public List<RecommendedEventProto> getInteractionsCount(List<Long> eventIds) {
        try {
            InteractionsCountRequestProto request = InteractionsCountRequestProto.newBuilder()
                    .addAllEventId(eventIds)
                    .build();
            return toList(stub().getInteractionsCount(request));
        } catch (Exception e) {
            log.error("Failed to get interactions count: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Получить рейтинг одного мероприятия
     */
    public double getEventRating(long eventId) {
        List<RecommendedEventProto> result = getInteractionsCount(List.of(eventId));
        return result.isEmpty() ? 0.0 : result.get(0).getScore();
    }

    /**
     * Получить рейтинг нескольких мероприятий
     */
    public java.util.Map<Long, Double> getEventsRating(List<Long> eventIds) {
        List<RecommendedEventProto> result = getInteractionsCount(eventIds);
        return result.stream()
                .collect(java.util.stream.Collectors.toMap(
                        RecommendedEventProto::getEventId,
                        RecommendedEventProto::getScore
                ));
    }

    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub stub() {
        ManagedChannel channel = channelProvider.getChannel(SERVICE_ID);
        return RecommendationsControllerGrpc.newBlockingStub(channel);
    }

    private List<RecommendedEventProto> toList(Iterator<RecommendedEventProto> iterator) {
        Stream<RecommendedEventProto> stream = StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
        return stream.collect(Collectors.toList());
    }
}