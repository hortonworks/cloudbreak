package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleEvent.DECOMMISSION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.generator.FlowOfflineStateGraphGenerator.FLOW_CONFIGS_PACKAGE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleEvent.STACK_DOWNSCALE_EVENT;
import static com.sequenceiq.flow.core.chain.finalize.config.FlowChainFinalizeEvent.FLOWCHAIN_FINALIZE_TRIGGER_EVENT;
import static com.sequenceiq.flow.core.chain.init.config.FlowChainInitEvent.FLOWCHAIN_INIT_TRIGGER_EVENT;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.type.ScalingType;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterDownscaleDetails;
import com.sequenceiq.cloudbreak.core.flow2.event.MultiHostgroupClusterAndStackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.graph.FlowChainConfigGraphGeneratorUtil;

@ExtendWith(MockitoExtension.class)
public class MultiHostgroupDownscaleFlowEventChainFactoryTest {

    private static final Long STACK_ID = 198L;

    @InjectMocks
    private MultiHostgroupDownscaleFlowEventChainFactory underTest;

    @Mock
    private StackService stackService;

    @Mock
    private CloudPlatformVariant cloudPlatformVariant;

    @Test
    public void testInitEvent() {
        assertEquals(FlowChainTriggers.FULL_DOWNSCALE_MULTIHOSTGROUP_TRIGGER_EVENT, underTest.initEvent());
    }

    @Test
    public void testCreateFlowWithTwoHostgroup() {
        Map<String, Set<Long>> instanceIdsByHostgroupMap = new HashMap<>();
        instanceIdsByHostgroupMap.put("firstGroup", Sets.newHashSet(1L, 2L));
        instanceIdsByHostgroupMap.put("secondGroup", Sets.newHashSet(3L, 4L));
        ClusterDownscaleDetails details = new ClusterDownscaleDetails();
        MultiHostgroupClusterAndStackDownscaleTriggerEvent event = new MultiHostgroupClusterAndStackDownscaleTriggerEvent("selector", STACK_ID,
                instanceIdsByHostgroupMap, details, ScalingType.DOWNSCALE_TOGETHER, new Promise<>());

        when(stackService.getPlatformVariantByStackId(STACK_ID)).thenReturn(cloudPlatformVariant);
        when(cloudPlatformVariant.getVariant()).thenReturn(Variant.variant("AWS"));

        FlowTriggerEventQueue flowTriggerQueue = underTest.createFlowTriggerEventQueue(event);

        Queue<Selectable> queue = flowTriggerQueue.getQueue();
        Queue<Selectable> restrainedQueueData = new ConcurrentLinkedQueue<>(queue);
        assertEquals(4L, queue.size());
        assertEquals(FLOWCHAIN_INIT_TRIGGER_EVENT.event(), queue.poll().selector());
        assertEquals(DECOMMISSION_EVENT.event(), queue.poll().selector());
        assertEquals(STACK_DOWNSCALE_EVENT.event(), queue.poll().selector());
        assertEquals(FLOWCHAIN_FINALIZE_TRIGGER_EVENT.event(), queue.poll().selector());
        flowTriggerQueue.getQueue().addAll(restrainedQueueData);
        FlowChainConfigGraphGeneratorUtil.generateFor(underTest, FLOW_CONFIGS_PACKAGE, flowTriggerQueue, "FULL_DOWNSCALE");
    }

}
