package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UNSET;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UPGRADE_PREPARE_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UPGRADE_PREPARE_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UPGRADE_PREPARE_STARTED;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.ClusterUpgradePreparationState;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.event.ClusterUpgradePreparationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.ClusterUpgradeValidationState;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.UpgradePreparationChainTriggerEvent;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.cloudbreak.service.upgrade.image.CentosToRedHatUpgradeAvailabilityService;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.ClusterUseCaseAware;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@Component
public class PrepareClusterUpgradeFlowEventChainFactory implements FlowEventChainFactory<UpgradePreparationChainTriggerEvent>, ClusterUseCaseAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrepareClusterUpgradeFlowEventChainFactory.class);

    @Inject
    private CentosToRedHatUpgradeAvailabilityService centosToRedHatUpgradeAvailabilityService;

    @Override
    public String initEvent() {
        return FlowChainTriggers.CLUSTER_UPGRADE_PREPARATION_CHAIN_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(UpgradePreparationChainTriggerEvent event) {
        Optional<Image> helperImage = centosToRedHatUpgradeAvailabilityService.findHelperImageIfNecessary(
                event.getImageChangeDto().getImageId(),
                event.getResourceId());
        UpgradePreparationChainTriggerEvent upgradeTriggerEvent = helperImage.map(image ->
                replaceEventForUpgradePreparationForRhel(image, event)).orElse(event);

        LOGGER.debug("Creating flow trigger event queue for upgrade preparation with event {}", event);
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        flowEventChain.add(createUpgradeValidationTriggerEvent(upgradeTriggerEvent));
        flowEventChain.add(createClusterUpgradePreparationTriggerEvent(upgradeTriggerEvent));
        return new FlowTriggerEventQueue(getName(), upgradeTriggerEvent, flowEventChain);
    }

    @Override
    public Value getUseCaseForFlowState(Enum flowState) {
        if (ClusterUpgradeValidationState.INIT_STATE.equals(flowState)) {
            return UPGRADE_PREPARE_STARTED;
        } else if (ClusterUpgradePreparationState.CLUSTER_UPGRADE_PREPARATION_FINISHED_STATE.equals(flowState)) {
            return UPGRADE_PREPARE_FINISHED;
        } else if (flowState.toString().endsWith("FAILED_STATE")) {
            return UPGRADE_PREPARE_FAILED;
        } else {
            return UNSET;
        }
    }

    private ClusterUpgradeValidationTriggerEvent createUpgradeValidationTriggerEvent(UpgradePreparationChainTriggerEvent event) {
        return new ClusterUpgradeValidationTriggerEvent(event.getResourceId(), event.accepted(), event.getImageChangeDto().getImageId(), false, false, false);
    }

    private ClusterUpgradePreparationTriggerEvent createClusterUpgradePreparationTriggerEvent(UpgradePreparationChainTriggerEvent event) {
        return new ClusterUpgradePreparationTriggerEvent(event.getResourceId(), event.accepted(), event.getImageChangeDto(), event.getRuntimeVersion());
    }

    private UpgradePreparationChainTriggerEvent replaceEventForUpgradePreparationForRhel(Image helperImage, UpgradePreparationChainTriggerEvent event) {
        ImageChangeDto originalImageChangeDto = event.getImageChangeDto();
        LOGGER.debug("Creating new event where changing the image from RHEL8 {} to centos7 {} to perform the runtime upgrade",
                originalImageChangeDto.getImageId(),
                helperImage.getUuid());
        event.setImageChangeDto(new ImageChangeDto(
                originalImageChangeDto.getStackId(),
                helperImage.getUuid(),
                originalImageChangeDto.getImageCatalogName(),
                originalImageChangeDto.getImageCatalogUrl()));
        return event;
    }
}