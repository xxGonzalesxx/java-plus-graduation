package client;

import com.google.protobuf.Timestamp;
import io.grpc.ManagedChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.UserActionProto;

import java.time.Instant;


@Slf4j
@Service
public class CollectorClient {

    private static final String SERVICE_ID = "collector";

    private final GrpcChannelProvider channelProvider;

    public CollectorClient(GrpcChannelProvider channelProvider) {
        this.channelProvider = channelProvider;
    }

    /**
     * Отправить действие "просмотр"
     */
    public void sendView(long userId, long eventId) {
        sendUserAction(userId, eventId, ActionTypeProto.ACTION_VIEW);
    }

    /**
     * Отправить действие "регистрация"
     */
    public void sendRegister(long userId, long eventId) {
        sendUserAction(userId, eventId, ActionTypeProto.ACTION_REGISTER);
    }

    /**
     * Отправить действие "лайк"
     */
    public void sendLike(long userId, long eventId) {
        sendUserAction(userId, eventId, ActionTypeProto.ACTION_LIKE);
    }

    private void sendUserAction(long userId, long eventId, ActionTypeProto actionType) {
        try {
            ManagedChannel channel = channelProvider.getChannel(SERVICE_ID);
            UserActionControllerGrpc.UserActionControllerBlockingStub stub =
                    UserActionControllerGrpc.newBlockingStub(channel);

            Instant now = Instant.now();
            UserActionProto request = UserActionProto.newBuilder()
                    .setUserId(userId)
                    .setEventId(eventId)
                    .setActionType(actionType)
                    .setTimestamp(Timestamp.newBuilder()
                            .setSeconds(now.getEpochSecond())
                            .setNanos(now.getNano())
                            .build())
                    .build();

            stub.collectUserAction(request);
            log.debug("User action sent: userId={}, eventId={}, actionType={}",
                    userId, eventId, actionType);
        } catch (Exception e) {
            log.warn("Failed to send user action to collector-service: userId={}, eventId={}, actionType={}",
                    userId, eventId, actionType, e);
        }
    }
}