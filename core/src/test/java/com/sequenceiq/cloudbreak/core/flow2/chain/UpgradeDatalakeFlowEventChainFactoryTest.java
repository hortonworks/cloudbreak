package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainTriggers.DATALAKE_CLUSTER_UPGRADE_CHAIN_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeEvent.CLUSTER_UPGRADE_INIT_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.START_CLUSTER_UPGRADE_VALIDATION_INIT_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateEvent.SALT_UPDATE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.generator.FlowOfflineStateGraphGenerator.FLOW_CONFIGS_PACKAGE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncEvent.STACK_SYNC_EVENT;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.SALT_MASTER_KEY_PAIR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.sync.ClusterSyncEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterUpgradeTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackSyncTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.rotation.flow.chain.SecretRotationFlowChainTriggerEvent;
import com.sequenceiq.cloudbreak.service.salt.SaltVersionUpgradeService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.upgrade.image.CentosToRedHatUpgradeAvailabilityService;
import com.sequenceiq.cloudbreak.service.upgrade.image.locked.LockedComponentService;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.graph.FlowChainConfigGraphGeneratorUtil;

@ExtendWith(MockitoExtension.class)
class UpgradeDatalakeFlowEventChainFactoryTest {

    private static final String IMAGE_ID = "imageId";

    private static final long STACK_ID = 1L;

    @InjectMocks
    private UpgradeDatalakeFlowEventChainFactory underTest;

    @Mock
    private LockedComponentService lockedComponentService;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private CentosToRedHatUpgradeAvailabilityService centOSToRedHatUpgradeAvailabilityService;

    @Mock
    private SaltVersionUpgradeService saltVersionUpgradeService;

    @Test
    void testInitEvent() {
        assertEquals(DATALAKE_CLUSTER_UPGRADE_CHAIN_TRIGGER_EVENT, underTest.initEvent());
    }

    @Test
    void testCreateFlowTriggerEventQueue() {
        when(centOSToRedHatUpgradeAvailabilityService.findHelperImageIfNecessary(IMAGE_ID, STACK_ID)).thenReturn(Optional.empty());
        String secretRotationSelector = EventSelectorUtil.selector(SecretRotationFlowChainTriggerEvent.class);
        when(saltVersionUpgradeService.getSaltSecretRotationTriggerEvent(1L))
                .thenReturn(List.of(
                        new SecretRotationFlowChainTriggerEvent(secretRotationSelector, 1L, null, List.of(SALT_MASTER_KEY_PAIR), null, null)));

        FlowTriggerEventQueue flowTriggerQueue = underTest.createFlowTriggerEventQueue(
                new ClusterUpgradeTriggerEvent(DATALAKE_CLUSTER_UPGRADE_CHAIN_TRIGGER_EVENT, STACK_ID, IMAGE_ID, true));

        assertEquals(6, flowTriggerQueue.getQueue().size());
        Queue<Selectable> restrainedQueueData = new ConcurrentLinkedDeque<>(flowTriggerQueue.getQueue());
        assertSyncTriggerEvent(flowTriggerQueue);
        assertClusterSyncTriggerEvent(flowTriggerQueue);
        assertClusterUpgradeValidationTriggerEvent(flowTriggerQueue);
        assertSaltSecretRotationTriggerEvent(flowTriggerQueue);
        assertSaltUpdateTriggerEvent(flowTriggerQueue);
        assertClusterUpgradeTriggerEvent(flowTriggerQueue);
        flowTriggerQueue.getQueue().addAll(restrainedQueueData);
        FlowChainConfigGraphGeneratorUtil.generateFor(underTest, FLOW_CONFIGS_PACKAGE, flowTriggerQueue);
    }

    private void assertClusterSyncTriggerEvent(FlowTriggerEventQueue flowTriggerEventQueue) {
        Selectable event = flowTriggerEventQueue.getQueue().remove();
        assertEquals(ClusterSyncEvent.CLUSTER_SYNC_EVENT.event(), event.getSelector());
        assertEquals(STACK_ID, event.getResourceId());
        assertInstanceOf(StackEvent.class, event);
    }

    private void assertSyncTriggerEvent(FlowTriggerEventQueue flowTriggerEventQueue) {
        Selectable event = flowTriggerEventQueue.getQueue().remove();
        assertEquals(STACK_SYNC_EVENT.event(), event.getSelector());
        assertEquals(STACK_ID, event.getResourceId());
        assertInstanceOf(StackSyncTriggerEvent.class, event);
    }

    private void assertClusterUpgradeValidationTriggerEvent(FlowTriggerEventQueue flowTriggerEventQueue) {
        Selectable event = flowTriggerEventQueue.getQueue().remove();
        assertEquals(START_CLUSTER_UPGRADE_VALIDATION_INIT_EVENT.event(), event.getSelector());
        assertEquals(STACK_ID, event.getResourceId());
        assertInstanceOf(ClusterUpgradeValidationTriggerEvent.class, event);
        ClusterUpgradeValidationTriggerEvent triggerEvent = (ClusterUpgradeValidationTriggerEvent) event;
        assertEquals(IMAGE_ID, triggerEvent.getImageId());
    }

    private void assertSaltSecretRotationTriggerEvent(FlowTriggerEventQueue flowChainQueue) {
        Selectable saltSecretRotationEvent = flowChainQueue.getQueue().remove();
        assertInstanceOf(SecretRotationFlowChainTriggerEvent.class, saltSecretRotationEvent);
        SecretRotationFlowChainTriggerEvent secretRotationFlowChainTriggerEvent = (SecretRotationFlowChainTriggerEvent) saltSecretRotationEvent;
        assertThat(secretRotationFlowChainTriggerEvent.getSecretTypes()).containsExactlyInAnyOrder(SALT_MASTER_KEY_PAIR);
    }

    private void assertSaltUpdateTriggerEvent(FlowTriggerEventQueue flowChainQueue) {
        Selectable saltUpdateEvent = flowChainQueue.getQueue().remove();
        assertEquals(SALT_UPDATE_EVENT.event(), saltUpdateEvent.selector());
        assertEquals(STACK_ID, saltUpdateEvent.getResourceId());
        assertInstanceOf(StackEvent.class, saltUpdateEvent);
    }

    private void assertClusterUpgradeTriggerEvent(FlowTriggerEventQueue flowChainQueue) {
        Selectable upgradeEvent = flowChainQueue.getQueue().remove();
        assertEquals(CLUSTER_UPGRADE_INIT_EVENT.event(), upgradeEvent.selector());
        assertEquals(STACK_ID, upgradeEvent.getResourceId());
        assertInstanceOf(ClusterUpgradeTriggerEvent.class, upgradeEvent);
        ClusterUpgradeTriggerEvent clusterUpgradeTriggerEvent = (ClusterUpgradeTriggerEvent) upgradeEvent;
        assertEquals(IMAGE_ID, clusterUpgradeTriggerEvent.getImageId());
    }

}