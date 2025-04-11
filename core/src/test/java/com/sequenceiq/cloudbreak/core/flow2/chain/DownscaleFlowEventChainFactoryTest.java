package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleEvent.DECOMMISSION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.generator.FlowOfflineStateGraphGenerator.FLOW_CONFIGS_PACKAGE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleEvent.STACK_DOWNSCALE_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.type.ScalingType;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterAndStackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.graph.FlowChainConfigGraphGeneratorUtil;

@ExtendWith(MockitoExtension.class)
class DownscaleFlowEventChainFactoryTest {

    @Mock
    private StackService stackService;

    @InjectMocks
    private DownscaleFlowEventChainFactory underTest;

    @Test
    void initEvent() {
        assertEquals(FlowChainTriggers.FULL_DOWNSCALE_TRIGGER_EVENT, underTest.initEvent());
    }

    @Test
    void createFlowTriggerEventQueue() {
        long stackId = 0L;
        ClusterAndStackDownscaleTriggerEvent event = new ClusterAndStackDownscaleTriggerEvent("selector", stackId, Map.of(), ScalingType.DOWNSCALE_TOGETHER);
        BDDMockito.when(stackService.getViewByIdWithoutAuth(eq(stackId))).thenReturn(mock(StackView.class));

        FlowTriggerEventQueue flowTriggerQueue = underTest.createFlowTriggerEventQueue(event);

        Queue<Selectable> queue = flowTriggerQueue.getQueue();
        Queue<Selectable> restrainedQueueData = new ConcurrentLinkedQueue<>(queue);
        assertEquals(2L, queue.size());
        assertEquals(DECOMMISSION_EVENT.event(), queue.poll().selector());
        assertEquals(STACK_DOWNSCALE_EVENT.event(), queue.poll().selector());
        flowTriggerQueue.getQueue().addAll(restrainedQueueData);
        FlowChainConfigGraphGeneratorUtil.generateFor(underTest, FLOW_CONFIGS_PACKAGE, flowTriggerQueue);
    }
}