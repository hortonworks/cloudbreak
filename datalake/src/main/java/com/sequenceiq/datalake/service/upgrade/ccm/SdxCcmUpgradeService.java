package com.sequenceiq.datalake.service.upgrade.ccm;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_CCM_UPGRADE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_CCM_UPGRADE_ALREADY_UPGRADED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_CCM_UPGRADE_ERROR_ENVIRONMENT_IS_NOT_AVAILABLE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_CCM_UPGRADE_ERROR_ENVIRONMENT_IS_NOT_LATEST;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_CCM_UPGRADE_ERROR_INVALID_COUNT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_CCM_UPGRADE_NOT_AVAILABLE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_CCM_UPGRADE_NOT_UPGRADEABLE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_CCM_UPGRADE_NO_DATALAKE;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.service.EnvironmentClientService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.sdx.api.model.SdxCcmUpgradeResponse;

@Component
public class SdxCcmUpgradeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxCcmUpgradeService.class);

    @Inject
    private EnvironmentClientService environmentService;

    @Inject
    private SdxService sdxService;

    @Inject
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Inject
    private CloudbreakMessagesService messagesService;

    public SdxCcmUpgradeResponse upgradeCcm(String environmentCrn) {
        checkEnvironment(environmentCrn);
        Optional<SdxCluster> sdxClusterOpt = getSdxCluster(environmentCrn);
        if (sdxClusterOpt.isEmpty()) {
            LOGGER.debug("Environment {} has no datalake", environmentCrn);
            return new SdxCcmUpgradeResponse(getMessage(DATALAKE_CCM_UPGRADE_NO_DATALAKE, List.of(environmentCrn)), FlowIdentifier.notTriggered());
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

    private void checkEnvironment(String environmentCrn) {
        DetailedEnvironmentResponse environment = environmentService.getByCrn(environmentCrn);
        if (environment.getTunnel() != Tunnel.latestUpgradeTarget()) {
            LOGGER.debug("Environment {} is not on the latest CCM", environmentCrn);
            throw new BadRequestException(getMessage(DATALAKE_CCM_UPGRADE_ERROR_ENVIRONMENT_IS_NOT_LATEST, List.of(environmentCrn)));
        }

        if (environment.getEnvironmentStatus() != EnvironmentStatus.AVAILABLE) {
            LOGGER.debug("Environment {} is no Available", environmentCrn);
            throw new BadRequestException(getMessage(DATALAKE_CCM_UPGRADE_ERROR_ENVIRONMENT_IS_NOT_AVAILABLE, List.of(environmentCrn)));
        }
    }

    private Optional<SdxCluster> getSdxCluster(String environmentCrn) {
        List<SdxCluster> clusters = sdxService.listSdxByEnvCrn(environmentCrn);
        if (clusters.size() > 1) {
            LOGGER.debug("Environment {} has more than 1 datalake", environmentCrn);
            throw new BadRequestException(getMessage(DATALAKE_CCM_UPGRADE_ERROR_INVALID_COUNT, List.of(environmentCrn)));
        }
        if (clusters.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(clusters.get(0));
    }

    private SdxCcmUpgradeResponse checkPrerequisitesAndTrigger(SdxCluster sdxCluster, StackV4Response stack) {
        if (!stack.getStatus().isAvailable()) {
            LOGGER.debug("Datalake stack {} is not available for CCM upgrade", stack.getName());
            return new SdxCcmUpgradeResponse(getMessage(DATALAKE_CCM_UPGRADE_NOT_AVAILABLE), FlowIdentifier.notTriggered());
        }

        LOGGER.debug("Datalake stack {} has to be upgraded from TunnelType {} to {}", stack.getName(), stack.getTunnel().name(), Tunnel.latestUpgradeTarget());
        return triggerCcmUpgradeFlow(sdxCluster);
    }

    private SdxCcmUpgradeResponse alreadyOnLatestAnswer(StackV4Response stack) {
        LOGGER.debug("Datalake stack {} already has TunnelType {}", stack.getName(), stack.getTunnel().name());
        return new SdxCcmUpgradeResponse(getMessage(DATALAKE_CCM_UPGRADE_ALREADY_UPGRADED), FlowIdentifier.notTriggered());
    }

    private SdxCcmUpgradeResponse cannotUpgradeAnswer(StackV4Response stack) {
        LOGGER.debug("Datalake stack {} has TunnelType {}. No CCM upgrade is possible.", stack.getName(), stack.getTunnel().name());
        return new SdxCcmUpgradeResponse(getMessage(DATALAKE_CCM_UPGRADE_NOT_UPGRADEABLE), FlowIdentifier.notTriggered());
    }

    private SdxCcmUpgradeResponse triggerCcmUpgradeFlow(SdxCluster cluster) {
        MDCBuilder.buildMdcContext(cluster);
        FlowIdentifier flowIdentifier = sdxReactorFlowManager.triggerCcmUpgradeFlow(cluster);
        return new SdxCcmUpgradeResponse(getMessage(DATALAKE_CCM_UPGRADE, null), flowIdentifier);
    }

    private String getMessage(ResourceEvent resourceEvent) {
        return messagesService.getMessage(resourceEvent.getMessage());
    }

    private String getMessage(ResourceEvent resourceEvent, List<String> args) {
        return messagesService.getMessage(resourceEvent.getMessage(), args);
    }
}
