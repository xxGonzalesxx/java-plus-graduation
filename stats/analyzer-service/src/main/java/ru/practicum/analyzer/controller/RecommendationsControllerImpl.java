package ru.practicum.analyzer.controller;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.service.GrpcService;
import ru.practicum.analyzer.service.RecommendationService;
import ru.practicum.ewm.stats.proto.*;

import java.util.List;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class RecommendationsControllerImpl
        extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {

    private final RecommendationService recommendationService;

    @Override
    public void getRecommendationsForUser(
            UserPredictionsRequestProto request,
            StreamObserver<RecommendedEventProto> responseObserver) {

        log.info("Getting recommendations for user: {}", request.getUserId());

        try {
            List<RecommendedEventProto> recommendations = recommendationService
                    .getRecommendationsForUser(request.getUserId(), request.getMaxResults());

            for (RecommendedEventProto rec : recommendations) {
                responseObserver.onNext(rec);
            }
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error getting recommendations", e);
            responseObserver.onError(
                    io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException()
            );
        }
    }

    @Override
    public void getSimilarEvents(
            SimilarEventsRequestProto request,
            StreamObserver<RecommendedEventProto> responseObserver) {

        log.info("Getting similar events: eventId={}, userId={}",
                request.getEventId(), request.getUserId());

        try {
            List<RecommendedEventProto> similarEvents = recommendationService
                    .getSimilarEvents(request.getEventId(), request.getUserId(), request.getMaxResults());

            for (RecommendedEventProto rec : similarEvents) {
                responseObserver.onNext(rec);
            }
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error getting similar events", e);
            responseObserver.onError(
                    io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException()
            );
        }
    }

    @Override
    public void getInteractionsCount(
            InteractionsCountRequestProto request,
            StreamObserver<RecommendedEventProto> responseObserver) {

        log.info("Getting interactions count for {} events", request.getEventIdCount());

        try {
            List<RecommendedEventProto> interactions = recommendationService
                    .getInteractionsCount(request.getEventIdList());

            for (RecommendedEventProto rec : interactions) {
                responseObserver.onNext(rec);
            }
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error getting interactions count", e);
            responseObserver.onError(
                    io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException()
            );
        }
    }
}