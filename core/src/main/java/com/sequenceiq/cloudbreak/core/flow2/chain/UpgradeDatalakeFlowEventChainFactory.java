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
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.chain.util.SetDefaultJavaVersionFlowChainService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeState;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateState;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterUpgradeTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.DataLakeUpgradeFlowChainTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackImageUpdateTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackSyncTriggerEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.cloudbreak.service.salt.SaltVersionUpgradeService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.upgrade.image.locked.LockedComponentService;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.ClusterUseCaseAware;
import com.sequenceiq.common.model.OsType;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@Component
public class UpgradeDatalakeFlowEventChainFactory implements FlowEventChainFactory<DataLakeUpgradeFlowChainTriggerEvent>, ClusterUseCaseAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeDatalakeFlowEventChainFactory.class);

    @Inject
    private LockedComponentService lockedComponentService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    @Inject
    private SaltVersionUpgradeService saltVersionUpgradeService;

    @Inject
    private SetDefaultJavaVersionFlowChainService setDefaultJavaVersionFlowChainService;

    @Override
    public String initEvent() {
        return FlowChainTriggers.DATALAKE_CLUSTER_UPGRADE_CHAIN_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(DataLakeUpgradeFlowChainTriggerEvent event) {
        LOGGER.debug("Creating flow trigger event queue for data lake upgrade with event {}", event);
        Image currentImage = getCurrentImage(event);
        ImageChangeDto imageChangeDto = getImageChangeDto(event, currentImage);
        StackDto stack = stackDtoService.getByIdWithoutResources(event.getResourceId());

        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        flowEventChain.addAll(getFullSyncEvent(event));
        flowEventChain.addAll(getClusterUpgradeValidationTriggerEvent(event, stack));
        flowEventChain.addAll(saltVersionUpgradeService.getSaltSecretRotationTriggerEvent(event.getResourceId()));
        flowEventChain.addAll(getSaltUpdateTriggerEvent(event));
        flowEventChain.addAll(getImageUpdateTriggerEvent(imageChangeDto));
        flowEventChain.addAll(setDefaultJavaVersionFlowChainService.setDefaultJavaVersionTriggerEvent(stack, imageChangeDto));
        flowEventChain.addAll(getClusterUpgradeTriggerEvent(event, currentImage));

        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }

    private List<StackEvent> getSaltUpdateTriggerEvent(DataLakeUpgradeFlowChainTriggerEvent event) {
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

    private List<Selectable> getFullSyncEvent(DataLakeUpgradeFlowChainTriggerEvent event) {
        LOGGER.info("Add sync events for full sync");
        List<Selectable> syncEvents = new ArrayList<>();

        syncEvents.add(new StackSyncTriggerEvent(STACK_SYNC_EVENT.event(), event.getResourceId(), true, event.accepted()));
        syncEvents.add(new StackEvent(CLUSTER_SYNC_EVENT.event(), event.getResourceId()));

        return syncEvents;
    }

    private List<StackImageUpdateTriggerEvent> getImageUpdateTriggerEvent(ImageChangeDto imageChangeDto) {
        return List.of(new StackImageUpdateTriggerEvent(STACK_IMAGE_UPDATE_TRIGGER_EVENT, imageChangeDto));
    }

    private ImageChangeDto getImageChangeDto(DataLakeUpgradeFlowChainTriggerEvent event, Image currentImage) {
        return new ImageChangeDto(event.getResourceId(), event.getImageId(), currentImage.getImageCatalogName(), currentImage.getImageCatalogUrl());
    }

    private List<ClusterUpgradeValidationTriggerEvent> getClusterUpgradeValidationTriggerEvent(DataLakeUpgradeFlowChainTriggerEvent event, StackDto stack) {
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

    private List<ClusterUpgradeTriggerEvent> getClusterUpgradeTriggerEvent(DataLakeUpgradeFlowChainTriggerEvent event, Image currentImage) {
        return List.of(
                new ClusterUpgradeTriggerEvent(
                        CLUSTER_UPGRADE_INIT_EVENT.event(),
                        event.getResourceId(),
                        event.accepted(),
                        event.getImageId(),
                        event.isRollingUpgradeEnabled(),
                        OsType.getByOsTypeString(currentImage.getOsType())));
    }

    private Image getCurrentImage(DataLakeUpgradeFlowChainTriggerEvent event) {
        try {
            return componentConfigProviderService.getImage(event.getResourceId());
        } catch (CloudbreakImageNotFoundException e) {
            throw new NotFoundException("Image not found for stack", e);
        }
    }
}