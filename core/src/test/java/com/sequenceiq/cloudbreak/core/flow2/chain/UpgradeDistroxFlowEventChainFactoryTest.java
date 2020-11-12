package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainTriggers.STACK_IMAGE_UPDATE_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeEvent.CLUSTER_UPGRADE_INIT_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateEvent.SALT_UPDATE_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Queue;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterUpgradeTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.DistroxUpgradeTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackImageUpdateTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;

class UpgradeDistroxFlowEventChainFactoryTest {

    private static final long STACK_ID = 1L;

    private final UpgradeDistroxFlowEventChainFactory underTest = new UpgradeDistroxFlowEventChainFactory();

    private final ImageChangeDto imageChangeDto = new ImageChangeDto(STACK_ID, "imageId", "imageCatalogName", "imageCatUrl");

    @Test
    public void testInitEvent() {
        assertEquals(FlowChainTriggers.DISTROX_CLUSTER_UPGRADE_CHAIN_TRIGGER_EVENT, underTest.initEvent());
    }

    @Test
    public void testChainQueue() {
        DistroxUpgradeTriggerEvent event = new DistroxUpgradeTriggerEvent(FlowChainTriggers.DISTROX_CLUSTER_UPGRADE_CHAIN_TRIGGER_EVENT, STACK_ID,
                imageChangeDto, false);
        Queue<Selectable> flowChainQueue = underTest.createFlowTriggerEventQueue(event);
        assertEquals(3, flowChainQueue.size());
        assertSaltUpdateEvent(flowChainQueue);
        assertUpgradeEvent(flowChainQueue);
        assertImageUpdateEvent(flowChainQueue);
    }

    private void assertImageUpdateEvent(Queue<Selectable> flowChainQueue) {
        Selectable imageUpdateEvent = flowChainQueue.remove();
        assertEquals(STACK_IMAGE_UPDATE_TRIGGER_EVENT, imageUpdateEvent.selector());
        assertEquals(STACK_ID, imageUpdateEvent.getResourceId());
        assertTrue(imageUpdateEvent instanceof StackImageUpdateTriggerEvent);
        StackImageUpdateTriggerEvent event = (StackImageUpdateTriggerEvent) imageUpdateEvent;
        assertEquals(imageChangeDto.getImageId(), event.getNewImageId());
        assertEquals(imageChangeDto.getImageCatalogName(), event.getImageCatalogName());
        assertEquals(imageChangeDto.getImageCatalogUrl(), event.getImageCatalogUrl());
    }

    private void assertUpgradeEvent(Queue<Selectable> flowChainQueue) {
        Selectable upgradeEvent = flowChainQueue.remove();
        assertEquals(CLUSTER_UPGRADE_INIT_EVENT.event(), upgradeEvent.selector());
        assertEquals(STACK_ID, upgradeEvent.getResourceId());
        assertTrue(upgradeEvent instanceof ClusterUpgradeTriggerEvent);
        assertEquals(imageChangeDto.getImageId(), ((ClusterUpgradeTriggerEvent) upgradeEvent).getImageId());
    }

    private void assertSaltUpdateEvent(Queue<Selectable> flowChainQueue) {
        Selectable saltUpdateEvent = flowChainQueue.remove();
        assertEquals(SALT_UPDATE_EVENT.event(), saltUpdateEvent.selector());
        assertEquals(STACK_ID, saltUpdateEvent.getResourceId());
        assertTrue(saltUpdateEvent instanceof StackEvent);
    }
}