package com.sequenceiq.freeipa.flow.chain;

import static com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIpaVerticalScaleEvent.STACK_VERTICALSCALE_EVENT;
import static com.sequenceiq.freeipa.flow.graph.FlowOfflineStateGraphGenerator.FLOW_CONFIGS_PACKAGE_NAME;
import static com.sequenceiq.freeipa.flow.stack.image.change.event.ImageChangeEvents.IMAGE_CHANGE_EVENT;
import static com.sequenceiq.freeipa.rotation.FreeIpaSecretType.SALT_MASTER_KEY_PAIR;
import static com.sequenceiq.freeipa.rotation.FreeIpaSecretType.SALT_SIGN_KEY_PAIR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.rotation.flow.chain.SecretRotationFlowChainTriggerEvent;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.core.chain.init.flowevents.FlowChainInitPayload;
import com.sequenceiq.flow.graph.FlowChainConfigGraphGeneratorUtil;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.VerticalScaleRequest;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.SaltSecurityConfig;
import com.sequenceiq.freeipa.entity.SecurityConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.DownscaleEvent;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.event.ChangePrimaryGatewayEvent;
import com.sequenceiq.freeipa.flow.freeipa.salt.update.SaltUpdateEvent;
import com.sequenceiq.freeipa.flow.freeipa.salt.update.SaltUpdateTriggerEvent;
import com.sequenceiq.freeipa.flow.freeipa.upgrade.UpgradeEvent;
import com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleEvent;
import com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIpaVerticalScalingTriggerEvent;
import com.sequenceiq.freeipa.flow.stack.image.change.event.ImageChangeEvent;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.instance.InstanceGroupService;

@ExtendWith(MockitoExtension.class)
class UpgradeFlowEventChainFactoryTest {

    private static final String OPERATION_ID = "opId";

    private static final long STACK_ID = 1L;

    @Mock
    private InstanceGroupService instanceGroupService;

    @Mock
    private StackService stackService;

    @InjectMocks
    private UpgradeFlowEventChainFactory underTest;

