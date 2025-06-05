package com.sequenceiq.cloudbreak.service.upgrade.ccm;

import static com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainTriggers.UPGRADE_CCM_CHAIN_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATAHUB_UPGRADE_CCM_ALREADY_UPGRADED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATAHUB_UPGRADE_CCM_ERROR_ENVIRONMENT_IS_NOT_LATEST;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATAHUB_UPGRADE_CCM_NOT_AVAILABLE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATAHUB_UPGRADE_CCM_NOT_UPGRADEABLE;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.StackCcmUpgradeV4Response;
import com.sequenceiq.cloudbreak.api.model.CcmUpgradeResponseType;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorNotifier;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmFlowChainTriggerEvent;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class StackCcmUpgradeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackCcmUpgradeService.class);

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private StackService stackService;

    @Inject
    private ReactorNotifier reactorNotifier;

    @Inject
    private EnvironmentService environmentService;

    @Inject
    private CloudbreakMessagesService messagesService;

    public StackCcmUpgradeV4Response upgradeCcm(NameOrCrn nameOrCrn) {
        Long workspaceId = restRequestThreadLocalService.getRequestedWorkspaceId();
        Stack stack = stackService.getByNameOrCrnInWorkspace(nameOrCrn, workspaceId);
        MDCBuilder.buildMdcContext(stack);
        LOGGER.info("CCM upgrade has been initiated for stack {}", nameOrCrn.getNameOrCrn());
        checkEnvironment(stack.getEnvironmentCrn());

        if (Tunnel.getUpgradables().contains(stack.getTunnel())) {
            return checkPrerequisitesAndTrigger(stack);
        } else if (stack.getTunnel() == Tunnel.latestUpgradeTarget()) {
            return alreadyOnLatestAnswer(stack);
        } else {
            return cannotUpgradeAnswer(stack);
        }
    }

    private void checkEnvironment(String environmentCrn) {
        DetailedEnvironmentResponse environment = environmentService.getByCrn(environmentCrn);
        if (environment.getTunnel() != Tunnel.latestUpgradeTarget()) {
            LOGGER.info("Environment {} is not on the latest CCM", environmentCrn);
            throw new BadRequestException(getMessage(DATAHUB_UPGRADE_CCM_ERROR_ENVIRONMENT_IS_NOT_LATEST, List.of(environmentCrn)));
        }
    }

    private StackCcmUpgradeV4Response alreadyOnLatestAnswer(Stack stack) {
        LOGGER.info("Datahub stack {} already has TunnelType {}", stack.getName(), stack.getTunnel().name());
        return new StackCcmUpgradeV4Response(CcmUpgradeResponseType.SKIP, FlowIdentifier.notTriggered(),
                getMessage(DATAHUB_UPGRADE_CCM_ALREADY_UPGRADED), stack.getResourceCrn());
    }

    private StackCcmUpgradeV4Response cannotUpgradeAnswer(Stack stack) {
        LOGGER.info("Datahub stack {} has TunnelType {}. No CCM upgrade is possible.", stack.getName(), stack.getTunnel().name());
        return new StackCcmUpgradeV4Response(CcmUpgradeResponseType.ERROR, FlowIdentifier.notTriggered(),
                getMessage(DATAHUB_UPGRADE_CCM_NOT_UPGRADEABLE), stack.getResourceCrn());
    }

    private StackCcmUpgradeV4Response checkPrerequisitesAndTrigger(Stack stack) {
        if (!stack.getStatus().isAvailable() && Status.UPGRADE_CCM_FAILED != stack.getStatus()) {
            LOGGER.info("Datahub stack {} is not available for CCM upgrade", stack.getName());
            return new StackCcmUpgradeV4Response(CcmUpgradeResponseType.ERROR, FlowIdentifier.notTriggered(),
                    getMessage(DATAHUB_UPGRADE_CCM_NOT_AVAILABLE), stack.getResourceCrn());
        }

        LOGGER.info("Datahub stack {} has to be upgraded from TunnelType {} to {}", stack.getName(), stack.getTunnel().name(), Tunnel.latestUpgradeTarget());
        return triggerCcmUpgradeFlow(stack);
    }

    private StackCcmUpgradeV4Response triggerCcmUpgradeFlow(Stack stack) {
        Cluster cluster = stack.getCluster();
        String selector = UPGRADE_CCM_CHAIN_TRIGGER_EVENT;
        UpgradeCcmFlowChainTriggerEvent event = new UpgradeCcmFlowChainTriggerEvent(selector, stack.getId(),
                Optional.ofNullable(cluster).map(Cluster::getId).orElse(null), stack.getTunnel());
        FlowIdentifier triggeredFlow = reactorNotifier.notify(stack.getId(), selector, event);

        return new StackCcmUpgradeV4Response(CcmUpgradeResponseType.TRIGGERED, triggeredFlow, null, stack.getResourceCrn());
    }

    private String getMessage(ResourceEvent resourceEvent) {
        return messagesService.getMessage(resourceEvent.getMessage());
    }

    private String getMessage(ResourceEvent resourceEvent, List<String> args) {
        return messagesService.getMessage(resourceEvent.getMessage(), args);
    }

    public int getNotUpgradedStackCount(String envCrn) {
        MDCBuilder.addResourceCrn(envCrn);
        MDCBuilder.addEnvironmentCrn(envCrn);
        return stackService.getNotUpgradedStackCount(envCrn, Tunnel.getUpgradables());
    }
}
