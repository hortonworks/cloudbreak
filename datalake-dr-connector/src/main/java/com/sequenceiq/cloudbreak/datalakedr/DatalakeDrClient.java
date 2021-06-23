package com.sequenceiq.cloudbreak.datalakedr;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.grpc.internal.GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE;

import java.util.Optional;
import java.util.UUID;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.datalakedr.datalakeDRGrpc;
import com.cloudera.thunderhead.service.datalakedr.datalakeDRGrpc.datalakeDRBlockingStub;
import com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusRequest;
import com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeRequest;
import com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.DatalakeBackupInfo;
import com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.ListDatalakeBackupRequest;
import com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.ListDatalakeBackupResponse;
import com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeRequest;
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

    public DatalakeDrStatusResponse triggerBackup(String datalakeName, String backupLocation, String backupName, String actorCrn) {
        if (!datalakeDrConfig.isConfigured()) {
            return missingConnectorResponse();
        }

        checkNotNull(datalakeName);
        checkNotNull(actorCrn);
        checkNotNull(backupLocation);

        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            BackupDatalakeRequest.Builder builder = BackupDatalakeRequest.newBuilder()
                    .setDatalakeName(datalakeName)
                    .setBackupLocation(backupLocation);
            if (!Strings.isNullOrEmpty(backupName)) {
                builder.setBackupName(backupName);
            }
            return statusConverter.convert(
                    newStub(channelWrapper.getChannel(), UUID.randomUUID().toString(), actorCrn)
                            .backupDatalake(builder.build())
            );
        }
    }

    public DatalakeDrStatusResponse triggerRestore(String datalakeName, String backupId, String backupLocationOverride, String actorCrn) {
        if (!datalakeDrConfig.isConfigured()) {
            return missingConnectorResponse();
        }

        checkNotNull(datalakeName);
        checkNotNull(actorCrn);

        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            RestoreDatalakeRequest.Builder builder = RestoreDatalakeRequest.newBuilder()
                    .setDatalakeName(datalakeName);
            if (!Strings.isNullOrEmpty(backupId)) {
                builder.setBackupId(backupId);
            }
            if (!Strings.isNullOrEmpty(backupLocationOverride)) {
                builder.setBackupLocationOverride(backupLocationOverride);
            }
            return statusConverter.convert(
                    newStub(channelWrapper.getChannel(), UUID.randomUUID().toString(), actorCrn)
                            .restoreDatalake(builder.build())
            );
        }
    }

    public DatalakeDrStatusResponse getBackupStatusByBackupId(String datalakeName, String backupId, String actorCrn) {
        return getBackupStatusByBackupId(datalakeName, backupId, null, actorCrn);
    }

    public DatalakeDrStatusResponse getBackupStatusByBackupId(String datalakeName, String backupId,
        String backupName, String actorCrn) {
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
            if (!Strings.isNullOrEmpty(backupName)) {
                builder.setBackupName(backupName);
            }
            if (!Strings.isNullOrEmpty(backupId)) {
                builder.setBackupId(backupId);
            }
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

    public DatalakeBackupInfo getLastSuccessBackup(String datalakeName, String actorCrn) {
        DatalakeBackupInfo datalakeBackupInfo = null;
        if (!datalakeDrConfig.isConfigured()) {
            return null;
        }

        checkNotNull(datalakeName);
        checkNotNull(actorCrn);

        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            ListDatalakeBackupRequest.Builder builder = ListDatalakeBackupRequest.newBuilder()
                    .setDatalakeName(datalakeName);
            ListDatalakeBackupResponse response = newStub(channelWrapper.getChannel(), UUID.randomUUID().toString(), actorCrn)
                    .listDatalakeBackups(builder.build());

            if (response != null && response.getDatalakeInfoList() != null) {
                datalakeBackupInfo = response.getDatalakeInfoList().stream()
                        .filter(backup -> "SUCCESSFUL".equals(backup.getOverallState()))
                        .findFirst().orElse(null);
            }
            return datalakeBackupInfo;
        }
    }

    public DatalakeBackupInfo getBackupById(String datalakeName, String backupId, String actorCrn) {
        DatalakeBackupInfo datalakeBackupInfo = null;
        if (!datalakeDrConfig.isConfigured()) {
            return null;
        }

        checkNotNull(datalakeName);
        checkNotNull(backupId);
        checkNotNull(actorCrn);

        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            ListDatalakeBackupRequest.Builder builder = ListDatalakeBackupRequest.newBuilder()
                    .setDatalakeName(datalakeName);
            ListDatalakeBackupResponse response = newStub(channelWrapper.getChannel(), UUID.randomUUID().toString(), actorCrn)
                    .listDatalakeBackups(builder.build());

            if (response != null && response.getDatalakeInfoList() != null) {
                datalakeBackupInfo = response.getDatalakeInfoList().stream()
                        .filter(backup -> backupId.equals(backup.getBackupId()))
                        .findFirst().orElse(null);
            }
            return datalakeBackupInfo;
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
        return new DatalakeDrStatusResponse(UUID.randomUUID().toString(),
            DatalakeDrStatusResponse.State.FAILED,
            Optional.of(NO_CONNECTOR_ERROR)
        );
    }
}
