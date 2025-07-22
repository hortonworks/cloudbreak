package com.sequenceiq.cloudbreak.core.flow2.chain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.Map;
import java.util.Queue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskUpdateRequest;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.event.CoreProviderTemplateUpdateEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.event.CoreRootVolumeUpdateTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterRepairTriggerEvent;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.core.chain.init.flowevents.FlowChainInitPayload;

@ExtendWith(MockitoExtension.class)
public class CoreRootVolumeUpdateFlowEventChainFactoryTest {

    private static final Long STACK_ID = 1L;

    @InjectMocks
    private CoreRootVolumeUpdateFlowEventChainFactory underTest;

    @Test
    void testInitEvent() {
        assertEquals(FlowChainTriggers.CORE_ROOT_VOLUME_UPDATE_TRIGGER_EVENT, underTest.initEvent());
    }

    @Test
    void testCreateFlowTriggerEventQueue() {
        DiskUpdateRequest diskUpdateRequest = new DiskUpdateRequest();
        diskUpdateRequest.setDiskType(DiskType.ADDITIONAL_DISK);
        diskUpdateRequest.setGroup("executor");
        diskUpdateRequest.setVolumeType("gp2");
        diskUpdateRequest.setSize(100);
        CoreRootVolumeUpdateTriggerEvent event =
                new CoreRootVolumeUpdateTriggerEvent(
                        FlowChainTriggers.CORE_ROOT_VOLUME_UPDATE_TRIGGER_EVENT, STACK_ID,
                        Map.of(),
                        "gp2",
                        100,
                        "executor",
                        DiskType.ADDITIONAL_DISK.name()
                );
        FlowTriggerEventQueue result = underTest.createFlowTriggerEventQueue(event);
        assertEquals("CoreRootVolumeUpdateFlowEventChainFactory", result.getFlowChainName());
        assertEquals(event, result.getTriggerEvent());
        Queue<Selectable> resultQueue = result.getQueue();
        assertEquals(3, resultQueue.size());
        assertInstanceOf(FlowChainInitPayload.class, resultQueue.poll());
        assertInstanceOf(CoreProviderTemplateUpdateEvent.class, resultQueue.poll());
        assertInstanceOf(ClusterRepairTriggerEvent.class, resultQueue.poll());
    }
}
