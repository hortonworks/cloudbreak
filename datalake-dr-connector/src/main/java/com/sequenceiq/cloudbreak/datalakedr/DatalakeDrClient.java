package com.sequenceiq.cloudbreak.datalakedr;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.datalakedr.datalakeDRGrpc;
import com.cloudera.thunderhead.service.datalakedr.datalakeDRGrpc.datalakeDRBlockingStub;
import com.cloudera.thunderhead.service.datalakedr.datalakeDRProto;
import com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeRequest;
import com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusRequest;
import com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.DatalakeBackupInfo;
import com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.ListDatalakeBackupRequest;
import com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.ListDatalakeBackupResponse;
import com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeRequest;
import com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeStatusRequest;
import com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.SkipFlag;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.datalakedr.config.DatalakeDrConfig;
import com.sequenceiq.cloudbreak.datalakedr.converter.DatalakeDataInfoJsonToObjectConverter;
import com.sequenceiq.cloudbreak.datalakedr.converter.GrpcStatusResponseToDatalakeBackupRestoreStatusResponseConverter;
import com.sequenceiq.cloudbreak.datalakedr.model.DatalakeBackupStatusResponse;
import com.sequenceiq.cloudbreak.datalakedr.model.DatalakeOperationStatus;
import com.sequenceiq.cloudbreak.datalakedr.model.DatalakeRestoreStatusResponse;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;
import com.sequenceiq.cloudbreak.grpc.altus.AltusMetadataInterceptor;
import com.sequenceiq.cloudbreak.grpc.util.GrpcUtil;

import io.grpc.ManagedChannel;