    @Test
    void testFlowChainCreation() {
        ImageSettingsRequest imageSettingsRequest = new ImageSettingsRequest();
        UpgradeEvent event = new UpgradeEvent("selector", STACK_ID, Sets.newHashSet("repl1", "repl2"), "pgw", OPERATION_ID,
                imageSettingsRequest, false, false, null, null, null);
        when(stackService.getByIdWithListsInTransaction(eq(STACK_ID))).thenReturn(createStack(false, false));

        FlowTriggerEventQueue eventQueue = underTest.createFlowTriggerEventQueue(event);

        assertEquals("UpgradeFlowEventChainFactory", eventQueue.getFlowChainName());
        Queue<Selectable> queue = eventQueue.getQueue();
        Queue<Selectable> restrainedQueueData = new ConcurrentLinkedQueue<>(queue);
        assertEquals(12, queue.size());

        FlowChainInitPayload flowChainInitPayload = (FlowChainInitPayload) queue.poll();
        assertEquals(STACK_ID, flowChainInitPayload.getResourceId());

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
        assertFalse(upscaleEvent1.getRepair());
        assertEquals(UpscaleFlowEvent.UPSCALE_EVENT.event(), upscaleEvent1.selector());
        assertEquals(4, upscaleEvent1.getInstanceCountByGroup());

        ChangePrimaryGatewayEvent changePrimaryGatewayEvent = (ChangePrimaryGatewayEvent) queue.poll();
        assertEquals(OPERATION_ID, changePrimaryGatewayEvent.getOperationId());
        assertEquals(STACK_ID, changePrimaryGatewayEvent.getResourceId());
        assertFalse(changePrimaryGatewayEvent.getFinalChain());
        assertEquals(ChangePrimaryGatewayFlowEvent.CHANGE_PRIMARY_GATEWAY_EVENT.event(), changePrimaryGatewayEvent.selector());
        assertEquals(3, changePrimaryGatewayEvent.getRepairInstanceIds().size());
        assertTrue(List.of("repl1", "repl2", "pgw").containsAll(changePrimaryGatewayEvent.getRepairInstanceIds()));

        DownscaleEvent downscaleEvent1 = (DownscaleEvent) queue.poll();
        assertEquals(OPERATION_ID, downscaleEvent1.getOperationId());
        assertEquals(STACK_ID, downscaleEvent1.getResourceId());
        assertTrue(downscaleEvent1.isChained());
        assertFalse(downscaleEvent1.isFinalChain());
        assertFalse(downscaleEvent1.isRepair());
        assertEquals(DownscaleFlowEvent.DOWNSCALE_EVENT.event(), downscaleEvent1.selector());
        assertEquals(3, downscaleEvent1.getInstanceCountByGroup());
        assertEquals(1, downscaleEvent1.getInstanceIds().size());
        assertEquals("pgw", downscaleEvent1.getInstanceIds().get(0));

        UpscaleEvent upscaleEvent2 = (UpscaleEvent) queue.poll();
        assertEquals(OPERATION_ID, upscaleEvent2.getOperationId());
        assertEquals(STACK_ID, upscaleEvent2.getResourceId());
        assertTrue(upscaleEvent2.isChained());
        assertFalse(upscaleEvent2.isFinalChain());
        assertFalse(upscaleEvent2.getRepair());
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

        UpscaleEvent upscaleEvent3 = (UpscaleEvent) queue.poll();
        assertEquals(OPERATION_ID, upscaleEvent3.getOperationId());
        assertEquals(STACK_ID, upscaleEvent3.getResourceId());
        assertTrue(upscaleEvent3.isChained());
        assertFalse(upscaleEvent3.isFinalChain());
        assertFalse(upscaleEvent3.getRepair());
        assertEquals(UpscaleFlowEvent.UPSCALE_EVENT.event(), upscaleEvent3.selector());
        assertEquals(4, upscaleEvent3.getInstanceCountByGroup());

        DownscaleEvent downscaleEvent3 = (DownscaleEvent) queue.poll();
        assertEquals(OPERATION_ID, downscaleEvent3.getOperationId());
        assertEquals(STACK_ID, downscaleEvent3.getResourceId());
        assertTrue(downscaleEvent3.isChained());
        assertFalse(downscaleEvent3.isFinalChain());
        assertFalse(downscaleEvent3.isRepair());
        assertEquals(DownscaleFlowEvent.DOWNSCALE_EVENT.event(), downscaleEvent3.selector());
        assertEquals(3, downscaleEvent3.getInstanceCountByGroup());
        assertEquals(1, downscaleEvent3.getInstanceIds().size());
        String firstInstanceToDownscale = downscaleEvent3.getInstanceIds().get(0);
        assertTrue(firstInstanceToDownscale.startsWith("repl"));

        assertNotEquals(firstInstanceToDownscale, secondInstanceToDownscale);

        SaltUpdateTriggerEvent saltUpdateTriggerEvent2 = (SaltUpdateTriggerEvent) queue.poll();
        assertEquals(OPERATION_ID, saltUpdateTriggerEvent2.getOperationId());
        assertEquals(STACK_ID, saltUpdateTriggerEvent2.getResourceId());
        assertEquals(SaltUpdateEvent.SALT_UPDATE_EVENT.event(), saltUpdateTriggerEvent2.selector());
        assertTrue(saltUpdateTriggerEvent2.isChained());
        assertTrue(saltUpdateTriggerEvent2.isFinalChain());

        eventQueue.getQueue().addAll(restrainedQueueData);
        FlowChainConfigGraphGeneratorUtil.generateFor(underTest, FLOW_CONFIGS_PACKAGE_NAME, eventQueue);
    }

