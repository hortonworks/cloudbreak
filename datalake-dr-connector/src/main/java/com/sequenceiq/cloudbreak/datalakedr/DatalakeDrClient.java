package com.sequenceiq.cloudbreak.datalakedr;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.grpc.internal.GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE;

import com.cloudera.thunderhead.service.datalakedr.datalakeDRGrpc;
import com.cloudera.thunderhead.service.datalakedr.datalakeDRGrpc.datalakeDRBlockingStub;
import com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusRequest;
import com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeStatusRequest;
import com.sequenceiq.cloudbreak.datalakedr.config.DatalakeDrConfig;
import com.sequenceiq.cloudbreak.datalakedr.converter.GrpcStatusResponseToDatalakeDrStatusResponseConverter;
import com.sequenceiq.cloudbreak.datalakedr.model.DatalakeDrStatusResponse;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;
import com.sequenceiq.cloudbreak.grpc.altus.AltusMetadataInterceptor;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DatalakeDrClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeDrClient.class);

    private final DatalakeDrConfig datalakeDrConfig;

    private final GrpcStatusResponseToDatalakeDrStatusResponseConverter statusConverter;

    public DatalakeDrClient(DatalakeDrConfig datalakeDrConfig, GrpcStatusResponseToDatalakeDrStatusResponseConverter statusConverter) {
        this.datalakeDrConfig = datalakeDrConfig;
        this.statusConverter = statusConverter;
    }

    public DatalakeDrStatusResponse getBackupStatusByBackupId(String datalakeName, String backupId, String actorCrn) {
        checkNotNull(datalakeName);
        checkNotNull(actorCrn);
        checkNotNull(backupId);

        actorCrn = "crn:altus:iam:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:user:8d1e890c-8a2e-4cf9-96bd-d50478bc027e";
        datalakeName = "biglauer-az-dr-4-dl";

        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            BackupDatalakeStatusRequest.Builder builder = BackupDatalakeStatusRequest.newBuilder()
                .setDatalakeName(datalakeName);
//                .setBackupId(backupId);

            return statusConverter.convert(
                newStub(channelWrapper.getChannel(), UUID.randomUUID().toString(), actorCrn)
                    .backupDatalakeStatus(builder.build())
            );
        }
    }

    public DatalakeDrStatusResponse getRestoreStatusByRestoreId(String datalakeName, String restoreId, String actorCrn) {
        checkNotNull(datalakeName);
        checkNotNull(actorCrn);
        checkNotNull(restoreId);

        actorCrn = "crn:altus:iam:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:user:8d1e890c-8a2e-4cf9-96bd-d50478bc027e";
        datalakeName = "biglauer-az-dr-4-dl";

        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            RestoreDatalakeStatusRequest.Builder builder = RestoreDatalakeStatusRequest.newBuilder()
                .setDatalakeName(datalakeName);
//                .setRestoreId(restoreId);

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
            .withInterceptors(new AltusMetadataInterceptor(requestId, actorCrn));
    }
}