@Component
public class DatalakeDrClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeDrClient.class);

    private static final String NO_CONNECTOR_ERROR = "The thunderhead-datalakedr service connector is not configured. Cannot get status.";

    private final DatalakeDrConfig datalakeDrConfig;

    private final GrpcStatusResponseToDatalakeBackupRestoreStatusResponseConverter statusConverter;

    @Qualifier("datalakeDrManagedChannelWrapper")
    @Inject
    private ManagedChannelWrapper channelWrapper;

    @Inject
    private DatalakeDataInfoJsonToObjectConverter datalakeDataInfoJsonToObjectConverter;

    public DatalakeDrClient(DatalakeDrConfig datalakeDrConfig, GrpcStatusResponseToDatalakeBackupRestoreStatusResponseConverter statusConverter) {
        this.datalakeDrConfig = datalakeDrConfig;
        this.statusConverter = statusConverter;
    }

    public DatalakeBackupStatusResponse triggerBackup(String datalakeName, String backupLocation, String backupName, String actorCrn,
            DatalakeDrSkipOptions datalakeDrSkipOptions) {
        if (!datalakeDrConfig.isConfigured()) {
            return missingConnectorResponseOnBackup();
        }

        checkNotNull(datalakeName);
        checkNotNull(actorCrn, "actorCrn should not be null.");
        checkNotNull(backupLocation);

        BackupDatalakeRequest.Builder builder = BackupDatalakeRequest.newBuilder()
                .setDatalakeName(datalakeName)
                .setBackupLocation(backupLocation)
                .setCloseDbConnections(true);

        if (datalakeDrSkipOptions.isSkipValidation()) {
            builder.setSkipValidation(true);
        }
        if (datalakeDrSkipOptions.isSkipAtlasMetadata()) {
            builder.setSkipAtlasMetadata(SkipFlag.SKIP);
        }
        if (datalakeDrSkipOptions.isSkipRangerAudits()) {
            builder.setSkipRangerAudits(SkipFlag.SKIP);
        }
        if (datalakeDrSkipOptions.isSkipRangerMetadata()) {
            builder.setSkipRangerHmsMetadata(SkipFlag.SKIP);
        }
        if (!Strings.isNullOrEmpty(backupName)) {
            builder.setBackupName(backupName);
        }
        return statusConverter.convert(
                newStub(channelWrapper.getChannel(), UUID.randomUUID().toString(), actorCrn)
                        .backupDatalake(builder.build())
        );
    }

    public DatalakeBackupStatusResponse triggerBackupValidation(String datalakeName, String backupLocation, String actorCrn) {
        if (!datalakeDrConfig.isConfigured()) {
            return missingConnectorResponseOnBackup();
        }

        checkNotNull(datalakeName);
        checkNotNull(actorCrn, "actorCrn should not be null.");
        checkNotNull(backupLocation);

        BackupDatalakeRequest.Builder builder = BackupDatalakeRequest.newBuilder()
                .setDatalakeName(datalakeName)
                .setBackupLocation(backupLocation)
                .setValidationOnly(true);
        return statusConverter.convert(
                newStub(channelWrapper.getChannel(), UUID.randomUUID().toString(), actorCrn)
                        .backupDatalake(builder.build())
        );
    }

    public DatalakeRestoreStatusResponse triggerRestore(String datalakeName, String backupId, String backupLocationOverride, String actorCrn,
            DatalakeDrSkipOptions datalakeDrSkipOptions) {
        if (!datalakeDrConfig.isConfigured()) {
            return missingConnectorResponseOnRestore();
        }
        checkNotNull(datalakeName);
        checkNotNull(actorCrn, "actorCrn should not be null.");

        RestoreDatalakeRequest.Builder builder = RestoreDatalakeRequest.newBuilder()
                .setDatalakeName(datalakeName);
        if (!Strings.isNullOrEmpty(backupId)) {
            builder.setBackupId(backupId);
        }
        if (datalakeDrSkipOptions.isSkipValidation()) {
            builder.setSkipValidation(true);
        }
        if (datalakeDrSkipOptions.isSkipAtlasMetadata()) {
            builder.setSkipAtlasMetadata(SkipFlag.SKIP);
        }
        if (datalakeDrSkipOptions.isSkipRangerAudits()) {
            builder.setSkipRangerAudits(SkipFlag.SKIP);
        }
        if (datalakeDrSkipOptions.isSkipRangerMetadata()) {
            builder.setSkipRangerHmsMetadata(SkipFlag.SKIP);
        }
        if (!Strings.isNullOrEmpty(backupLocationOverride)) {
            builder.setBackupLocationOverride(backupLocationOverride);
        }
        return statusConverter.convert(
                newStub(channelWrapper.getChannel(), UUID.randomUUID().toString(), actorCrn)
                        .restoreDatalake(builder.build())
        );
    }

    public DatalakeBackupStatusResponse getBackupStatus(String datalakeName, String actorCrn) {
        return getBackupStatus(datalakeName, null, null, actorCrn);
    }

    public DatalakeBackupStatusResponse getBackupStatus(String datalakeName, String backupId, String actorCrn) {
        return getBackupStatus(datalakeName, backupId, null, actorCrn);
    }

    public DatalakeBackupStatusResponse getBackupStatus(String datalakeName, String backupId, String backupName, String actorCrn) {
        if (!datalakeDrConfig.isConfigured()) {
            return missingConnectorResponseOnBackup();
        }

        checkNotNull(datalakeName);
        checkNotNull(actorCrn, "actorCrn should not be null.");

        BackupDatalakeStatusRequest.Builder builder = BackupDatalakeStatusRequest.newBuilder()
                .setDatalakeName(datalakeName);
        if (!Strings.isNullOrEmpty(backupId)) {
            builder.setBackupId(backupId);
        }
        if (!Strings.isNullOrEmpty(backupName)) {
            builder.setBackupName(backupName);
        }
        return statusConverter.convert(
                newStub(channelWrapper.getChannel(), UUID.randomUUID().toString(), actorCrn)
                        .backupDatalakeStatus(builder.build())
        );
    }

    public String getBackupId(String datalakeName, String backupName, String actorCrn) {
        if (!datalakeDrConfig.isConfigured()) {
            throw new IllegalStateException("altus.datalakedr.endpoint is not enabled or configured appropriately!");
        }

        checkNotNull(datalakeName);
        checkNotNull(actorCrn, "actorCrn should not be null.");

        BackupDatalakeStatusRequest.Builder builder = BackupDatalakeStatusRequest.newBuilder()
                .setDatalakeName(datalakeName);
        if (!Strings.isNullOrEmpty(backupName)) {
            builder.setBackupName(backupName);
        }
        return newStub(channelWrapper.getChannel(), UUID.randomUUID().toString(), actorCrn).backupDatalakeStatus(builder.build()).getBackupId();
    }

    public DatalakeBackupStatusResponse getBackupStatusByBackupId(String datalakeName, String backupId, String actorCrn) {
        return getBackupStatusByBackupId(datalakeName, backupId, null, actorCrn);
    }

    public DatalakeBackupStatusResponse getBackupStatusByBackupId(String datalakeName, String backupId,
            String backupName, String actorCrn) {
        if (!datalakeDrConfig.isConfigured()) {
            return missingConnectorResponseOnBackup();
        }

        checkNotNull(datalakeName);
        checkNotNull(actorCrn, "actorCrn should not be null.");
        checkNotNull(backupId);

        BackupDatalakeStatusRequest.Builder builder = BackupDatalakeStatusRequest.newBuilder()
                .setDatalakeName(datalakeName)
                .setBackupId(backupId);
        if (!Strings.isNullOrEmpty(backupName)) {
            builder.setBackupName(backupName);
        }
        return statusConverter.convert(
                newStub(channelWrapper.getChannel(), UUID.randomUUID().toString(), actorCrn)
                        .backupDatalakeStatus(builder.build())
        );
    }

    public DatalakeRestoreStatusResponse getRestoreStatusByRestoreId(String datalakeName, String restoreId, String actorCrn) {
        if (!datalakeDrConfig.isConfigured()) {
            return missingConnectorResponseOnRestore();
        }

        checkNotNull(datalakeName);
        checkNotNull(actorCrn, "actorCrn should not be null.");
        checkNotNull(restoreId);

        RestoreDatalakeStatusRequest.Builder builder = RestoreDatalakeStatusRequest.newBuilder()
                .setDatalakeName(datalakeName)
                .setRestoreId(restoreId);

        return statusConverter.convert(
                newStub(channelWrapper.getChannel(), UUID.randomUUID().toString(), actorCrn)
                        .restoreDatalakeStatus(builder.build())
        );
    }

    public DatalakeRestoreStatusResponse getRestoreStatus(String datalakeName, String actorCrn) {
        return getRestoreStatus(datalakeName, null, null, actorCrn);
    }

    public DatalakeRestoreStatusResponse getRestoreStatus(String datalakeName, String restoreId, String actorCrn) {
        return getRestoreStatus(datalakeName, restoreId, null, actorCrn);
    }

    public DatalakeRestoreStatusResponse getRestoreStatus(String datalakeName, String restoreId, String backupName, String actorCrn) {
        if (!datalakeDrConfig.isConfigured()) {
            return missingConnectorResponseOnRestore();
        }

        checkNotNull(datalakeName);
        checkNotNull(actorCrn, "actorCrn should not be null.");

        RestoreDatalakeStatusRequest.Builder builder = RestoreDatalakeStatusRequest.newBuilder()
                .setDatalakeName(datalakeName);
        if (!Strings.isNullOrEmpty(restoreId)) {
            builder.setRestoreId(restoreId);
        }
        return statusConverter.convert(
                newStub(channelWrapper.getChannel(), UUID.randomUUID().toString(), actorCrn)
                        .restoreDatalakeStatus(builder.build())
        );
    }

    public String getRestoreId(String datalakeName, String backupName, String actorCrn) {
        if (!datalakeDrConfig.isConfigured()) {
            throw new IllegalStateException("altus.datalakedr.endpoint is not enabled or configured appropriately!");
        }

        checkNotNull(datalakeName);
        checkNotNull(actorCrn, "actorCrn should not be null.");

        RestoreDatalakeStatusRequest.Builder builder = RestoreDatalakeStatusRequest.newBuilder()
                .setDatalakeName(datalakeName);
        return newStub(channelWrapper.getChannel(), UUID.randomUUID().toString(), actorCrn).restoreDatalakeStatus(builder.build()).getRestoreId();
    }

    public DatalakeBackupInfo getLastSuccessfulBackup(String datalakeName, String actorCrn, Optional<String> runtime) {
        if (!datalakeDrConfig.isConfigured()) {
            return null;
        }

        checkNotNull(datalakeName);
        checkNotNull(actorCrn, "actorCrn should not be null.");

        ListDatalakeBackupRequest.Builder builder = ListDatalakeBackupRequest.newBuilder()
                .setDatalakeName(datalakeName);
        ListDatalakeBackupResponse response = newStub(channelWrapper.getChannel(), UUID.randomUUID().toString(), actorCrn)
                .listDatalakeBackups(builder.build());

        if (response != null) {
            return response.getDatalakeInfoList().stream()
                    .filter(backup -> "SUCCESSFUL".equals(backup.getOverallState()))
                    .filter(backup -> runtime.isEmpty() || backup.getRuntimeVersion().equalsIgnoreCase(runtime.get()))
                    .peek(backupInfo -> LOGGER.debug(
                            "The following successful backup was found for data lake {} and runtime {}: {}", datalakeName, runtime, backupInfo))
                    .findFirst()
                    .orElse(null);
        }
        LOGGER.debug("No successful backup was found for data lake {} and runtime {}", datalakeName, runtime);
        return null;
    }

    public DatalakeBackupInfo getBackupById(String datalakeName, String backupId, String actorCrn) {
        DatalakeBackupInfo datalakeBackupInfo = null;
        if (!datalakeDrConfig.isConfigured()) {
            return null;
        }

        checkNotNull(datalakeName);
        checkNotNull(backupId);
        checkNotNull(actorCrn, "actorCrn should not be null.");

        ListDatalakeBackupRequest.Builder builder = ListDatalakeBackupRequest.newBuilder()
                .setDatalakeName(datalakeName);
        ListDatalakeBackupResponse response = newStub(channelWrapper.getChannel(), UUID.randomUUID().toString(), actorCrn)
                .listDatalakeBackups(builder.build());

        if (response != null) {
            datalakeBackupInfo = response.getDatalakeInfoList().stream()
                    .filter(backup -> backupId.equals(backup.getBackupId()))
                    .findFirst().orElse(null);
        }
        return datalakeBackupInfo;
    }

    public datalakeDRProto.SubmitDatalakeDataInfoResponse submitDatalakeDataInfo(String operationId, String inputJson, String actorCrn) {
        if (!datalakeDrConfig.isConfigured()) {
            return null;
        }

        checkNotNull(operationId);
        checkNotNull(inputJson);
        checkNotNull(actorCrn, "actorCrn should not be null.");

        datalakeDRProto.DatalakeDataInfoObject datalakeDataInfoObject = datalakeDataInfoJsonToObjectConverter.convert(operationId, inputJson);
        return newStub(channelWrapper.getChannel(), UUID.randomUUID().toString(), actorCrn)
                .submitDatalakeDataInfo(datalakeDataInfoObject);
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
        checkNotNull(requestId, "requestId should not be null.");
        return datalakeDRGrpc.newBlockingStub(channel)
                .withInterceptors(
                        GrpcUtil.getTimeoutInterceptor(datalakeDrConfig.getGrpcTimeoutSec()),
                        new AltusMetadataInterceptor(requestId, actorCrn));
    }

    private DatalakeBackupStatusResponse missingConnectorResponseOnBackup() {
        return new DatalakeBackupStatusResponse(UUID.randomUUID().toString(),
                DatalakeOperationStatus.State.FAILED,
                List.of(), "", NO_CONNECTOR_ERROR
        );
    }

    private DatalakeRestoreStatusResponse missingConnectorResponseOnRestore() {
        return new DatalakeRestoreStatusResponse(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                DatalakeOperationStatus.State.FAILED, List.of(),
                NO_CONNECTOR_ERROR);
    }
}