    @Test
    void testFlowChainCreationOnlyOneRepl() {
        ImageSettingsRequest imageSettingsRequest = new ImageSettingsRequest();
        UpgradeEvent event = new UpgradeEvent("selector", STACK_ID, Sets.newHashSet("repl1", "repl2"), "pgw", OPERATION_ID,
                imageSettingsRequest, false, false, null, null, Sets.newHashSet("repl2"));
        when(stackService.getByIdWithListsInTransaction(eq(STACK_ID))).thenReturn(createStack(false, false));

        FlowTriggerEventQueue eventQueue = underTest.createFlowTriggerEventQueue(event);

        assertEquals("UpgradeFlowEventChainFactory", eventQueue.getFlowChainName());
        Queue<Selectable> queue = eventQueue.getQueue();
        Queue<Selectable> restrainedQueueData = new ConcurrentLinkedQueue<>(queue);
        assertEquals(7, queue.size());

        FlowChainInitPayload flowChainInitPayload = (FlowChainInitPayload) queue.poll();
        assertEquals(STACK_ID, flowChainInitPayload.getResourceId());

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
        assertFalse(upscaleEvent1.getRepair());
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
        assertTrue(firstInstanceToDownscale.startsWith("repl2"));

        SaltUpdateTriggerEvent saltUpdateTriggerEvent2 = (SaltUpdateTriggerEvent) queue.poll();
        assertEquals(OPERATION_ID, saltUpdateTriggerEvent2.getOperationId());
        assertEquals(STACK_ID, saltUpdateTriggerEvent2.getResourceId());
        assertEquals(SaltUpdateEvent.SALT_UPDATE_EVENT.event(), saltUpdateTriggerEvent2.selector());
        assertTrue(saltUpdateTriggerEvent2.isChained());
        assertTrue(saltUpdateTriggerEvent2.isFinalChain());

        eventQueue.getQueue().addAll(restrainedQueueData);
        FlowChainConfigGraphGeneratorUtil.generateFor(underTest, FLOW_CONFIGS_PACKAGE_NAME, eventQueue, "WITH_ONLY_ONE_REPLACEMENT");
    }

    @Test
    void testFlowChainCreationOnlyPgw() {
        ImageSettingsRequest imageSettingsRequest = new ImageSettingsRequest();
        UpgradeEvent event = new UpgradeEvent("selector", STACK_ID, Sets.newHashSet("repl1", "repl2"), "pgw", OPERATION_ID,
                imageSettingsRequest, false, false, null, null, Sets.newHashSet("pgw"));
        when(stackService.getByIdWithListsInTransaction(eq(STACK_ID))).thenReturn(createStack(false, false));

        FlowTriggerEventQueue eventQueue = underTest.createFlowTriggerEventQueue(event);

        assertEquals("UpgradeFlowEventChainFactory", eventQueue.getFlowChainName());
        Queue<Selectable> queue = eventQueue.getQueue();
        Queue<Selectable> restrainedQueueData = new ConcurrentLinkedQueue<>(queue);
        assertEquals(8, queue.size());

        FlowChainInitPayload flowChainInitPayload = (FlowChainInitPayload) queue.poll();
        assertEquals(STACK_ID, flowChainInitPayload.getResourceId());

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

        UpscaleEvent upscaleEvent3 = (UpscaleEvent) queue.poll();
        assertEquals(OPERATION_ID, upscaleEvent3.getOperationId());
        assertEquals(STACK_ID, upscaleEvent3.getResourceId());
        assertTrue(upscaleEvent3.isChained());
        assertFalse(upscaleEvent3.isFinalChain());
        assertFalse(upscaleEvent3.getRepair());
        assertEquals(UpscaleFlowEvent.UPSCALE_EVENT.event(), upscaleEvent3.selector());
        assertEquals(4, upscaleEvent3.getInstanceCountByGroup());

        ChangePrimaryGatewayEvent changePrimaryGatewayEvent = (ChangePrimaryGatewayEvent) queue.poll();
        assertEquals(OPERATION_ID, changePrimaryGatewayEvent.getOperationId());
        assertEquals(STACK_ID, changePrimaryGatewayEvent.getResourceId());
        assertFalse(changePrimaryGatewayEvent.getFinalChain());
        assertEquals(ChangePrimaryGatewayFlowEvent.CHANGE_PRIMARY_GATEWAY_EVENT.event(), changePrimaryGatewayEvent.selector());
        assertEquals(3, changePrimaryGatewayEvent.getRepairInstanceIds().size());
        assertTrue(List.of("repl1", "repl2", "pgw").containsAll(changePrimaryGatewayEvent.getRepairInstanceIds()));

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

        eventQueue.getQueue().addAll(restrainedQueueData);
        FlowChainConfigGraphGeneratorUtil.generateFor(underTest, FLOW_CONFIGS_PACKAGE_NAME, eventQueue, "WITH_ONLY_PRIMARY_GATEWAY_REPLACEMENT");
    }

