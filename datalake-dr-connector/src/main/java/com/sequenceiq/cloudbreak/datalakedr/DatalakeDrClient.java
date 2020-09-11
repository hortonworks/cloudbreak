package com.sequenceiq.cloudbreak.datalakedr;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.grpc.internal.GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE;

import com.cloudera.thunderhead.service.datalakedr.datalakeDRGrpc;
import com.cloudera.thunderhead.service.datalakedr.datalakeDRGrpc.datalakeDRBlockingStub;
import com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusRequest;
import com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusResponse;
import com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeStatusRequest;
import com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeStatusResponse;
import com.sequenceiq.cloudbreak.datalakedr.config.DatalakeDrConfig;
import com.sequenceiq.cloudbreak.datalakedr.model.DatalakeDrStatusResponse;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;
import com.sequenceiq.cloudbreak.grpc.altus.AltusMetadataInterceptor;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DatalakeDrClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeDrClient.class);

    private final DatalakeDrConfig datalakeDrConfig;

    public DatalakeDrClient(DatalakeDrConfig datalakeDrConfig) {
        LOGGER.info("HER Creating DatalakeDrClient");
        this.datalakeDrConfig = datalakeDrConfig;
    }

    public DatalakeDrStatusResponse getBackupStatus(String datalakeName, String backupId, String actorCrn) {
        checkNotNull(datalakeName);
//        checkNotNull(actorCrn);

        LOGGER.info("HER datalakeName " + datalakeName);
        LOGGER.info("HER actorCrn " + String.valueOf(actorCrn));
        LOGGER.info("HER backupId " + String.valueOf(backupId));

        actorCrn = "crn:altus:iam:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:user:f69b8b5a-e955-4b28-b482-65dbaecab076";
        datalakeName = "hreeve-dev-sdx";

        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            BackupDatalakeStatusRequest.Builder builder = BackupDatalakeStatusRequest.newBuilder()
                .setDatalakeName(datalakeName);
            if (backupId != null) {
                builder.setBackupId(backupId);
            }

            BackupDatalakeStatusResponse response = newStub(channelWrapper.getChannel(), UUID.randomUUID().toString(), actorCrn)
                .backupDatalakeStatus(builder.build());

            return new DatalakeDrStatusResponse(
                DatalakeDrStatusResponse.State.valueOf(response.getOverallState()),
                Optional.ofNullable(response.getFailureReason())
            );
        }
    }

    public DatalakeDrStatusResponse getRestoreStatus(String datalakeName, String restoreId, String actorCrn) {
        checkNotNull(datalakeName);
        checkNotNull(actorCrn);

        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            RestoreDatalakeStatusRequest.Builder builder = RestoreDatalakeStatusRequest.newBuilder()
                .setDatalakeName(datalakeName);
            if (restoreId != null) {
                builder.setRestoreId(restoreId);
            }

            RestoreDatalakeStatusResponse response = newStub(channelWrapper.getChannel(), UUID.randomUUID().toString(), actorCrn)
                .restoreDatalakeStatus(builder.build());

            return new DatalakeDrStatusResponse(
                DatalakeDrStatusResponse.State.valueOf(response.getOverallState()),
                Optional.ofNullable(response.getFailureReason())
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
