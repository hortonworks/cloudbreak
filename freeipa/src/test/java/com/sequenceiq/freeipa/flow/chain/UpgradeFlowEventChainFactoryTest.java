package com.sequenceiq.freeipa.flow.chain;

import static com.sequenceiq.freeipa.flow.stack.image.change.event.ImageChangeEvents.IMAGE_CHANGE_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Queue;

import org.junit.jupiter.api.Test;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsRequest;
import com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.DownscaleEvent;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.event.ChangePrimaryGatewayEvent;
import com.sequenceiq.freeipa.flow.freeipa.salt.update.SaltUpdateEvent;
import com.sequenceiq.freeipa.flow.freeipa.salt.update.SaltUpdateTriggerEvent;
import com.sequenceiq.freeipa.flow.freeipa.upgrade.UpgradeEvent;
import com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleEvent;
import com.sequenceiq.freeipa.flow.stack.image.change.event.ImageChangeEvent;

class UpgradeFlowEventChainFactoryTest {

    public static final String OPERATION_ID = "opId";

    public static final long STACK_ID = 1L;

    private UpgradeFlowEventChainFactory underTest = new UpgradeFlowEventChainFactory();

    @Test
    public void testFlowChainCreation() {
        ImageSettingsRequest imageSettingsRequest = new ImageSettingsRequest();
        UpgradeEvent event = new UpgradeEvent("selector", STACK_ID, Sets.newHashSet("repl1", "repl2"), "pgw", OPERATION_ID, imageSettingsRequest, false);

        FlowTriggerEventQueue eventQueue = underTest.createFlowTriggerEventQueue(event);

        assertEquals("UpgradeFlowEventChainFactory", eventQueue.getFlowChainName());
        Queue<Selectable> queue = eventQueue.getQueue();
        assertEquals(10, queue.size());

        SaltUpdateTriggerEvent saltUpdateTriggerEvent = (SaltUpdateTriggerEvent) queue.poll();
        assertEquals(OPERATION_ID, saltUpdateTriggerEvent.getOperationId());
        assertEquals(STACK_ID, saltUpdateTriggerEvent.getResourceId());
        assertEquals(SaltUpdateEvent.SALT_UPDATE_EVENT.event(), saltUpdateTriggerEvent.selector());
        assertFalse(saltUpdateTriggerEvent.isFinalChain());

        ImageChangeEvent imageChangeEvent = (ImageChangeEvent) queue.poll();
        assertEquals(OPERATION_ID, imageChangeEvent.getOperationId());
        assertEquals(STACK_ID, imageChangeEvent.getResourceId());
        assertEquals(IMAGE_CHANGE_EVENT.event(), imageChangeEvent.selector());
        assertEquals(imageSettingsRequest, imageChangeEvent.getRequest());

        UpscaleEvent upscaleEvent1 = (UpscaleEvent) queue.poll();
        assertEquals(OPERATION_ID, upscaleEvent1.getOperationId());
        assertEquals(STACK_ID, upscaleEvent1.getResourceId());
        assertTrue(upscaleEvent1.isChained());
        assertFalse(upscaleEvent1.isFinalChain());
        assertFalse(upscaleEvent1.isRepair());
        assertEquals(UpscaleFlowEvent.UPSCALE_EVENT.event(), upscaleEvent1.selector());
        assertEquals(4, upscaleEvent1.getInstanceCountByGroup());

        DownscaleEvent downscaleEvent1 = (DownscaleEvent) queue.poll();
        assertEquals(OPERATION_ID, downscaleEvent1.getOperationId());
        assertEquals(STACK_ID, downscaleEvent1.getResourceId());
        assertTrue(downscaleEvent1.isChained());
        assertFalse(downscaleEvent1.isFinalChain());
        assertFalse(downscaleEvent1.isRepair());
        assertEquals(DownscaleFlowEvent.DOWNSCALE_EVENT.event(), downscaleEvent1.selector());
        assertEquals(3, downscaleEvent1.getInstanceCountByGroup());
        assertEquals(1, downscaleEvent1.getInstanceIds().size());
        String firstInstanceToDownscale = downscaleEvent1.getInstanceIds().get(0);
        assertTrue(firstInstanceToDownscale.startsWith("repl"));

        UpscaleEvent upscaleEvent2 = (UpscaleEvent) queue.poll();
        assertEquals(OPERATION_ID, upscaleEvent2.getOperationId());
        assertEquals(STACK_ID, upscaleEvent2.getResourceId());
        assertTrue(upscaleEvent2.isChained());
        assertFalse(upscaleEvent2.isFinalChain());
        assertFalse(upscaleEvent2.isRepair());
        assertEquals(UpscaleFlowEvent.UPSCALE_EVENT.event(), upscaleEvent2.selector());
        assertEquals(4, upscaleEvent2.getInstanceCountByGroup());

        DownscaleEvent downscaleEvent2 = (DownscaleEvent) queue.poll();
        assertEquals(OPERATION_ID, downscaleEvent2.getOperationId());
        assertEquals(STACK_ID, downscaleEvent2.getResourceId());
        assertTrue(downscaleEvent2.isChained());
        assertFalse(downscaleEvent2.isFinalChain());
        assertFalse(downscaleEvent2.isRepair());
        assertEquals(DownscaleFlowEvent.DOWNSCALE_EVENT.event(), downscaleEvent2.selector());
        assertEquals(3, downscaleEvent2.getInstanceCountByGroup());
        assertEquals(1, downscaleEvent2.getInstanceIds().size());
        String secondInstanceToDownscale = downscaleEvent2.getInstanceIds().get(0);
        assertTrue(secondInstanceToDownscale.startsWith("repl"));

        assertNotEquals(firstInstanceToDownscale, secondInstanceToDownscale);

        UpscaleEvent upscaleEvent3 = (UpscaleEvent) queue.poll();
        assertEquals(OPERATION_ID, upscaleEvent3.getOperationId());
        assertEquals(STACK_ID, upscaleEvent3.getResourceId());
        assertTrue(upscaleEvent3.isChained());
        assertFalse(upscaleEvent3.isFinalChain());
        assertFalse(upscaleEvent3.isRepair());
        assertEquals(UpscaleFlowEvent.UPSCALE_EVENT.event(), upscaleEvent3.selector());
        assertEquals(4, upscaleEvent3.getInstanceCountByGroup());

        ChangePrimaryGatewayEvent changePrimaryGatewayEvent = (ChangePrimaryGatewayEvent) queue.poll();
        assertEquals(OPERATION_ID, changePrimaryGatewayEvent.getOperationId());
        assertEquals(STACK_ID, changePrimaryGatewayEvent.getResourceId());
        assertFalse(changePrimaryGatewayEvent.isFinalChain());
        assertEquals(ChangePrimaryGatewayFlowEvent.CHANGE_PRIMARY_GATEWAY_EVENT.event(), changePrimaryGatewayEvent.selector());
        assertEquals(3, changePrimaryGatewayEvent.getRepairInstaceIds().size());
        assertTrue(List.of("repl1", "repl2", "pgw").containsAll(changePrimaryGatewayEvent.getRepairInstaceIds()));

        DownscaleEvent downscaleEvent3 = (DownscaleEvent) queue.poll();
        assertEquals(OPERATION_ID, downscaleEvent3.getOperationId());
        assertEquals(STACK_ID, downscaleEvent3.getResourceId());
        assertTrue(downscaleEvent3.isChained());
        assertFalse(downscaleEvent3.isFinalChain());
        assertFalse(downscaleEvent3.isRepair());
        assertEquals(DownscaleFlowEvent.DOWNSCALE_EVENT.event(), downscaleEvent3.selector());
        assertEquals(3, downscaleEvent3.getInstanceCountByGroup());
        assertEquals(1, downscaleEvent3.getInstanceIds().size());
        assertEquals("pgw", downscaleEvent3.getInstanceIds().get(0));

        SaltUpdateTriggerEvent saltUpdateTriggerEvent2 = (SaltUpdateTriggerEvent) queue.poll();
        assertEquals(OPERATION_ID, saltUpdateTriggerEvent2.getOperationId());
        assertEquals(STACK_ID, saltUpdateTriggerEvent2.getResourceId());
        assertEquals(SaltUpdateEvent.SALT_UPDATE_EVENT.event(), saltUpdateTriggerEvent2.selector());
        assertTrue(saltUpdateTriggerEvent2.isChained());
        assertTrue(saltUpdateTriggerEvent2.isFinalChain());
    }

    @Test
    public void testInitEvent() {
        assertEquals(FlowChainTriggers.UPGRADE_TRIGGER_EVENT, underTest.initEvent());
    }

}