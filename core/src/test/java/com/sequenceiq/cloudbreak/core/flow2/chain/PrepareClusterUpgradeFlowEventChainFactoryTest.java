package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UNSET;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UPGRADE_PREPARE_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UPGRADE_PREPARE_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UPGRADE_PREPARE_STARTED;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.ClusterUpgradePreparationStateSelectors.START_CLUSTER_UPGRADE_PREPARATION_INIT_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.START_CLUSTER_UPGRADE_VALIDATION_INIT_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.sync.ClusterSyncEvent.CLUSTER_SYNC_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncEvent.STACK_SYNC_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.ClusterUpgradePreparationState;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.event.ClusterUpgradePreparationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.ClusterUpgradeValidationState;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.UpgradePreparationChainTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@ExtendWith(MockitoExtension.class)
class PrepareClusterUpgradeFlowEventChainFactoryTest {

    private static final long STACK_ID = 1L;

    private static final String IMAGE_ID = "imageId";

    @InjectMocks
    private PrepareClusterUpgradeFlowEventChainFactory underTest;

    @Test
    void initEventShouldReturnCorrectValue() {
        String result = underTest.initEvent();
        assertEquals("CLUSTER_UPGRADE_PREPARATION_CHAIN_TRIGGER_EVENT", result);
    }

    @Test
    void createFlowTriggerEventQueueShouldReturnCorrectQueue() {
        UpgradePreparationChainTriggerEvent event = createEvent();

        FlowTriggerEventQueue flowChainQueue = underTest.createFlowTriggerEventQueue(event);
        assertEquals(4, flowChainQueue.getQueue().size());

        assertSyncEvents(flowChainQueue);
        assertUpgradeValidationEvent(flowChainQueue, IMAGE_ID);
        assertUpdatePreparationEvent(flowChainQueue, IMAGE_ID);

    }

    private void assertSyncEvents(FlowTriggerEventQueue flowChainQueue) {
        Selectable syncStackEvent = flowChainQueue.getQueue().remove();
        assertEquals(STACK_SYNC_EVENT.event(), syncStackEvent.selector());
        assertEquals(STACK_ID, syncStackEvent.getResourceId());
        assertInstanceOf(StackEvent.class, syncStackEvent);

        Selectable syncClusterEvent = flowChainQueue.getQueue().remove();
        assertEquals(CLUSTER_SYNC_EVENT.event(), syncClusterEvent.selector());
        assertEquals(STACK_ID, syncClusterEvent.getResourceId());
        assertInstanceOf(StackEvent.class, syncClusterEvent);
    }

    private void assertUpgradeValidationEvent(FlowTriggerEventQueue flowChainQueue, String imageId) {
        Selectable upgradeValidationEvent = flowChainQueue.getQueue().remove();
        assertEquals(START_CLUSTER_UPGRADE_VALIDATION_INIT_EVENT.event(), upgradeValidationEvent.selector());
        assertEquals(STACK_ID, upgradeValidationEvent.getResourceId());

        assertInstanceOf(ClusterUpgradeValidationTriggerEvent.class, upgradeValidationEvent);
        assertEquals(imageId, ((ClusterUpgradeValidationTriggerEvent) upgradeValidationEvent).getImageId());
    }

    private void assertUpdatePreparationEvent(FlowTriggerEventQueue flowChainQueue, String imageId) {
        Selectable upgradePreparationEvent = flowChainQueue.getQueue().remove();
        assertEquals(START_CLUSTER_UPGRADE_PREPARATION_INIT_EVENT.event(), upgradePreparationEvent.selector());
        assertEquals(STACK_ID, upgradePreparationEvent.getResourceId());

        assertInstanceOf(ClusterUpgradePreparationTriggerEvent.class, upgradePreparationEvent);
        assertEquals(imageId, ((ClusterUpgradePreparationTriggerEvent) upgradePreparationEvent).getImageChangeDto().getImageId());
    }

    @Test
    void getUseCaseForFlowStateShouldReturnCorrectValue() {
        UsageProto.CDPClusterStatus.Value result = underTest.getUseCaseForFlowState(ClusterUpgradeValidationState.INIT_STATE);
        assertEquals(UPGRADE_PREPARE_STARTED, result);

        result = underTest.getUseCaseForFlowState(ClusterUpgradePreparationState.CLUSTER_UPGRADE_PREPARATION_FINISHED_STATE);
        assertEquals(UPGRADE_PREPARE_FINISHED, result);

        result = underTest.getUseCaseForFlowState(ClusterUpgradePreparationState.CLUSTER_UPGRADE_PREPARATION_FAILED_STATE);
        assertEquals(UPGRADE_PREPARE_FAILED, result);

        result = underTest.getUseCaseForFlowState(ClusterUpgradePreparationState.CLUSTER_UPGRADE_PREPARATION_PARCEL_DISTRIBUTION_STATE);
        assertEquals(UNSET, result);
    }

    private UpgradePreparationChainTriggerEvent createEvent() {
        ImageChangeDto imageChangeDto = new ImageChangeDto(STACK_ID, IMAGE_ID, "catalogName", "catalogUrl");
        return new UpgradePreparationChainTriggerEvent("resourceId", STACK_ID, imageChangeDto, "runtimeVersion");
    }
}