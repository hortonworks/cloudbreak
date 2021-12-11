package com.sequenceiq.datalake.service.resize.recovery;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;

import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recovery.RecoveryStatus;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.flow.chain.DatalakeResizeFlowEventChainFactory;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.recovery.RecoveryService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.Flow2Handler;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.service.flowlog.FlowChainLogService;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(ResizeRecoveryService.class);

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Inject
    private Flow2Handler flow2Handler;

    @Inject
    private FlowChainLogService flowChainLogService;

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Override
    public SdxRecoverableResponse validateRecovery(SdxCluster sdxCluster, SdxRecoveryRequest request) {
        Optional<FlowLog> flowLogOptional = flow2Handler.getFirstStateLogfromLatestFlow(sdxCluster.getId());
        if (flowLogOptional.isEmpty()) {
            return new SdxRecoverableResponse("No recent actions on this cluster", RecoveryStatus.NON_RECOVERABLE);
        }
        if (entitlementService.isDatalakeResizeRecoveryEnabled(ThreadBasedUserCrnProvider.getAccountId())) {
            if (!DatalakeResizeFlowEventChainFactory.class.getSimpleName()
                    .equals(flowChainLogService.getFlowChainType(flowLogOptional.get().getFlowChainId()))) {
                return new SdxRecoverableResponse("No recent resize operation", RecoveryStatus.NON_RECOVERABLE);
            }
        } else {
            return new SdxRecoverableResponse("Resize Recovery entitlement not enabled", RecoveryStatus.NON_RECOVERABLE);
        }
        SdxStatusEntity actualStatusForSdx = sdxStatusService.getActualStatusForSdx(sdxCluster);
        switch (actualStatusForSdx.getStatus()) {
            case STOP_FAILED:
                return new SdxRecoverableResponse("Resize can be recovered from a failed stop", RecoveryStatus.RECOVERABLE);
            case STOPPED:
                return new SdxRecoverableResponse("Resize can recover cluster", RecoveryStatus.RECOVERABLE);
            case PROVISIONING_FAILED:
                return new SdxRecoverableResponse("Failed to provision, recovery will restart original data lake, and delete the new one",
                        RecoveryStatus.RECOVERABLE);
            case DELETE_FAILED:
                return new SdxRecoverableResponse("Failed to delete original data lake, not a recoverable error", RecoveryStatus.NON_RECOVERABLE);
            case RUNNING:
                if (actualStatusForSdx.getStatusReason().contains("Datalake restore failed")) {
                    return new SdxRecoverableResponse(
                            "Failed to restore backup to new data lake, recovery will restart original data lake, and delete the new one",
                            RecoveryStatus.RECOVERABLE
                    );
                } else {
                    return new SdxRecoverableResponse("Datalake is running, resize can not be recovered from this point", RecoveryStatus.NON_RECOVERABLE);
                }
            default:
                return new SdxRecoverableResponse("Resize can not be recovered from this point", RecoveryStatus.NON_RECOVERABLE);
        }
    }

    @Override
    public SdxRecoverableResponse validateRecovery(SdxCluster sdxCluster) {
        return validateRecovery(sdxCluster, null);
    }

    @Override
    public SdxRecoveryResponse triggerRecovery(SdxCluster sdxCluster, SdxRecoveryRequest sdxRecoveryRequest) {
        SdxStatusEntity actualStatusForSdx = sdxStatusService.getActualStatusForSdx(sdxCluster);
        if (entitlementService.isDatalakeResizeRecoveryEnabled(ThreadBasedUserCrnProvider.getAccountId())) {
            switch (actualStatusForSdx.getStatus()) {
                case STOP_FAILED:
                    return new SdxRecoveryResponse(sdxReactorFlowManager.triggerSdxStartFlow(sdxCluster));
                case STOPPED:
                    if (sdxCluster.isDetached()) {
                        return new SdxRecoveryResponse(sdxReactorFlowManager.triggerSdxResizeRecovery(sdxCluster, null));
                    } else {
                        return new SdxRecoveryResponse(sdxReactorFlowManager.triggerSdxStartFlow(sdxCluster));
                    }
                case PROVISIONING_FAILED:
                    return new SdxRecoveryResponse(sdxReactorFlowManager.triggerSdxResizeRecovery(getOldCluster(sdxCluster), sdxCluster));
                case RUNNING:
                    if (actualStatusForSdx.getStatusReason().contains("Datalake restore failed")) {
                        return new SdxRecoveryResponse(sdxReactorFlowManager.triggerSdxResizeRecovery(getOldCluster(sdxCluster), sdxCluster));
                    } else {
                        throw new NotImplementedException("Cluster is currently running and cannot be recovered");
                    }
                default:
                    throw new NotImplementedException("Cluster is currently in an unrecoverable state");
            }
        } else {
            throw new BadRequestException("Entitlement for resize recovery is missing");
        }
    }

    private SdxCluster getOldCluster(SdxCluster newCluster) {
        return sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(
                newCluster.getAccountId(), newCluster.getEnvCrn()
        ).orElseThrow(notFound(
                "detached SDX cluster",
                "Env CRN: " + newCluster.getEnvCrn() + ", Account ID: " + newCluster.getAccountId()
        ));
    }
}
