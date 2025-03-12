package com.sequenceiq.datalake.service.resize.recovery;

import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.CERT_RENEWAL_FAILED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.CERT_ROTATION_FAILED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.CLUSTER_AMBIGUOUS;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.CLUSTER_UNREACHABLE;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATAHUB_REFRESH_FAILED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_RESTORE_FAILED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DELETE_FAILED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.NODE_FAILURE;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.PROVISIONING_FAILED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.RECOVERY_FAILED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.RUNNING;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.START_FAILED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.STOPPED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.STOP_FAILED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.SYNC_FAILED;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recovery.RecoveryStatus;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.recovery.RecoveryService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.sdx.api.model.SdxRecoverableResponse;
import com.sequenceiq.sdx.api.model.SdxRecoveryRequest;
import com.sequenceiq.sdx.api.model.SdxRecoveryResponse;

@Component
/**
 * Provides entrypoint for recovery of failed SDX resize.
 *
 * The main entry point is {@code triggerRecovery}, which starts a cloudbreak Flow to recover the Data Lake.
 * ensure a Resize recovery is appropriate using {@code validateRecovery}
 *
 */
public class ResizeRecoveryService implements RecoveryService {

    @VisibleForTesting
    static final Set<DatalakeStatusEnum> FAILURE_STATES = Set.of(
            PROVISIONING_FAILED, DELETE_FAILED, START_FAILED, STOP_FAILED, CLUSTER_AMBIGUOUS, CLUSTER_UNREACHABLE,
            NODE_FAILURE, SYNC_FAILED, CERT_ROTATION_FAILED, CERT_RENEWAL_FAILED, DATALAKE_RESTORE_FAILED, RECOVERY_FAILED,
            DATAHUB_REFRESH_FAILED
    );

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Override
    public SdxRecoverableResponse validateRecovery(SdxCluster sdxCluster, SdxRecoveryRequest request) {
        if (!entitlementService.isDatalakeResizeRecoveryEnabled(ThreadBasedUserCrnProvider.getAccountId())) {
            return new SdxRecoverableResponse("Resize Recovery entitlement not enabled", RecoveryStatus.NON_RECOVERABLE);
        }
        SdxStatusEntity actualStatusForSdx = sdxStatusService.getActualStatusForSdx(sdxCluster);
        DatalakeStatusEnum status = actualStatusForSdx.getStatus();
        String statusReason = actualStatusForSdx.getStatusReason();

        if (getDetachedClusterFromResizedCluster(sdxCluster).isPresent()) {
            return validateRecoveryResizedClusterPresent(status, statusReason, request != null && request.isForced());
        }
        return validateRecoveryOnlyOriginalCluster(sdxCluster, status, statusReason);
    }

    @Override
    public SdxRecoverableResponse validateRecovery(SdxCluster sdxCluster) {
        return validateRecovery(sdxCluster, null);
    }

    @Override
    public SdxRecoveryResponse triggerRecovery(SdxCluster sdxCluster, SdxRecoveryRequest sdxRecoveryRequest) {
        Optional<SdxCluster> detachedCluster = getDetachedClusterFromResizedCluster(sdxCluster);
        if (detachedCluster.isPresent()) {
            return new SdxRecoveryResponse(sdxReactorFlowManager.triggerSdxResizeRecovery(detachedCluster.get(), Optional.ofNullable(sdxCluster)));
        }
        return new SdxRecoveryResponse(sdxReactorFlowManager.triggerSdxResizeRecovery(sdxCluster, Optional.empty()));
    }

    private Optional<SdxCluster> getDetachedClusterFromResizedCluster(SdxCluster cluster) {
        Optional<SdxCluster> detachedCluster = sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(
                cluster.getAccountId(), cluster.getEnvCrn()
        );
        if (detachedCluster.isPresent() && !detachedCluster.get().getId().equals(cluster.getId())) {
            return detachedCluster;
        }
        return Optional.empty();
    }

    private String getReasonNonRecoverable(DatalakeStatusEnum status) {
        if (RUNNING.equals(status)) {
            return "Datalake is running, resize can not be recovered from this point";
        } else if (DELETE_FAILED.equals(status)) {
            return "Failed to delete original data lake, not a recoverable error";
        } else {
            return "Cannot recover from resize due to Datalake not being in a recoverable status: " + status;
        }
    }

    private SdxRecoverableResponse validateRecoveryResizedClusterPresent(DatalakeStatusEnum status, String statusReason, boolean force) {
        if (force) {
            return new SdxRecoverableResponse("Force flag provided. Resized datalake with status " + status + " will be recovered by force, " +
                    "deleting the new resized datalake and recovering the old detached datalake.", RecoveryStatus.RECOVERABLE);
        }

        if (FAILURE_STATES.contains(status)) {
            return new SdxRecoverableResponse("Resized data lake is in a failed state. Recovery will restart the original data lake, " +
                    "and delete the new one", RecoveryStatus.RECOVERABLE);
        } else if (RUNNING.equals(status) && statusReason != null && statusReason.contains("Datalake restore failed")) {
            return new SdxRecoverableResponse(
                    "Failed to restore backup to new data lake, recovery will restart original data lake, and delete the new one",
                    RecoveryStatus.RECOVERABLE
            );
        }
        return new SdxRecoverableResponse(
                "Resize can not be recovered from this point for the new cluster: " + getReasonNonRecoverable(status),
                RecoveryStatus.NON_RECOVERABLE
        );
    }

    private SdxRecoverableResponse validateRecoveryOnlyOriginalCluster(SdxCluster sdxCluster, DatalakeStatusEnum status, String statusReason) {
        List<SdxCluster> sdxClusters = sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNull(sdxCluster.getAccountId(), sdxCluster.getEnvCrn());
        if (sdxClusters.size() > 1) {
            return new SdxRecoverableResponse(
                    "Resize can not be recovered from original cluster. It must have exactly one datalake in the environment",
                    RecoveryStatus.NON_RECOVERABLE);
        } else if (STOPPED.equals(status)) {
            if (sdxCluster.isDetached()) {
                return new SdxRecoverableResponse("Resize can recover detached cluster", RecoveryStatus.RECOVERABLE);
            } else if (statusReason != null && statusReason.contains("SDX detach failed")) {
                return new SdxRecoverableResponse("Resize can recover stopped cluster", RecoveryStatus.RECOVERABLE);
            }
        } else if (STOP_FAILED.equals(status) && statusReason != null && statusReason.contains("Datalake resize failure")) {
            return new SdxRecoverableResponse("Resize can be recovered from a failed stop", RecoveryStatus.RECOVERABLE);
        }
        return new SdxRecoverableResponse(
                "Resize can not be recovered from this point for single cluster: " + getReasonNonRecoverable(status),
                RecoveryStatus.NON_RECOVERABLE
        );
    }
}
