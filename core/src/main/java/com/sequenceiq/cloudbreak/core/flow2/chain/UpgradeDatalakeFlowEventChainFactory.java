package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UNSET;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UPGRADE_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UPGRADE_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UPGRADE_STARTED;
import static com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainTriggers.STACK_IMAGE_UPDATE_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeEvent.CLUSTER_UPGRADE_INIT_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.sync.ClusterSyncEvent.CLUSTER_SYNC_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncEvent.STACK_SYNC_EVENT;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeState;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateState;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterUpgradeTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackImageUpdateTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackSyncTriggerEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.cloudbreak.service.salt.SaltVersionUpgradeService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.upgrade.image.OsChangeUtil;
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
    private ComponentConfigProviderService componentConfigProviderService;

    @Inject
    private OsChangeUtil osChangeUtil;

    @Inject
    private SaltVersionUpgradeService saltVersionUpgradeService;

    @Override
    public String initEvent() {
        return FlowChainTriggers.DATALAKE_CLUSTER_UPGRADE_CHAIN_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(ClusterUpgradeTriggerEvent event) {
        LOGGER.debug("Creating flow trigger event queue for data lake upgrade with event {}", event);
        ClusterUpgradeTriggerEvent upgradeTriggerEvent = getEventForRuntimeUpgrade(event);

        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();

        flowEventChain.addAll(getFullSyncEvent(upgradeTriggerEvent));
        flowEventChain.addAll(getClusterUpgradeValidationTriggerEvent(upgradeTriggerEvent));
        flowEventChain.addAll(saltVersionUpgradeService.getSaltSecretRotationTriggerEvent(event.getResourceId()));
        flowEventChain.addAll(getSaltUpdateTriggerEvent(upgradeTriggerEvent));
        flowEventChain.addAll(getImageUpdateTriggerEvent(upgradeTriggerEvent));
        flowEventChain.addAll(getClusterUpgradeTriggerEvent(upgradeTriggerEvent));

        return new FlowTriggerEventQueue(getName(), upgradeTriggerEvent, flowEventChain);
    }

    private List<StackEvent> getSaltUpdateTriggerEvent(ClusterUpgradeTriggerEvent event) {
        return List.of(new StackEvent(SaltUpdateEvent.SALT_UPDATE_EVENT.event(), event.getResourceId(), event.accepted()));
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

    private ClusterUpgradeTriggerEvent getEventForRuntimeUpgrade(ClusterUpgradeTriggerEvent event) {
        return osChangeUtil
                .findHelperImageIfNecessary(event.getImageId(), event.getResourceId())
                .map(image -> createEventForRuntimeUpgrade(image, event))
                .orElse(event);
    }

    private List<Selectable> getFullSyncEvent(ClusterUpgradeTriggerEvent event) {
        LOGGER.info("Add sync events for full sync");
        List<Selectable> syncEvents = new ArrayList<>();

        syncEvents.add(new StackSyncTriggerEvent(STACK_SYNC_EVENT.event(), event.getResourceId(), true, event.accepted()));
        syncEvents.add(new StackEvent(CLUSTER_SYNC_EVENT.event(), event.getResourceId()));

        return syncEvents;
    }

    private com.sequenceiq.cloudbreak.cloud.model.Image findImage(Long stackId) {
        try {
            return componentConfigProviderService.getImage(stackId);
        } catch (CloudbreakImageNotFoundException e) {
            throw new NotFoundException("Image not found for stack", e);
        }
    }

    private List<StackImageUpdateTriggerEvent> getImageUpdateTriggerEvent(ClusterUpgradeTriggerEvent event) {
        com.sequenceiq.cloudbreak.cloud.model.Image image = findImage(event.getResourceId());
        ImageChangeDto imageChangeDto = new ImageChangeDto(event.getResourceId(), event.getImageId(), image.getImageCatalogName(), image.getImageCatalogUrl());
        return List.of(new StackImageUpdateTriggerEvent(STACK_IMAGE_UPDATE_TRIGGER_EVENT, imageChangeDto));
    }

    private List<ClusterUpgradeValidationTriggerEvent> getClusterUpgradeValidationTriggerEvent(ClusterUpgradeTriggerEvent event) {
        StackDto stack = stackDtoService.getById(event.getResourceId());
        boolean lockComponents = lockedComponentService.isComponentsLocked(stack, event.getImageId());
        return List.of(
                new ClusterUpgradeValidationTriggerEvent(
                        event.getResourceId(),
                        event.accepted(),
                        event.getImageId(),
                        lockComponents,
                        event.isRollingUpgradeEnabled(),
                        true)
        );
    }

    private List<ClusterUpgradeTriggerEvent> getClusterUpgradeTriggerEvent(ClusterUpgradeTriggerEvent event) {
        return List.of(
                new ClusterUpgradeTriggerEvent(
                        CLUSTER_UPGRADE_INIT_EVENT.event(),
                        event.getResourceId(),
                        event.accepted(),
                        event.getImageId(),
                        event.isRollingUpgradeEnabled()
                )
        );
    }

    private ClusterUpgradeTriggerEvent createEventForRuntimeUpgrade(Image helperImage, ClusterUpgradeTriggerEvent event) {
        LOGGER.debug("Creating new event where changing the image from RHEL8 {} to centos7 {} for perform the runtime upgrade", event.getImageId(),
                helperImage.getUuid());
        return new ClusterUpgradeTriggerEvent(event.getSelector(), event.getResourceId(), event.accepted(), helperImage.getUuid(),
                event.isRollingUpgradeEnabled());
    }
}