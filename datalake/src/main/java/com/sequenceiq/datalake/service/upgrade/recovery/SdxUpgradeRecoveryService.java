package com.sequenceiq.datalake.service.upgrade.recovery;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recovery.RecoveryStatus.NON_RECOVERABLE;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recovery.RecoveryValidationV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.exception.CloudbreakApiException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.service.FreeipaService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.sdx.api.model.SdxRecoverableResponse;
import com.sequenceiq.sdx.api.model.SdxRecoveryRequest;
import com.sequenceiq.sdx.api.model.SdxRecoveryResponse;
import com.sequenceiq.sdx.api.model.SdxRecoveryType;

@Component
public class SdxUpgradeRecoveryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxUpgradeRecoveryService.class);

    @Inject
    private SdxService sdxService;

    @Inject
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Inject
    private CloudbreakMessagesService messagesService;

    @Inject
    private FreeipaService freeipaService;

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    @Inject
    private WebApplicationExceptionMessageExtractor exceptionMessageExtractor;

    public SdxRecoveryResponse triggerRecovery(String userCrn, NameOrCrn clusterNameOrCrn, SdxRecoveryRequest recoverRequest) {
        SdxCluster cluster = sdxService.getByNameOrCrn(userCrn, clusterNameOrCrn);
        MDCBuilder.buildMdcContext(cluster);
        return initSdxRecovery(recoverRequest, cluster);
    }

    public SdxRecoverableResponse validateRecovery(String userCrn, NameOrCrn clusterNameOrCrn) {
        SdxCluster cluster = sdxService.getByNameOrCrn(userCrn, clusterNameOrCrn);
        MDCBuilder.buildMdcContext(cluster);
        return validateStackRecoverable(cluster);
    }

    private SdxRecoveryResponse initSdxRecovery(SdxRecoveryRequest request, SdxCluster cluster) {

        SdxRecoverableResponse validationResponse = validateStackRecoverable(cluster);

        if (validationResponse.getStatus() == NON_RECOVERABLE) {
            LOGGER.debug("Cluster is not in a recoverable state with message: {}", validationResponse.getReason());
            throw new BadRequestException(validationResponse.getReason());
        }
        FlowIdentifier flowIdentifier = triggerDatalakeUpgradeRecoveryFlow(request.getType(), cluster);
        return new SdxRecoveryResponse(flowIdentifier);
    }

    private SdxRecoverableResponse validateStackRecoverable(SdxCluster sdxCluster) {
        try {
            String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
            RecoveryValidationV4Response response = ThreadBasedUserCrnProvider.doAsInternalActor(() ->
                    stackV4Endpoint.getClusterRecoverableByNameInternal(0L, sdxCluster.getClusterName(), initiatorUserCrn));
            return new SdxRecoverableResponse(response.getReason(), response.getStatus());
        } catch (WebApplicationException e) {
            String exceptionMessage = exceptionMessageExtractor.getErrorMessage(e);
            String message = String.format("Stack recovery validation failed on cluster: [%s]. Message: [%s]",
                    sdxCluster.getClusterName(), exceptionMessage);
            throw new CloudbreakApiException(message, e);
        }
    }

    private FlowIdentifier triggerDatalakeUpgradeRecoveryFlow(SdxRecoveryType recoveryType, SdxCluster cluster) {
        return sdxReactorFlowManager.triggerDatalakeRuntimeRecoveryFlow(cluster, recoveryType);
    }

}
