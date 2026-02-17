package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainTriggers.DATALAKE_CLUSTER_UPGRADE_CHAIN_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainTriggers.STACK_IMAGE_UPDATE_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeEvent.CLUSTER_UPGRADE_INIT_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.START_CLUSTER_UPGRADE_VALIDATION_INIT_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateEvent.SALT_UPDATE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.generator.FlowOfflineStateGraphGenerator.FLOW_CONFIGS_PACKAGE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncEvent.STACK_SYNC_EVENT;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.SALT_MASTER_KEY_PAIR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.chain.util.SetDefaultJavaVersionFlowChainService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.java.SetDefaultJavaVersionFlowEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.java.SetDefaultJavaVersionTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.sync.ClusterSyncEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterUpgradeTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.DataLakeUpgradeFlowChainTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackImageUpdateTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackSyncTriggerEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.rotation.flow.chain.SecretRotationFlowChainTriggerEvent;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.cloudbreak.service.salt.SaltVersionUpgradeService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.upgrade.image.locked.LockedComponentService;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.graph.FlowChainConfigGraphGeneratorUtil;

@ExtendWith(MockitoExtension.class)
class UpgradeDatalakeFlowEventChainFactoryTest {

    private static final String IMAGE_ID = "imageId";

    private static final long STACK_ID = 1L;

    private static final String IMAGE_CATALOG_NAME = "dev";

    private static final String IMAGE_CATALOG_URL = "http://dev.catalog.url";

    @InjectMocks
    private UpgradeDatalakeFlowEventChainFactory underTest;

    @Mock
    private LockedComponentService lockedComponentService;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private SaltVersionUpgradeService saltVersionUpgradeService;

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @Mock
    private SetDefaultJavaVersionFlowChainService setDefaultJavaVersionFlowChainService;

    @Test
    void testInitEvent() {
        assertEquals(DATALAKE_CLUSTER_UPGRADE_CHAIN_TRIGGER_EVENT, underTest.initEvent());
    }

    @Test
    void testCreateFlowTriggerEventQueue() throws Exception {
        Image targetImage = Image.builder()
                .withImageId(IMAGE_ID)
                .withImageCatalogName(IMAGE_CATALOG_NAME)
                .withImageCatalogUrl(IMAGE_CATALOG_URL)
                .build();
        when(componentConfigProviderService.getImage(STACK_ID)).thenReturn(targetImage);
        String secretRotationSelector = EventSelectorUtil.selector(SecretRotationFlowChainTriggerEvent.class);
        when(saltVersionUpgradeService.getSaltSecretRotationTriggerEvent(1L))
                .thenReturn(List.of(
                        new SecretRotationFlowChainTriggerEvent(secretRotationSelector, 1L, null, List.of(SALT_MASTER_KEY_PAIR), null, null)));
        StackDto stackDto = mock(StackDto.class);
        when(stackDtoService.getByIdWithoutResources(1L)).thenReturn(stackDto);
        SetDefaultJavaVersionTriggerEvent setDefaultJavaEvent =
                new SetDefaultJavaVersionTriggerEvent(SetDefaultJavaVersionFlowEvent.SET_DEFAULT_JAVA_VERSION_EVENT.event(), STACK_ID, "17",
                        false, false, false);
        when(setDefaultJavaVersionFlowChainService.setDefaultJavaVersionTriggerEvent(eq(stackDto), any(ImageChangeDto.class)))
                .thenReturn(List.of(setDefaultJavaEvent));

        FlowTriggerEventQueue flowTriggerQueue = underTest.createFlowTriggerEventQueue(
                new DataLakeUpgradeFlowChainTriggerEvent(DATALAKE_CLUSTER_UPGRADE_CHAIN_TRIGGER_EVENT, STACK_ID, IMAGE_ID, true));

        assertEquals(8, flowTriggerQueue.getQueue().size());
        Queue<Selectable> restrainedQueueData = new ConcurrentLinkedDeque<>(flowTriggerQueue.getQueue());
        assertSyncTriggerEvent(flowTriggerQueue);
        assertClusterSyncTriggerEvent(flowTriggerQueue);
        assertClusterUpgradeValidationTriggerEvent(flowTriggerQueue);
        assertSaltSecretRotationTriggerEvent(flowTriggerQueue);
        assertSaltUpdateTriggerEvent(flowTriggerQueue);
        assertImageUpdateTriggerEvent(flowTriggerQueue);
        assertSetDefaultJavaEvent(flowTriggerQueue);
        assertClusterUpgradeTriggerEvent(flowTriggerQueue);
        flowTriggerQueue.getQueue().addAll(restrainedQueueData);
        FlowChainConfigGraphGeneratorUtil.generateFor(underTest, FLOW_CONFIGS_PACKAGE, flowTriggerQueue);
    }

    @Test
    void testCreateFlowTriggerEventQueueShouldThrowNotFoundException() throws Exception {
        doThrow(new CloudbreakImageNotFoundException("Image not found")).when(componentConfigProviderService).getImage(STACK_ID);

        String errorMessage = Assertions.assertThrows(NotFoundException.class, () -> underTest.createFlowTriggerEventQueue(
                new DataLakeUpgradeFlowChainTriggerEvent(DATALAKE_CLUSTER_UPGRADE_CHAIN_TRIGGER_EVENT, STACK_ID, IMAGE_ID, true))).getMessage();
        assertEquals("Image not found for stack", errorMessage);
    }

    private void assertImageUpdateTriggerEvent(FlowTriggerEventQueue flowTriggerEventQueue) {
        Selectable event = flowTriggerEventQueue.getQueue().remove();
        assertEquals(STACK_IMAGE_UPDATE_TRIGGER_EVENT, event.getSelector());
        assertEquals(STACK_ID, event.getResourceId());
        assertInstanceOf(StackImageUpdateTriggerEvent.class, event);
        StackImageUpdateTriggerEvent triggerEvent = (StackImageUpdateTriggerEvent) event;
        assertEquals(IMAGE_ID, triggerEvent.getNewImageId());
        assertEquals(IMAGE_CATALOG_NAME, triggerEvent.getImageCatalogName());
        assertEquals(IMAGE_CATALOG_URL, triggerEvent.getImageCatalogUrl());
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

    private void assertSetDefaultJavaEvent(FlowTriggerEventQueue flowChainQueue) {
        Selectable javaEvent = flowChainQueue.getQueue().remove();
        assertInstanceOf(SetDefaultJavaVersionTriggerEvent.class, javaEvent);
    }

}