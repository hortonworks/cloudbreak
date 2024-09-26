package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UNSET;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UPGRADE_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UPGRADE_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UPGRADE_STARTED;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeEvent.CLUSTER_UPGRADE_INIT_EVENT;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeState;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateState;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterUpgradeTriggerEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.upgrade.image.CentosToRedHatUpgradeAvailabilityService;
import com.sequenceiq.cloudbreak.service.upgrade.image.locked.LockedComponentService;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.ClusterUseCaseAware;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@Component
public class UpgradeDatalakeFlowEventChainFactory implements FlowEventChainFactory<ClusterUpgradeTriggerEvent>, ClusterUseCaseAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeDatalakeFlowEventChainFactory.class);

    @Inject
    private LockedComponentService lockedComponentService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private CentosToRedHatUpgradeAvailabilityService centOSToRedHatUpgradeAvailabilityService;

    @Override
    public String initEvent() {
        return FlowChainTriggers.DATALAKE_CLUSTER_UPGRADE_CHAIN_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(ClusterUpgradeTriggerEvent event) {
        Optional<Image> helperImage = centOSToRedHatUpgradeAvailabilityService.findHelperImageIfNecessary(event.getImageId(), event.getResourceId());
        ClusterUpgradeTriggerEvent upgradeTriggerEvent = helperImage.map(image -> createEventForRuntimeUpgrade(image, event)).orElse(event);

        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        addClusterUpgradePreparationTriggerEvent(upgradeTriggerEvent).ifPresent(flowEventChain::add);
        addSaltUpdateTriggerEvent(upgradeTriggerEvent).ifPresent(flowEventChain::add);
        addClusterUpgradeTriggerEvent(upgradeTriggerEvent).ifPresent(flowEventChain::add);
        return new FlowTriggerEventQueue(getName(), upgradeTriggerEvent, flowEventChain);
    }

    private Optional<StackEvent> addSaltUpdateTriggerEvent(ClusterUpgradeTriggerEvent event) {
        return Optional.of(new StackEvent(SaltUpdateEvent.SALT_UPDATE_EVENT.event(), event.getResourceId(), event.accepted()));
    }

    @Override
    public CDPClusterStatus.Value getUseCaseForFlowState(Enum flowState) {
        if (SaltUpdateState.INIT_STATE.equals(flowState)) {
            return UPGRADE_STARTED;
        } else if (ClusterUpgradeState.CLUSTER_UPGRADE_FINISHED_STATE.equals(flowState)) {
            return UPGRADE_FINISHED;
        } else if (flowState.toString().endsWith("FAILED_STATE")) {
            return UPGRADE_FAILED;
        } else {
            return UNSET;
        }
    }

    private Optional<ClusterUpgradeValidationTriggerEvent> addClusterUpgradePreparationTriggerEvent(ClusterUpgradeTriggerEvent event) {
        StackDto stack = stackDtoService.getById(event.getResourceId());
        boolean lockComponents = lockedComponentService.isComponentsLocked(stack, event.getImageId());
        return Optional.of(new ClusterUpgradeValidationTriggerEvent(event.getResourceId(), event.accepted(), event.getImageId(), lockComponents,
                event.isRollingUpgradeEnabled(), true));
    }

    private Optional<ClusterUpgradeTriggerEvent> addClusterUpgradeTriggerEvent(ClusterUpgradeTriggerEvent event) {
        return Optional.of(new ClusterUpgradeTriggerEvent(CLUSTER_UPGRADE_INIT_EVENT.event(), event.getResourceId(), event.accepted(),
                event.getImageId(), event.isRollingUpgradeEnabled()));
    }

    private ClusterUpgradeTriggerEvent createEventForRuntimeUpgrade(Image helperImage, ClusterUpgradeTriggerEvent event) {
        LOGGER.debug("Creating new event where changing the image from RHEL8 {} to centos7 {} for perform the runtime upgrade", event.getImageId(),
                helperImage.getUuid());
        return new ClusterUpgradeTriggerEvent(event.getSelector(), event.getResourceId(), event.accepted(), helperImage.getUuid(),
                event.isRollingUpgradeEnabled());
    }
}