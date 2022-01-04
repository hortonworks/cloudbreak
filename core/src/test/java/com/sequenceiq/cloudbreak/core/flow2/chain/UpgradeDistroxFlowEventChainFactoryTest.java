package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainTriggers.CLUSTER_REPAIR_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainTriggers.STACK_IMAGE_UPDATE_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeEvent.CLUSTER_UPGRADE_INIT_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.START_CLUSTER_UPGRADE_VALIDATION_INIT_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateEvent.SALT_UPDATE_EVENT;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterUpgradeTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.DistroXUpgradeTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackImageUpdateTriggerEvent;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterRepairTriggerEvent;
import com.sequenceiq.cloudbreak.service.cluster.ClusterRepairService;
import com.sequenceiq.cloudbreak.service.cluster.model.HostGroupName;
import com.sequenceiq.cloudbreak.service.cluster.model.RepairValidation;
import com.sequenceiq.cloudbreak.service.cluster.model.Result;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@ExtendWith(MockitoExtension.class)
class UpgradeDistroxFlowEventChainFactoryTest {

    private static final long STACK_ID = 1L;

    @InjectMocks
    private UpgradeDistroxFlowEventChainFactory underTest;

    private final ImageChangeDto imageChangeDto = new ImageChangeDto(STACK_ID, "imageId", "imageCatalogName", "imageCatUrl");

    @Mock
    private ClusterRepairService clusterRepairService;

    @Test
    public void testInitEvent() {
        assertEquals(FlowChainTriggers.DISTROX_CLUSTER_UPGRADE_CHAIN_TRIGGER_EVENT, underTest.initEvent());
    }

    @Test
    public void testChainQueueForNonReplaceVms() {
        ReflectionTestUtils.setField(underTest, "upgradeValidationEnabled", true);
        DistroXUpgradeTriggerEvent event = new DistroXUpgradeTriggerEvent(FlowChainTriggers.DISTROX_CLUSTER_UPGRADE_CHAIN_TRIGGER_EVENT, STACK_ID,
                imageChangeDto, false, false, "variant");
        FlowTriggerEventQueue flowChainQueue = underTest.createFlowTriggerEventQueue(event);
        assertEquals(4, flowChainQueue.getQueue().size());
        assertUpdateValidationEvent(flowChainQueue);
        assertSaltUpdateEvent(flowChainQueue);
        assertUpgradeEvent(flowChainQueue);
        assertImageUpdateEvent(flowChainQueue);
    }

    @Test
    public void testChainQueueForReplaceVms() {
        ReflectionTestUtils.setField(underTest, "upgradeValidationEnabled", true);
        Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> repairStartResult = Result.success(new HashMap<>());
        when(clusterRepairService.validateRepair(any(), any(), any(), eq(false))).thenReturn(repairStartResult);

        DistroXUpgradeTriggerEvent event = new DistroXUpgradeTriggerEvent(FlowChainTriggers.DISTROX_CLUSTER_UPGRADE_CHAIN_TRIGGER_EVENT, STACK_ID,
                imageChangeDto, true, true, "variant");
        FlowTriggerEventQueue flowChainQueue = underTest.createFlowTriggerEventQueue(event);
        assertEquals(5, flowChainQueue.getQueue().size());
        assertUpdateValidationEvent(flowChainQueue);
        assertSaltUpdateEvent(flowChainQueue);
        assertUpgradeEvent(flowChainQueue);
        assertImageUpdateEvent(flowChainQueue);
        assertRepairEvent(flowChainQueue);
    }

    private void assertUpdateValidationEvent(FlowTriggerEventQueue flowChainQueue) {
        Selectable upgradeValidationEvent = flowChainQueue.getQueue().remove();
        assertEquals(START_CLUSTER_UPGRADE_VALIDATION_INIT_EVENT.event(), upgradeValidationEvent.selector());
        assertEquals(STACK_ID, upgradeValidationEvent.getResourceId());
        assertTrue(upgradeValidationEvent instanceof ClusterUpgradeValidationTriggerEvent);
        assertEquals(imageChangeDto.getImageId(), ((ClusterUpgradeValidationTriggerEvent) upgradeValidationEvent).getImageId());
    }

    private void assertImageUpdateEvent(FlowTriggerEventQueue flowChainQueue) {
        Selectable imageUpdateEvent = flowChainQueue.getQueue().remove();
        assertEquals(STACK_IMAGE_UPDATE_TRIGGER_EVENT, imageUpdateEvent.selector());
        assertEquals(STACK_ID, imageUpdateEvent.getResourceId());
        assertTrue(imageUpdateEvent instanceof StackImageUpdateTriggerEvent);
        StackImageUpdateTriggerEvent event = (StackImageUpdateTriggerEvent) imageUpdateEvent;
        assertEquals(imageChangeDto.getImageId(), event.getNewImageId());
        assertEquals(imageChangeDto.getImageCatalogName(), event.getImageCatalogName());
        assertEquals(imageChangeDto.getImageCatalogUrl(), event.getImageCatalogUrl());
    }

    private void assertUpgradeEvent(FlowTriggerEventQueue flowChainQueue) {
        Selectable upgradeEvent = flowChainQueue.getQueue().remove();
        assertEquals(CLUSTER_UPGRADE_INIT_EVENT.event(), upgradeEvent.selector());
        assertEquals(STACK_ID, upgradeEvent.getResourceId());
        assertTrue(upgradeEvent instanceof ClusterUpgradeTriggerEvent);
        assertEquals(imageChangeDto.getImageId(), ((ClusterUpgradeTriggerEvent) upgradeEvent).getImageId());
    }

    private void assertSaltUpdateEvent(FlowTriggerEventQueue flowChainQueue) {
        Selectable saltUpdateEvent = flowChainQueue.getQueue().remove();
        assertEquals(SALT_UPDATE_EVENT.event(), saltUpdateEvent.selector());
        assertEquals(STACK_ID, saltUpdateEvent.getResourceId());
        assertTrue(saltUpdateEvent instanceof StackEvent);
    }

    private void assertRepairEvent(FlowTriggerEventQueue flowChainQueue) {
        Selectable repairEvent = flowChainQueue.getQueue().remove();
        assertEquals(CLUSTER_REPAIR_TRIGGER_EVENT, repairEvent.selector());
        assertEquals(STACK_ID, repairEvent.getResourceId());
        assertTrue(repairEvent instanceof ClusterRepairTriggerEvent);
    }
}