    @Test
    void testFlowChainCreationWithOldInstances() {
        ImageSettingsRequest imageSettingsRequest = new ImageSettingsRequest();
        UpgradeEvent event = new UpgradeEvent("selector", STACK_ID, Sets.newHashSet("repl1", "repl2"), "pgw", OPERATION_ID,
                imageSettingsRequest, false, false, null, null, Sets.newHashSet("repl2", "pgw"));
        when(stackService.getByIdWithListsInTransaction(eq(STACK_ID))).thenReturn(createStack(false, false));

        FlowTriggerEventQueue eventQueue = underTest.createFlowTriggerEventQueue(event);

        assertEquals("UpgradeFlowEventChainFactory", eventQueue.getFlowChainName());
        Queue<Selectable> queue = eventQueue.getQueue();
        Queue<Selectable> restrainedQueueData = new ConcurrentLinkedQueue<>(queue);
        assertEquals(10, queue.size());

        FlowChainInitPayload flowChainInitPayload = (FlowChainInitPayload) queue.poll();
        assertEquals(STACK_ID, flowChainInitPayload.getResourceId());

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
        assertFalse(upscaleEvent1.getRepair());
        assertEquals(UpscaleFlowEvent.UPSCALE_EVENT.event(), upscaleEvent1.selector());
        assertEquals(4, upscaleEvent1.getInstanceCountByGroup());

        ChangePrimaryGatewayEvent changePrimaryGatewayEvent = (ChangePrimaryGatewayEvent) queue.poll();
        assertEquals(OPERATION_ID, changePrimaryGatewayEvent.getOperationId());
        assertEquals(STACK_ID, changePrimaryGatewayEvent.getResourceId());
        assertFalse(changePrimaryGatewayEvent.getFinalChain());
        assertEquals(ChangePrimaryGatewayFlowEvent.CHANGE_PRIMARY_GATEWAY_EVENT.event(), changePrimaryGatewayEvent.selector());
        assertEquals(3, changePrimaryGatewayEvent.getRepairInstanceIds().size());
        assertTrue(List.of("repl1", "repl2", "pgw").containsAll(changePrimaryGatewayEvent.getRepairInstanceIds()));

        DownscaleEvent downscaleEvent1 = (DownscaleEvent) queue.poll();
        assertEquals(OPERATION_ID, downscaleEvent1.getOperationId());
        assertEquals(STACK_ID, downscaleEvent1.getResourceId());
        assertTrue(downscaleEvent1.isChained());
        assertFalse(downscaleEvent1.isFinalChain());
        assertFalse(downscaleEvent1.isRepair());
        assertEquals(DownscaleFlowEvent.DOWNSCALE_EVENT.event(), downscaleEvent1.selector());
        assertEquals(3, downscaleEvent1.getInstanceCountByGroup());
        assertEquals(1, downscaleEvent1.getInstanceIds().size());
        assertEquals("pgw", downscaleEvent1.getInstanceIds().get(0));

        UpscaleEvent upscaleEvent3 = (UpscaleEvent) queue.poll();
        assertEquals(OPERATION_ID, upscaleEvent3.getOperationId());
        assertEquals(STACK_ID, upscaleEvent3.getResourceId());
        assertTrue(upscaleEvent3.isChained());
        assertFalse(upscaleEvent3.isFinalChain());
        assertFalse(upscaleEvent3.getRepair());
        assertEquals(UpscaleFlowEvent.UPSCALE_EVENT.event(), upscaleEvent3.selector());
        assertEquals(4, upscaleEvent3.getInstanceCountByGroup());

        DownscaleEvent downscaleEvent3 = (DownscaleEvent) queue.poll();
        assertEquals(OPERATION_ID, downscaleEvent3.getOperationId());
        assertEquals(STACK_ID, downscaleEvent3.getResourceId());
        assertTrue(downscaleEvent3.isChained());
        assertFalse(downscaleEvent3.isFinalChain());
        assertFalse(downscaleEvent3.isRepair());
        assertEquals(DownscaleFlowEvent.DOWNSCALE_EVENT.event(), downscaleEvent3.selector());
        assertEquals(3, downscaleEvent3.getInstanceCountByGroup());
        assertEquals(1, downscaleEvent3.getInstanceIds().size());
        String firstInstanceToDownscale = downscaleEvent3.getInstanceIds().get(0);
        assertTrue(firstInstanceToDownscale.startsWith("repl2"));

        SaltUpdateTriggerEvent saltUpdateTriggerEvent2 = (SaltUpdateTriggerEvent) queue.poll();
        assertEquals(OPERATION_ID, saltUpdateTriggerEvent2.getOperationId());
        assertEquals(STACK_ID, saltUpdateTriggerEvent2.getResourceId());
        assertEquals(SaltUpdateEvent.SALT_UPDATE_EVENT.event(), saltUpdateTriggerEvent2.selector());
        assertTrue(saltUpdateTriggerEvent2.isChained());
        assertTrue(saltUpdateTriggerEvent2.isFinalChain());

        eventQueue.getQueue().addAll(restrainedQueueData);
        FlowChainConfigGraphGeneratorUtil.generateFor(underTest, FLOW_CONFIGS_PACKAGE_NAME, eventQueue, "WITH_INSTANCES_ON_OLD_IMAGE");
    }

