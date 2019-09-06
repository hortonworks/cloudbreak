package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleEvent.DECOMMISSION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleEvent.STACK_DOWNSCALE_EVENT;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.common.type.ScalingType;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterDownscaleDetails;
import com.sequenceiq.cloudbreak.core.flow2.event.MultiHostgroupClusterAndStackDownscaleTriggerEvent;

import reactor.rx.Promise;

public class MultiHostgroupDownscaleFlowEventChainFactoryTest {

    private final MultiHostgroupDownscaleFlowEventChainFactory underTest = new MultiHostgroupDownscaleFlowEventChainFactory();

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
        MultiHostgroupClusterAndStackDownscaleTriggerEvent event = new MultiHostgroupClusterAndStackDownscaleTriggerEvent("selector", 1L,
                instanceIdsByHostgroupMap, details, ScalingType.DOWNSCALE_TOGETHER, new Promise<>());
        Queue<Selectable> queue = underTest.createFlowTriggerEventQueue(event);
        assertEquals(4L, queue.size());
        assertEquals(DECOMMISSION_EVENT.event(), queue.poll().selector());
        assertEquals(STACK_DOWNSCALE_EVENT.event(), queue.poll().selector());
        assertEquals(DECOMMISSION_EVENT.event(), queue.poll().selector());
        assertEquals(STACK_DOWNSCALE_EVENT.event(), queue.poll().selector());
    }

}
