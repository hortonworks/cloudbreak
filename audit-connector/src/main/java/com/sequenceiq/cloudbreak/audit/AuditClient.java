package com.sequenceiq.cloudbreak.audit;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.sequenceiq.cloudbreak.util.NullUtil.doIfNotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.audit.AuditGrpc;
import com.cloudera.thunderhead.service.audit.AuditGrpc.AuditBlockingStub;
import com.cloudera.thunderhead.service.audit.AuditProto;
import com.cloudera.thunderhead.service.audit.AuditProto.CreateAttemptAuditEventRequest;
import com.cloudera.thunderhead.service.audit.AuditProto.CreateAuditEventRequest;
import com.cloudera.thunderhead.service.audit.AuditProto.UpdateAttemptAuditEventWithResultRequest;
import com.sequenceiq.cloudbreak.audit.config.AuditConfig;
import com.sequenceiq.cloudbreak.audit.converter.AttemptAuditEventResultToGrpcAttemptAuditEventResultConverter;
import com.sequenceiq.cloudbreak.audit.converter.AuditEventToGrpcAuditEventConverter;
import com.sequenceiq.cloudbreak.audit.model.AttemptAuditEventResult;
import com.sequenceiq.cloudbreak.audit.model.AuditEvent;
import com.sequenceiq.cloudbreak.audit.model.ListAuditEvent;
import com.sequenceiq.cloudbreak.audit.util.ActorUtil;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;
import com.sequenceiq.cloudbreak.grpc.altus.AltusMetadataInterceptor;
import com.sequenceiq.cloudbreak.grpc.util.GrpcUtil;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

import io.grpc.ManagedChannel;

@Component
public class AuditClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditClient.class);

    private final AuditConfig auditConfig;

    private final AuditEventToGrpcAuditEventConverter auditEventConverter;

    private final AttemptAuditEventResultToGrpcAttemptAuditEventResultConverter resultConverter;

    private final ActorUtil actorUtil;

    @Qualifier("auditManagedChannelWrapper")
    @Inject
    private ManagedChannelWrapper channelWrapper;

    public AuditClient(AuditConfig auditConfig, AuditEventToGrpcAuditEventConverter auditEventConverter,
            AttemptAuditEventResultToGrpcAttemptAuditEventResultConverter resultConverter, ActorUtil actorUtil) {
        this.auditConfig = auditConfig;
        this.auditEventConverter = auditEventConverter;
        this.resultConverter = resultConverter;
        this.actorUtil = actorUtil;
    }

    @Retryable(value = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 5000))
    public void createAuditEvent(AuditEvent auditEvent) {
        String actorCrn = actorUtil.getActorCrn(auditEvent.getActor());
        LOGGER.info("Audit log entry will be created: {}", auditEvent);
        AuditProto.AuditEvent protoAuditEvent = auditEventConverter.convert(auditEvent);
        newStub(channelWrapper.getChannel(), protoAuditEvent.getRequestId(), actorCrn)
                .createAuditEvent(CreateAuditEventRequest.newBuilder()
                        .setAuditEvent(protoAuditEvent)
                        .build());
    }

    public void createAttemptAuditEvent(AuditEvent auditEvent) {
        String actorCrn = actorUtil.getActorCrn(auditEvent.getActor());
        AuditProto.AuditEvent protoAuditEvent = auditEventConverter.convert(auditEvent);
        newStub(channelWrapper.getChannel(), protoAuditEvent.getRequestId(), actorCrn)
                .createAttemptAuditEvent(CreateAttemptAuditEventRequest.newBuilder()
                        .setAuditEvent(protoAuditEvent)
                        .build());
    }

    public void updateAttemptAuditEventWithResult(AttemptAuditEventResult attemptAuditEventResult) {
        String requestId = Optional.ofNullable(attemptAuditEventResult.getRequestId()).orElse(MDCBuilder.getOrGenerateRequestId());
        AuditProto.AttemptAuditEventResult protoAttemptAuditEventResult = resultConverter.convert(attemptAuditEventResult);
        newStub(channelWrapper.getChannel(), requestId, attemptAuditEventResult.getActorCrn())
                .updateAttemptAuditEventWithResult(UpdateAttemptAuditEventWithResultRequest.newBuilder()
                        .setResult(protoAttemptAuditEventResult)
                        .build());
    }

    /**
     * Creates a new stub with the appropriate metadata injecting interceptors.
     *
     * @param channel   channel
     * @param requestId the request ID
     * @param actorCrn  actor
     * @return the stub
     */
    private AuditBlockingStub newStub(ManagedChannel channel, String requestId, String actorCrn) {
        checkNotNull(requestId, "requestId should not be null.");
        return AuditGrpc.newBlockingStub(channel)
                .withInterceptors(
                        GrpcUtil.getTimeoutInterceptor(auditConfig.getGrpcTimeoutSec()),
                        new AltusMetadataInterceptor(requestId, actorCrn)
                );
    }

    public List<AuditProto.CdpAuditEvent> listEvents(ListAuditEvent listAuditEvent) {
        String actorCrn = actorUtil.getActorCrn(listAuditEvent.getActor());
        AuditProto.ListEventsRequest.Builder builder = AuditProto.ListEventsRequest.newBuilder();
        doIfNotNull(listAuditEvent.getEventSource(), eventSource -> builder.setEventSource(eventSource.getName()));
        doIfNotNull(listAuditEvent.getAccountId(), builder::setAccountId);
        doIfNotNull(listAuditEvent.getRequestId(), builder::setRequestId);
        doIfNotNull(listAuditEvent.getFromTimestamp(), builder::setFromTimestamp);
        doIfNotNull(listAuditEvent.getToTimestamp(), builder::setToTimestamp);
        doIfNotNull(listAuditEvent.getPageSize(), builder::setPageSize);
        doIfNotNull(listAuditEvent.getPageToken(), builder::setPageToken);
        AuditProto.ListEventsResponse listEventsResponse = newStub(channelWrapper.getChannel(), UUID.randomUUID().toString(), actorCrn)
                .listEvents(builder.build());
        return listEventsResponse.getAuditEventList();
    }
}