    @Test
    void testFlowChainCreationWithVerticalScale() {
        ImageSettingsRequest imageSettingsRequest = new ImageSettingsRequest();
        UpgradeEvent event = new UpgradeEvent("selector", STACK_ID, Sets.newHashSet("repl1", "repl2"), "pgw", OPERATION_ID,
                imageSettingsRequest, false, false, null, new VerticalScaleRequest(), null);
        when(stackService.getByIdWithListsInTransaction(eq(STACK_ID))).thenReturn(createStack(false, false));

        FlowTriggerEventQueue eventQueue = underTest.createFlowTriggerEventQueue(event);

        assertEquals("UpgradeFlowEventChainFactory", eventQueue.getFlowChainName());
        Queue<Selectable> queue = eventQueue.getQueue();
        Queue<Selectable> restrainedQueueData = new ConcurrentLinkedQueue<>(queue);
        assertEquals(13, queue.size());

        FlowChainInitPayload flowChainInitPayload = (FlowChainInitPayload) queue.poll();
        assertEquals(STACK_ID, flowChainInitPayload.getResourceId());

        SaltUpdateTriggerEvent saltUpdateTriggerEvent = (SaltUpdateTriggerEvent) queue.poll();
        assertEquals(OPERATION_ID, saltUpdateTriggerEvent.getOperationId());
        assertEquals(STACK_ID, saltUpdateTriggerEvent.getResourceId());
        assertEquals(SaltUpdateEvent.SALT_UPDATE_EVENT.event(), saltUpdateTriggerEvent.selector());
        assertFalse(saltUpdateTriggerEvent.isFinalChain());

        FreeIpaVerticalScalingTriggerEvent verticalScalingTriggerEvent = (FreeIpaVerticalScalingTriggerEvent) queue.poll();
        assertEquals(OPERATION_ID, verticalScalingTriggerEvent.getOperationId());
        assertEquals(STACK_ID, verticalScalingTriggerEvent.getResourceId());
        assertEquals(STACK_VERTICALSCALE_EVENT.event(), verticalScalingTriggerEvent.selector());

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
        assertFalse(upscaleEvent1.getRepair());
        assertEquals(UpscaleFlowEvent.UPSCALE_EVENT.event(), upscaleEvent1.selector());
        assertEquals(4, upscaleEvent1.getInstanceCountByGroup());

        ChangePrimaryGatewayEvent changePrimaryGatewayEvent = (ChangePrimaryGatewayEvent) queue.poll();
        assertEquals(OPERATION_ID, changePrimaryGatewayEvent.getOperationId());
        assertEquals(STACK_ID, changePrimaryGatewayEvent.getResourceId());
        assertFalse(changePrimaryGatewayEvent.getFinalChain());
        assertEquals(ChangePrimaryGatewayFlowEvent.CHANGE_PRIMARY_GATEWAY_EVENT.event(), changePrimaryGatewayEvent.selector());
        assertEquals(3, changePrimaryGatewayEvent.getRepairInstanceIds().size());
        assertTrue(List.of("repl1", "repl2", "pgw").containsAll(changePrimaryGatewayEvent.getRepairInstanceIds()));

        DownscaleEvent downscaleEvent1 = (DownscaleEvent) queue.poll();
        assertEquals(OPERATION_ID, downscaleEvent1.getOperationId());
        assertEquals(STACK_ID, downscaleEvent1.getResourceId());
        assertTrue(downscaleEvent1.isChained());
        assertFalse(downscaleEvent1.isFinalChain());
        assertFalse(downscaleEvent1.isRepair());
        assertEquals(DownscaleFlowEvent.DOWNSCALE_EVENT.event(), downscaleEvent1.selector());
        assertEquals(3, downscaleEvent1.getInstanceCountByGroup());
        assertEquals(1, downscaleEvent1.getInstanceIds().size());
        assertEquals("pgw", downscaleEvent1.getInstanceIds().get(0));

        UpscaleEvent upscaleEvent2 = (UpscaleEvent) queue.poll();
        assertEquals(OPERATION_ID, upscaleEvent2.getOperationId());
        assertEquals(STACK_ID, upscaleEvent2.getResourceId());
        assertTrue(upscaleEvent2.isChained());
        assertFalse(upscaleEvent2.isFinalChain());
        assertFalse(upscaleEvent2.getRepair());
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

        UpscaleEvent upscaleEvent3 = (UpscaleEvent) queue.poll();
        assertEquals(OPERATION_ID, upscaleEvent3.getOperationId());
        assertEquals(STACK_ID, upscaleEvent3.getResourceId());
        assertTrue(upscaleEvent3.isChained());
        assertFalse(upscaleEvent3.isFinalChain());
        assertFalse(upscaleEvent3.getRepair());
        assertEquals(UpscaleFlowEvent.UPSCALE_EVENT.event(), upscaleEvent3.selector());
        assertEquals(4, upscaleEvent3.getInstanceCountByGroup());

        DownscaleEvent downscaleEvent3 = (DownscaleEvent) queue.poll();
        assertEquals(OPERATION_ID, downscaleEvent3.getOperationId());
        assertEquals(STACK_ID, downscaleEvent3.getResourceId());
        assertTrue(downscaleEvent3.isChained());
        assertFalse(downscaleEvent3.isFinalChain());
        assertFalse(downscaleEvent3.isRepair());
        assertEquals(DownscaleFlowEvent.DOWNSCALE_EVENT.event(), downscaleEvent3.selector());
        assertEquals(3, downscaleEvent3.getInstanceCountByGroup());
        assertEquals(1, downscaleEvent3.getInstanceIds().size());
        String firstInstanceToDownscale = downscaleEvent3.getInstanceIds().get(0);
        assertTrue(firstInstanceToDownscale.startsWith("repl"));

        assertNotEquals(firstInstanceToDownscale, secondInstanceToDownscale);

        SaltUpdateTriggerEvent saltUpdateTriggerEvent2 = (SaltUpdateTriggerEvent) queue.poll();
        assertEquals(OPERATION_ID, saltUpdateTriggerEvent2.getOperationId());
        assertEquals(STACK_ID, saltUpdateTriggerEvent2.getResourceId());
        assertEquals(SaltUpdateEvent.SALT_UPDATE_EVENT.event(), saltUpdateTriggerEvent2.selector());
        assertTrue(saltUpdateTriggerEvent2.isChained());
        assertTrue(saltUpdateTriggerEvent2.isFinalChain());

        eventQueue.getQueue().addAll(restrainedQueueData);
        FlowChainConfigGraphGeneratorUtil.generateFor(underTest, FLOW_CONFIGS_PACKAGE_NAME, eventQueue, "WITH_VERTICAL_SCALE");
    }

