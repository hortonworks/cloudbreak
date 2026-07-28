package com.sequenceiq.freeipa.flow.chain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.core.chain.finalize.flowevents.FlowChainFinalizePayload;
import com.sequenceiq.flow.core.chain.init.flowevents.FlowChainInitPayload;
import com.sequenceiq.flow.graph.FlowChainConfigGraphGeneratorUtil;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.Template;
import com.sequenceiq.freeipa.flow.freeipa.rollingvscale.event.FreeIpaRollingVerticalScaleChainTriggerEvent;
import com.sequenceiq.freeipa.flow.freeipa.rollingvscale.event.FreeIpaRollingVerticalScaleTriggerEvent;
import com.sequenceiq.freeipa.flow.freeipa.verticalscale.FreeIpaVerticalScaleService;
import com.sequenceiq.freeipa.flow.freeipa.verticalscale.model.FreeIpaVerticalScaleParameters;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class FreeIpaRollingVerticalScaleFlowEventChainFactoryTest {

    private static final long STACK_ID = 42L;

    @Mock
    private StackService stackService;

    @Mock
    private FreeIpaVerticalScaleService freeIpaVerticalScaleService;

    @Mock
    private Stack stack;

    @InjectMocks
    private FreeIpaRollingVerticalScaleFlowEventChainFactory underTest;

    @Test
    void test3NodeClusterReplicasBeforePrimaryGateway() {
        InstanceMetaData replicaA = createInstance("i-aaa", "a.host", InstanceMetadataType.GATEWAY);
        InstanceMetaData replicaB = createInstance("i-bbb", "b.host", InstanceMetadataType.GATEWAY);
        InstanceMetaData primary = createInstance("i-pgw", "c.host", InstanceMetadataType.GATEWAY_PRIMARY);

        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(Set.of(replicaA, replicaB, primary));
        mockInstanceGroupWithType("master", "m5.small");

        FreeIpaVerticalScaleParameters scaleConfig = new FreeIpaVerticalScaleParameters("master", "m5.xlarge", null, null);
        FreeIpaRollingVerticalScaleChainTriggerEvent chainEvent =
                new FreeIpaRollingVerticalScaleChainTriggerEvent(STACK_ID, scaleConfig, null);

        FlowTriggerEventQueue result = underTest.createFlowTriggerEventQueue(chainEvent);

        Queue<Selectable> queue = result.getQueue();
        assertEquals(5, queue.size());

        List<Selectable> events = new ArrayList<>(queue);

        assertInstanceOf(FlowChainInitPayload.class, events.get(0));

        FreeIpaRollingVerticalScaleTriggerEvent first = (FreeIpaRollingVerticalScaleTriggerEvent) events.get(1);
        FreeIpaRollingVerticalScaleTriggerEvent second = (FreeIpaRollingVerticalScaleTriggerEvent) events.get(2);
        FreeIpaRollingVerticalScaleTriggerEvent third = (FreeIpaRollingVerticalScaleTriggerEvent) events.get(3);

        // Primary gateway is always last among trigger events
        assertEquals("c.host", instanceFqdn(third, replicaA, replicaB, primary));

        // The two replicas appear before the primary
        String firstFqdn = instanceFqdn(first, replicaA, replicaB, primary);
        String secondFqdn = instanceFqdn(second, replicaA, replicaB, primary);
        assertTrue("a.host".equals(firstFqdn) || "b.host".equals(firstFqdn));
        assertTrue("a.host".equals(secondFqdn) || "b.host".equals(secondFqdn));
        assertNotEquals(firstFqdn, secondFqdn);

        assertInstanceOf(FlowChainFinalizePayload.class, events.get(4));
    }

    @Test
    void test1NodeClusterHasInitTriggerAndFinalize() {
        InstanceMetaData primary = createInstance("i-pgw", "single.host", InstanceMetadataType.GATEWAY_PRIMARY);

        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(Set.of(primary));
        mockInstanceGroupWithType("master", "m5.small");

        FreeIpaVerticalScaleParameters scaleConfig = new FreeIpaVerticalScaleParameters("master", "m5.xlarge", null, null);
        FreeIpaRollingVerticalScaleChainTriggerEvent chainEvent =
                new FreeIpaRollingVerticalScaleChainTriggerEvent(STACK_ID, scaleConfig, null);

        FlowTriggerEventQueue result = underTest.createFlowTriggerEventQueue(chainEvent);

        Queue<Selectable> queue = result.getQueue();
        assertEquals(3, queue.size());

        List<Selectable> events = new ArrayList<>(queue);
        assertInstanceOf(FlowChainInitPayload.class, events.get(0));
        assertInstanceOf(FreeIpaRollingVerticalScaleTriggerEvent.class, events.get(1));
        assertInstanceOf(FlowChainFinalizePayload.class, events.get(2));
    }

    @Test
    void testVerticalScaleRequestIsPropagatedToAllEvents() {
        InstanceMetaData replicaA = createInstance("i-aaa", "a.host", InstanceMetadataType.GATEWAY);
        InstanceMetaData replicaB = createInstance("i-bbb", "b.host", InstanceMetadataType.GATEWAY);
        InstanceMetaData primary = createInstance("i-pgw", "c.host", InstanceMetadataType.GATEWAY_PRIMARY);

        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(Set.of(replicaA, replicaB, primary));
        mockInstanceGroupWithType("master", "m5.small");

        FreeIpaVerticalScaleParameters scaleConfig = new FreeIpaVerticalScaleParameters("master", "m5.xlarge", null, null);
        FreeIpaRollingVerticalScaleChainTriggerEvent chainEvent =
                new FreeIpaRollingVerticalScaleChainTriggerEvent(STACK_ID, scaleConfig, null);

        FlowTriggerEventQueue result = underTest.createFlowTriggerEventQueue(chainEvent);

        List<Selectable> events = new ArrayList<>(result.getQueue());
        for (int i = 1; i < events.size() - 1; i++) {
            FreeIpaRollingVerticalScaleTriggerEvent triggerEvent = (FreeIpaRollingVerticalScaleTriggerEvent) events.get(i);
            assertEquals("m5.xlarge", triggerEvent.getScaleConfig().getTargetInstanceType());
            assertEquals("m5.small", triggerEvent.getScaleConfig().getOriginalInstanceType());
        }
    }

    @Test
    void testInitEvent() {
        assertEquals(FlowChainTriggers.FREEIPA_ROLLING_VERTICAL_SCALE_CHAIN_TRIGGER_EVENT, underTest.initEvent());
    }

    @Test
    void testGenerateFlowChainGraph() {
        InstanceMetaData replicaA = createInstance("i-aaa", "a.host", InstanceMetadataType.GATEWAY);
        InstanceMetaData primary = createInstance("i-pgw", "c.host", InstanceMetadataType.GATEWAY_PRIMARY);

        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(Set.of(replicaA, primary));
        mockInstanceGroupWithType("master", "m5.small");

        FreeIpaVerticalScaleParameters scaleConfig = new FreeIpaVerticalScaleParameters("master", "m5.xlarge", null, null);
        FreeIpaRollingVerticalScaleChainTriggerEvent chainEvent =
                new FreeIpaRollingVerticalScaleChainTriggerEvent(STACK_ID, scaleConfig, null);

        FlowTriggerEventQueue result = underTest.createFlowTriggerEventQueue(chainEvent);
        FlowChainConfigGraphGeneratorUtil.generateFor(underTest,
                "com.sequenceiq.freeipa.flow", result);
    }

    private InstanceMetaData createInstance(String instanceId, String fqdn, InstanceMetadataType type) {
        InstanceMetaData instance = new InstanceMetaData();
        instance.setInstanceId(instanceId);
        instance.setDiscoveryFQDN(fqdn);
        instance.setInstanceMetadataType(type);
        return instance;
    }

    private void mockInstanceGroupWithType(String groupName, String instanceType) {
        Template template = new Template();
        template.setInstanceType(instanceType);
        InstanceGroup ig = new InstanceGroup();
        ig.setGroupName(groupName);
        ig.setTemplate(template);
        when(stack.getInstanceGroups()).thenReturn(Set.of(ig));
    }

    /**
     * Resolves the FQDN of a trigger event by looking up the instanceId against the provided instances.
     */
    private String instanceFqdn(FreeIpaRollingVerticalScaleTriggerEvent event, InstanceMetaData... instances) {
        String instanceId = event.getInstanceId();
        for (InstanceMetaData im : instances) {
            if (instanceId.equals(im.getInstanceId())) {
                return im.getDiscoveryFQDN();
            }
        }
        throw new IllegalArgumentException("Unknown instanceId in trigger event: " + instanceId);
    }
}
