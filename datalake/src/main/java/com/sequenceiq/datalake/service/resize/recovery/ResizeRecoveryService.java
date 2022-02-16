package com.sequenceiq.datalake.service.resize.recovery;

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

    public SdxRecoverableResponse validateRecovery(SdxCluster sdxCluster) {
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
            case PROVISIONING_FAILED:
                return new SdxRecoverableResponse("Failed to provision, recovery will restart original data lake, and delete the new one",
                        RecoveryStatus.RECOVERABLE);
            case DATALAKE_RESTORE_FAILED:
                return new SdxRecoverableResponse("Failed to restore backup to new data lake, recovery will restart original data lake, and delete the new one",
                        RecoveryStatus.RECOVERABLE);
            case DELETE_FAILED:
                return new SdxRecoverableResponse("Failed to delete original data lake, not a recoverable error", RecoveryStatus.NON_RECOVERABLE);

            default:
                return new SdxRecoverableResponse("Resize can not be recovered from this point", RecoveryStatus.NON_RECOVERABLE);
        }
    }

    public SdxRecoveryResponse triggerRecovery(SdxCluster sdxCluster, SdxRecoveryRequest sdxRecoveryRequest) {
        SdxStatusEntity actualStatusForSdx = sdxStatusService.getActualStatusForSdx(sdxCluster);
        if (entitlementService.isDatalakeResizeRecoveryEnabled(ThreadBasedUserCrnProvider.getAccountId())) {
            switch (actualStatusForSdx.getStatus()) {
                case STOP_FAILED:
                    return new SdxRecoveryResponse(sdxReactorFlowManager.triggerSdxStartFlow(sdxCluster));
                case PROVISIONING_FAILED:
                case DATALAKE_RESTORE_FAILED:
                default:
                    throw new NotImplementedException("Cluster is currently in an unrecoverable state");
            }
        } else {
            throw new BadRequestException("Entitlement for resize recovery is missing");
        }

    }

}