    @Test
    void testFlowChainCreationWhenSaltKeyRotationRequired() {
        ImageSettingsRequest imageSettingsRequest = new ImageSettingsRequest();
        UpgradeEvent event = new UpgradeEvent("selector", STACK_ID, Sets.newHashSet("repl1", "repl2"), "pgw", OPERATION_ID,
                imageSettingsRequest, false, false, null, null, null);
        when(stackService.getByIdWithListsInTransaction(eq(STACK_ID))).thenReturn(createStack(true, true));

        FlowTriggerEventQueue eventQueue = underTest.createFlowTriggerEventQueue(event);

        assertEquals("UpgradeFlowEventChainFactory", eventQueue.getFlowChainName());
        Queue<Selectable> queue = eventQueue.getQueue();
        Queue<Selectable> restrainedQueueData = new ConcurrentLinkedQueue<>(queue);
        assertEquals(13, queue.size());

        FlowChainInitPayload flowChainInitPayload = (FlowChainInitPayload) queue.poll();
        assertEquals(STACK_ID, flowChainInitPayload.getResourceId());

        SecretRotationFlowChainTriggerEvent secretRotationTriggerEvent = (SecretRotationFlowChainTriggerEvent) queue.poll();
        assertEquals(STACK_ID, secretRotationTriggerEvent.getResourceId());
        assertNull(secretRotationTriggerEvent.getExecutionType());
        assertNull(secretRotationTriggerEvent.getAdditionalProperties());
        assertThat(secretRotationTriggerEvent.getSecretTypes()).containsExactly(SALT_SIGN_KEY_PAIR, SALT_MASTER_KEY_PAIR);

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
        assertFalse(upscaleEvent1.getRepair());
        assertEquals(UpscaleFlowEvent.UPSCALE_EVENT.event(), upscaleEvent1.selector());
        assertEquals(4, upscaleEvent1.getInstanceCountByGroup());

        ChangePrimaryGatewayEvent changePrimaryGatewayEvent = (ChangePrimaryGatewayEvent) queue.poll();
        assertEquals(OPERATION_ID, changePrimaryGatewayEvent.getOperationId());
        assertEquals(STACK_ID, changePrimaryGatewayEvent.getResourceId());
        assertFalse(changePrimaryGatewayEvent.getFinalChain());
        assertEquals(ChangePrimaryGatewayFlowEvent.CHANGE_PRIMARY_GATEWAY_EVENT.event(), changePrimaryGatewayEvent.selector());
        assertEquals(3, changePrimaryGatewayEvent.getRepairInstanceIds().size());
        assertTrue(List.of("repl1", "repl2", "pgw").containsAll(changePrimaryGatewayEvent.getRepairInstanceIds()));

        DownscaleEvent downscaleEvent1 = (DownscaleEvent) queue.poll();
        assertEquals(OPERATION_ID, downscaleEvent1.getOperationId());
        assertEquals(STACK_ID, downscaleEvent1.getResourceId());
        assertTrue(downscaleEvent1.isChained());
        assertFalse(downscaleEvent1.isFinalChain());
        assertFalse(downscaleEvent1.isRepair());
        assertEquals(DownscaleFlowEvent.DOWNSCALE_EVENT.event(), downscaleEvent1.selector());
        assertEquals(3, downscaleEvent1.getInstanceCountByGroup());
        assertEquals(1, downscaleEvent1.getInstanceIds().size());
        assertEquals("pgw", downscaleEvent1.getInstanceIds().get(0));

        UpscaleEvent upscaleEvent2 = (UpscaleEvent) queue.poll();
        assertEquals(OPERATION_ID, upscaleEvent2.getOperationId());
        assertEquals(STACK_ID, upscaleEvent2.getResourceId());
        assertTrue(upscaleEvent2.isChained());
        assertFalse(upscaleEvent2.isFinalChain());
        assertFalse(upscaleEvent2.getRepair());
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

        UpscaleEvent upscaleEvent3 = (UpscaleEvent) queue.poll();
        assertEquals(OPERATION_ID, upscaleEvent3.getOperationId());
        assertEquals(STACK_ID, upscaleEvent3.getResourceId());
        assertTrue(upscaleEvent3.isChained());
        assertFalse(upscaleEvent3.isFinalChain());
        assertFalse(upscaleEvent3.getRepair());
        assertEquals(UpscaleFlowEvent.UPSCALE_EVENT.event(), upscaleEvent3.selector());
        assertEquals(4, upscaleEvent3.getInstanceCountByGroup());

        DownscaleEvent downscaleEvent3 = (DownscaleEvent) queue.poll();
        assertEquals(OPERATION_ID, downscaleEvent3.getOperationId());
        assertEquals(STACK_ID, downscaleEvent3.getResourceId());
        assertTrue(downscaleEvent3.isChained());
        assertFalse(downscaleEvent3.isFinalChain());
        assertFalse(downscaleEvent3.isRepair());
        assertEquals(DownscaleFlowEvent.DOWNSCALE_EVENT.event(), downscaleEvent3.selector());
        assertEquals(3, downscaleEvent3.getInstanceCountByGroup());
        assertEquals(1, downscaleEvent3.getInstanceIds().size());
        String firstInstanceToDownscale = downscaleEvent3.getInstanceIds().get(0);
        assertTrue(firstInstanceToDownscale.startsWith("repl"));

        assertNotEquals(firstInstanceToDownscale, secondInstanceToDownscale);

        SaltUpdateTriggerEvent saltUpdateTriggerEvent2 = (SaltUpdateTriggerEvent) queue.poll();
        assertEquals(OPERATION_ID, saltUpdateTriggerEvent2.getOperationId());
        assertEquals(STACK_ID, saltUpdateTriggerEvent2.getResourceId());
        assertEquals(SaltUpdateEvent.SALT_UPDATE_EVENT.event(), saltUpdateTriggerEvent2.selector());
        assertTrue(saltUpdateTriggerEvent2.isChained());
        assertTrue(saltUpdateTriggerEvent2.isFinalChain());

        eventQueue.getQueue().addAll(restrainedQueueData);
        FlowChainConfigGraphGeneratorUtil.generateFor(underTest, FLOW_CONFIGS_PACKAGE_NAME, eventQueue);
    }

    @Test
    void testInitEvent() {
        assertEquals(FlowChainTriggers.UPGRADE_TRIGGER_EVENT, underTest.initEvent());
    }

    private Stack createStack(boolean legacySignKey, boolean masterPrivateKeyMissing) {
        Stack stack = new Stack();
        SecurityConfig securityConfig = new SecurityConfig();
        SaltSecurityConfig saltSecurityConfig = new SaltSecurityConfig();
        securityConfig.setSaltSecurityConfig(saltSecurityConfig);
        stack.setSecurityConfig(securityConfig);
        if (legacySignKey) {
            saltSecurityConfig.setSaltSignPublicKey("saltSignPublicKey");
        }
        if (!masterPrivateKeyMissing) {
            saltSecurityConfig.setSaltMasterPrivateKeyVault("saltMasterPrivateKey");
        }
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setInstanceMetaData(Set.of(new InstanceMetaData(), new InstanceMetaData()));
        stack.setInstanceGroups(Set.of(instanceGroup));
        return stack;
    }
}
