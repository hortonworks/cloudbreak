package com.sequenceiq.datalake.service.upgrade.recovery;

import java.util.Objects;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.datalakedr.datalakeDRProto;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recovery.RecoveryStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recovery.RecoveryValidationV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.exception.CloudbreakApiException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.service.recovery.RecoveryService;
import com.sequenceiq.datalake.service.sdx.dr.SdxBackupRestoreService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.sdx.api.model.SdxRecoverableResponse;
import com.sequenceiq.sdx.api.model.SdxRecoveryRequest;
import com.sequenceiq.sdx.api.model.SdxRecoveryResponse;
import com.sequenceiq.sdx.api.model.SdxRecoveryType;

@Component
public class SdxUpgradeRecoveryService implements RecoveryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxUpgradeRecoveryService.class);

    @Inject
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    @Inject
    private WebApplicationExceptionMessageExtractor exceptionMessageExtractor;

    @Inject
    private SdxBackupRestoreService backupRestoreService;

    @Inject
    private EntitlementService entitlementService;

    @Override
    public SdxRecoveryResponse triggerRecovery(SdxCluster sdxCluster, SdxRecoveryRequest recoverRequest) {
        MDCBuilder.buildMdcContext(sdxCluster);

        FlowIdentifier flowIdentifier = sdxReactorFlowManager.triggerDatalakeRuntimeRecoveryFlow(sdxCluster, recoverRequest.getType());
        return new SdxRecoveryResponse(flowIdentifier);
    }

    @Override
    public SdxRecoverableResponse validateRecovery(SdxCluster sdxCluster, SdxRecoveryRequest request) {
        MDCBuilder.buildMdcContext(sdxCluster);
        try {
            String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
            if (Objects.nonNull(request) && request.getType() == SdxRecoveryType.RECOVER_WITH_DATA) {
                String clusterRuntime = sdxCluster.getRuntime();
                Optional<datalakeDRProto.DatalakeBackupInfo> lastSuccessfulBackup = backupRestoreService.getLastSuccessfulBackupInfoWithRuntime(
                        sdxCluster.getClusterName(), initiatorUserCrn, clusterRuntime
                );
                if (lastSuccessfulBackup.isEmpty()) {
                    return new SdxRecoverableResponse("There is no successful backup taken yet for data lake cluster with runtime " + clusterRuntime + ".",
                            RecoveryStatus.NON_RECOVERABLE);
                }
            }
            RecoveryValidationV4Response response = ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> stackV4Endpoint.getClusterRecoverableByNameInternal(0L, sdxCluster.getClusterName(), initiatorUserCrn));
            if (response.getStatus().recoverable() && !entitlementService.isUpgradeRecoveryEnabled(sdxCluster.getAccountId())) {
                return new SdxRecoverableResponse("Missing CDP_CB_UPGRADE_RECOVERY entitlement. Please contact support.", RecoveryStatus.NON_RECOVERABLE);
            } else {
                return new SdxRecoverableResponse(response.getReason(), response.getStatus());
            }
        } catch (WebApplicationException e) {
            String exceptionMessage = exceptionMessageExtractor.getErrorMessage(e);
            String message = String.format("Stack recovery validation failed on cluster: [%s]. Message: [%s]",
                    sdxCluster.getClusterName(), exceptionMessage);
            throw new CloudbreakApiException(message, e);
        }
    }

    @Override
    public SdxRecoverableResponse validateRecovery(SdxCluster sdxCluster) {
        return validateRecovery(sdxCluster, null);
    }
}
