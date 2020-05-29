package com.sequenceiq.cloudbreak.audit;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.sequenceiq.cloudbreak.util.UuidUtil.uuidSupplier;
import static io.grpc.internal.GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.sequenceiq.cloudbreak.audit.model.ConfigInfo;
import com.sequenceiq.cloudbreak.audit.util.ActorUtil;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;
import com.sequenceiq.cloudbreak.grpc.altus.AltusMetadataInterceptor;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

@Component
public class AuditClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditClient.class);

    private final AuditConfig auditConfig;

    private final AuditEventToGrpcAuditEventConverter auditEventConverter;

    private final AttemptAuditEventResultToGrpcAttemptAuditEventResultConverter resultConverter;

    private final ActorUtil actorUtil;

    public AuditClient(AuditConfig auditConfig, AuditEventToGrpcAuditEventConverter auditEventConverter,
            AttemptAuditEventResultToGrpcAttemptAuditEventResultConverter resultConverter, ActorUtil actorUtil) {
        this.auditConfig = auditConfig;
        this.auditEventConverter = auditEventConverter;
        this.resultConverter = resultConverter;
        this.actorUtil = actorUtil;
    }

    public ConfigInfo getExportConfig(String actorCrn, String requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            String internalRequestId = Optional.ofNullable(requestId).orElseGet(uuidSupplier());

            AuditProto.ConfigInfo response = newStub(channelWrapper.getChannel(), internalRequestId, actorCrn)
                    .getExportConfig(AuditProto.GetExportConfigRequest.newBuilder().build()).getConfiguration();

            return ConfigInfo.builder()
                    .withActorCrn(actorCrn)
                    .withRequestId(internalRequestId)
                    .withEnabled(response.getEnabled())
                    .withCredentialName(response.getCredentialName())
                    .withStorageRegion(response.getStorageRegion())
                    .withStorageLocation(response.getStorageLocation())
                    .build();
        }
    }

    public ConfigInfo configureExport(ConfigInfo configInfo) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            String actorCrn = configInfo.getActorCrn();
            String requestId = Optional.ofNullable(configInfo.getRequestId()).orElseGet(uuidSupplier());
            AuditProto.ConfigInfo response = newStub(channelWrapper.getChannel(), requestId, actorCrn)
                    .configureExport(AuditProto.ConfigureExportRequest.newBuilder()
                            .setEnabled(configInfo.isEnabled())
                            .setCredentialName(configInfo.getCredentialName())
                            .setStorageRegion(configInfo.getStorageRegion())
                            .setStorageLocation(configInfo.getStorageLocation())
                            .build()).getConfiguration();

            return ConfigInfo.builder()
                    .withActorCrn(actorCrn)
                    .withRequestId(requestId)
                    .withEnabled(response.getEnabled())
                    .withCredentialName(response.getCredentialName())
                    .withStorageRegion(response.getStorageRegion())
                    .withStorageLocation(response.getStorageLocation())
                    .build();
        }
    }

    public void createAuditEvent(AuditEvent auditEvent) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            String actorCrn = actorUtil.getActorCrn(auditEvent.getActor());
            LOGGER.info("Audit log entry will be created: {}", auditEvent);
            AuditProto.AuditEvent protoAuditEvent = auditEventConverter.convert(auditEvent);
            newStub(channelWrapper.getChannel(), protoAuditEvent.getRequestId(), actorCrn)
                    .createAuditEvent(CreateAuditEventRequest.newBuilder()
                            .setAuditEvent(protoAuditEvent)
                            .build());
        }
    }

    public void createAttemptAuditEvent(AuditEvent auditEvent) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            String actorCrn = actorUtil.getActorCrn(auditEvent.getActor());
            AuditProto.AuditEvent protoAuditEvent = auditEventConverter.convert(auditEvent);
            newStub(channelWrapper.getChannel(), protoAuditEvent.getRequestId(), actorCrn)
                    .createAttemptAuditEvent(CreateAttemptAuditEventRequest.newBuilder()
                            .setAuditEvent(protoAuditEvent)
                            .build());
        }
    }

    public void updateAttemptAuditEventWithResult(AttemptAuditEventResult attemptAuditEventResult) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            String requestId = Optional.ofNullable(attemptAuditEventResult.getRequestId()).orElseGet(uuidSupplier());
            AuditProto.AttemptAuditEventResult protoAttemptAuditEventResult = resultConverter.convert(attemptAuditEventResult);
            newStub(channelWrapper.getChannel(), requestId, attemptAuditEventResult.getActorCrn())
                    .updateAttemptAuditEventWithResult(UpdateAttemptAuditEventWithResultRequest.newBuilder()
                            .setResult(protoAttemptAuditEventResult)
                            .build());
        }
    }

    /**
     * Creates Managed Channel wrapper from endpoint address
     *
     * @return the wrapper object
     */
    private ManagedChannelWrapper makeWrapper() {
        return new ManagedChannelWrapper(
                ManagedChannelBuilder.forAddress(auditConfig.getEndpoint(), auditConfig.getPort())
                        .usePlaintext()
                        .maxInboundMessageSize(DEFAULT_MAX_MESSAGE_SIZE)
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
        checkNotNull(requestId);
        return AuditGrpc.newBlockingStub(channel)
                .withInterceptors(new AltusMetadataInterceptor(requestId, actorCrn));
    }
}
