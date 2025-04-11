package com.sequenceiq.freeipa.flow.chain;

import static com.sequenceiq.freeipa.flow.chain.FlowChainTriggers.ROOT_VOLUME_UPDATE_TRIGGER_EVENT;
import static com.sequenceiq.freeipa.flow.graph.FlowOfflineStateGraphGenerator.FLOW_CONFIGS_PACKAGE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.core.chain.init.flowevents.FlowChainInitPayload;
import com.sequenceiq.flow.graph.FlowChainConfigGraphGeneratorUtil;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.DownscaleEvent;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.event.ChangePrimaryGatewayEvent;
import com.sequenceiq.freeipa.flow.freeipa.rootvolumeupdate.event.FreeIpaProviderTemplateUpdateEvent;
import com.sequenceiq.freeipa.flow.freeipa.rootvolumeupdate.event.RootVolumeUpdateEvent;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleEvent;

@ExtendWith(MockitoExtension.class)
class RootVolumeUpdateFlowEventChainFactoryTest {

    private static final Long STACK_ID = 1L;

    private static final String OPERATION_ID = "operation-id";

    @InjectMocks
    private RootVolumeUpdateFlowEventChainFactory underTest;

    @Test
    void testInitEvent() {
        assertEquals(ROOT_VOLUME_UPDATE_TRIGGER_EVENT, underTest.initEvent());
    }

    @Test
    void testCreateFlowTriggerEventQueue() {
        RootVolumeUpdateEvent event = new RootVolumeUpdateEvent(ROOT_VOLUME_UPDATE_TRIGGER_EVENT, STACK_ID, OPERATION_ID, 1,
                List.of("test-instance-id-1"), "test-instance-id-1");
        FlowTriggerEventQueue result = underTest.createFlowTriggerEventQueue(event);
        assertEquals("RootVolumeUpdateFlowEventChainFactory", result.getFlowChainName());
        assertEquals(event, result.getTriggerEvent());
        Queue<Selectable> resultQueue = result.getQueue();
        Queue<Selectable> restrainedQueueData = new ConcurrentLinkedQueue<>(resultQueue);
        assertEquals(5, resultQueue.size());
        assertInstanceOf(FlowChainInitPayload.class, resultQueue.poll());
        assertInstanceOf(FreeIpaProviderTemplateUpdateEvent.class, resultQueue.poll());
        assertInstanceOf(UpscaleEvent.class, resultQueue.poll());
        assertInstanceOf(ChangePrimaryGatewayEvent.class, resultQueue.poll());
        assertInstanceOf(DownscaleEvent.class, resultQueue.poll());

        result.getQueue().addAll(restrainedQueueData);
        FlowChainConfigGraphGeneratorUtil.generateFor(underTest, FLOW_CONFIGS_PACKAGE_NAME, result);
    }
}
