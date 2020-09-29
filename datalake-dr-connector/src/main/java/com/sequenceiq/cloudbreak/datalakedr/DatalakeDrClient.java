package com.sequenceiq.cloudbreak.datalakedr;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.grpc.internal.GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.datalakedr.datalakeDRGrpc;
import com.cloudera.thunderhead.service.datalakedr.datalakeDRGrpc.datalakeDRBlockingStub;
import com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusRequest;
import com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeStatusRequest;
import com.sequenceiq.cloudbreak.datalakedr.config.DatalakeDrConfig;
import com.sequenceiq.cloudbreak.datalakedr.converter.GrpcStatusResponseToDatalakeDrStatusResponseConverter;
import com.sequenceiq.cloudbreak.datalakedr.model.DatalakeDrStatusResponse;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;
import com.sequenceiq.cloudbreak.grpc.altus.AltusMetadataInterceptor;
import com.sequenceiq.cloudbreak.grpc.util.GrpcUtil;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.opentracing.Tracer;

@Component
public class DatalakeDrClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeDrClient.class);

    private static final String NO_CONNECTOR_ERROR = "The thunderhead-datalakedr service connector is not configured. Cannot get status.";

    private final DatalakeDrConfig datalakeDrConfig;

    private final GrpcStatusResponseToDatalakeDrStatusResponseConverter statusConverter;

    private final Tracer tracer;

    public DatalakeDrClient(DatalakeDrConfig datalakeDrConfig, GrpcStatusResponseToDatalakeDrStatusResponseConverter statusConverter, Tracer tracer) {
        this.datalakeDrConfig = datalakeDrConfig;
        this.statusConverter = statusConverter;
        this.tracer = tracer;
    }

    public DatalakeDrStatusResponse getBackupStatusByBackupId(String datalakeName, String backupId, String actorCrn) {
        if (!datalakeDrConfig.isConfigured()) {
            return missingConnectorResponse();
        }

        checkNotNull(datalakeName);
        checkNotNull(actorCrn);
        checkNotNull(backupId);

        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            BackupDatalakeStatusRequest.Builder builder = BackupDatalakeStatusRequest.newBuilder()
                .setDatalakeName(datalakeName)
                .setBackupId(backupId);

            return statusConverter.convert(
                newStub(channelWrapper.getChannel(), UUID.randomUUID().toString(), actorCrn)
                    .backupDatalakeStatus(builder.build())
            );
        }
    }

    public DatalakeDrStatusResponse getRestoreStatusByRestoreId(String datalakeName, String restoreId, String actorCrn) {
        if (!datalakeDrConfig.isConfigured()) {
            return missingConnectorResponse();
        }

        checkNotNull(datalakeName);
        checkNotNull(actorCrn);
        checkNotNull(restoreId);

        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            RestoreDatalakeStatusRequest.Builder builder = RestoreDatalakeStatusRequest.newBuilder()
                .setDatalakeName(datalakeName)
                .setRestoreId(restoreId);

            return statusConverter.convert(
                newStub(channelWrapper.getChannel(), UUID.randomUUID().toString(), actorCrn)
                    .restoreDatalakeStatus(builder.build())
            );
        }
    }

    /**
     * Creates Managed Channel wrapper from endpoint address
     *
     * @return the wrapper object
     */
    private ManagedChannelWrapper makeWrapper() {
        return new ManagedChannelWrapper(
            ManagedChannelBuilder.forAddress(datalakeDrConfig.getHost(), datalakeDrConfig.getPort())
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
    private datalakeDRBlockingStub newStub(ManagedChannel channel, String requestId, String actorCrn) {
        checkNotNull(requestId);
        return datalakeDRGrpc.newBlockingStub(channel)
            .withInterceptors(GrpcUtil.getTracingInterceptor(tracer), new AltusMetadataInterceptor(requestId, actorCrn));
    }

    private DatalakeDrStatusResponse missingConnectorResponse() {
        return new DatalakeDrStatusResponse(
            DatalakeDrStatusResponse.State.FAILED,
            Optional.of(NO_CONNECTOR_ERROR)
        );
    }
}
