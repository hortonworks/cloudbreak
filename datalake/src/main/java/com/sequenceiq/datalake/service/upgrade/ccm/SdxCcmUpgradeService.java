package com.sequenceiq.datalake.service.upgrade.ccm;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_UPGRADE_CCM;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_UPGRADE_CCM_ALREADY_UPGRADED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_UPGRADE_CCM_ERROR_ENVIRONMENT_IS_NOT_LATEST;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_UPGRADE_CCM_ERROR_INVALID_COUNT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_UPGRADE_CCM_NOT_AVAILABLE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_UPGRADE_CCM_NOT_UPGRADEABLE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_UPGRADE_CCM_NO_DATALAKE;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.StackCcmUpgradeV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.service.sdx.CloudbreakPoller;
import com.sequenceiq.datalake.service.sdx.EnvironmentService;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.sdx.api.model.CcmUpgradeResponseType;
import com.sequenceiq.sdx.api.model.SdxCcmUpgradeResponse;

@Component
public class SdxCcmUpgradeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxCcmUpgradeService.class);

    @Inject
    private EnvironmentService environmentService;

    @Inject
    private SdxService sdxService;

    @Inject
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Inject
    private CloudbreakMessagesService messagesService;

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    @Inject
    private CloudbreakPoller cloudbreakPoller;

    @Inject
    private CloudbreakFlowService cloudbreakFlowService;

    public SdxCcmUpgradeResponse upgradeCcm(String environmentCrn) {
        checkEnvironment(environmentCrn);
        Optional<SdxCluster> sdxClusterOpt = getSdxCluster(environmentCrn);
        if (sdxClusterOpt.isEmpty()) {
            return noDatalakeAnswer(environmentCrn);
        }
        SdxCluster sdxCluster = sdxClusterOpt.get();
        StackV4Response stack = sdxService.getDetail(sdxCluster.getClusterName(), null, sdxService.getAccountIdFromCrn(environmentCrn));

        if (Tunnel.getUpgradables().contains(stack.getTunnel())) {
            return checkPrerequisitesAndTrigger(sdxCluster, stack);
        } else if (stack.getTunnel() == Tunnel.latestUpgradeTarget()) {
            return alreadyOnLatestAnswer(stack);
        } else {
            return cannotUpgradeAnswer(stack);
        }
    }

    public void initAndWaitForStackUpgrade(SdxCluster sdxCluster, PollingConfig pollingConfig) {
        String stackCrn = sdxCluster.getStackCrn();
        LOGGER.debug("Initiating CCM upgrade on stack CRN {} for datalake {}", stackCrn, sdxCluster.getName());
        String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        StackCcmUpgradeV4Response upgradeResponse =
                ThreadBasedUserCrnProvider.doAsInternalActor(() -> stackV4Endpoint.upgradeCcmByCrnInternal(0L, stackCrn, initiatorUserCrn));
        cloudbreakFlowService.saveLastCloudbreakFlowChainId(sdxCluster, upgradeResponse.getFlowIdentifier());
        LOGGER.debug("Waiting for CCM upgrade on stack CRN {} for datalake {}", stackCrn, sdxCluster.getName());
        cloudbreakPoller.pollCcmUpgradeUntilAvailable(sdxCluster, pollingConfig);
    }

    private void checkEnvironment(String environmentCrn) {
        DetailedEnvironmentResponse environment = environmentService.getByCrn(environmentCrn);
        if (environment.getTunnel() != Tunnel.latestUpgradeTarget()) {
            LOGGER.debug("Environment {} is not on the latest CCM", environmentCrn);
            throw new BadRequestException(getMessage(DATALAKE_UPGRADE_CCM_ERROR_ENVIRONMENT_IS_NOT_LATEST, List.of(environmentCrn)));
        }
    }

    private Optional<SdxCluster> getSdxCluster(String environmentCrn) {
        List<SdxCluster> clusters = sdxService.listSdxByEnvCrn(environmentCrn);
        if (clusters.size() > 1) {
            LOGGER.debug("Environment {} has more than 1 datalake", environmentCrn);
            throw new BadRequestException(getMessage(DATALAKE_UPGRADE_CCM_ERROR_INVALID_COUNT, List.of(environmentCrn)));
        }
        if (clusters.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(clusters.get(0));
    }

    private SdxCcmUpgradeResponse checkPrerequisitesAndTrigger(SdxCluster sdxCluster, StackV4Response stack) {
        if (!stack.getStatus().isAvailable() && Status.UPGRADE_CCM_FAILED != stack.getStatus()) {
            LOGGER.debug("Datalake stack {} is not available for CCM upgrade", stack.getName());
            return new SdxCcmUpgradeResponse(CcmUpgradeResponseType.ERROR, FlowIdentifier.notTriggered(),
                    getMessage(DATALAKE_UPGRADE_CCM_NOT_AVAILABLE), stack.getCrn());
        }

        LOGGER.debug("Datalake stack {} has to be upgraded from TunnelType {} to {}", stack.getName(), stack.getTunnel().name(), Tunnel.latestUpgradeTarget());
        return triggerCcmUpgradeFlow(sdxCluster);
    }

    private SdxCcmUpgradeResponse noDatalakeAnswer(String environmentCrn) {
        LOGGER.debug("Environment {} has no datalake", environmentCrn);
        return new SdxCcmUpgradeResponse(CcmUpgradeResponseType.SKIP, FlowIdentifier.notTriggered(),
                getMessage(DATALAKE_UPGRADE_CCM_NO_DATALAKE, List.of(environmentCrn)), null);
    }

    private SdxCcmUpgradeResponse alreadyOnLatestAnswer(StackV4Response stack) {
        LOGGER.debug("Datalake stack {} already has TunnelType {}", stack.getName(), stack.getTunnel().name());
        return new SdxCcmUpgradeResponse(CcmUpgradeResponseType.SKIP, FlowIdentifier.notTriggered(),
                getMessage(DATALAKE_UPGRADE_CCM_ALREADY_UPGRADED), stack.getCrn());
    }

    private SdxCcmUpgradeResponse cannotUpgradeAnswer(StackV4Response stack) {
        LOGGER.debug("Datalake stack {} has TunnelType {}. No CCM upgrade is possible.", stack.getName(), stack.getTunnel().name());
        return new SdxCcmUpgradeResponse(CcmUpgradeResponseType.ERROR, FlowIdentifier.notTriggered(),
                getMessage(DATALAKE_UPGRADE_CCM_NOT_UPGRADEABLE), stack.getCrn());
    }

    private SdxCcmUpgradeResponse triggerCcmUpgradeFlow(SdxCluster cluster) {
        MDCBuilder.buildMdcContext(cluster);
        FlowIdentifier flowIdentifier = sdxReactorFlowManager.triggerCcmUpgradeFlow(cluster);
        return new SdxCcmUpgradeResponse(CcmUpgradeResponseType.TRIGGERED, flowIdentifier,
                getMessage(DATALAKE_UPGRADE_CCM, null), cluster.getResourceCrn());
    }

    private String getMessage(ResourceEvent resourceEvent) {
        return messagesService.getMessage(resourceEvent.getMessage());
    }

    private String getMessage(ResourceEvent resourceEvent, List<String> args) {
        return messagesService.getMessage(resourceEvent.getMessage(), args);
    }
}
