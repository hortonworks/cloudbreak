package com.sequenceiq.environment.environment.flow.chain;

import static com.sequenceiq.environment.environment.flow.generator.FlowOfflineStateGraphGenerator.FLOW_CONFIGS_PACKAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.environment.environment.flow.modify.network.event.EnvNetworkCidrsModificationTriggerEvent;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.graph.FlowChainConfigGraphGeneratorUtil;

@ExtendWith(MockitoExtension.class)
class EnvNetworkCidrsModificationFlowEventChainFactoryTest {
    private EnvNetworkCidrsModificationFlowEventChainFactory underTest;

    @Test
    void testFlowChainEventQueueBuildingAndGenerateGraph() {
        underTest = new EnvNetworkCidrsModificationFlowEventChainFactory();
        EnvNetworkCidrsModificationTriggerEvent triggerEvent = EnvNetworkCidrsModificationTriggerEvent.builder()
                .withResourceId(0L)
                .withResourceCrn("resourceCrn")
                .withNetworkCidrs(List.of("10.84.128.0/17", "10.84.0.0/17"))
                .build();
        String actorCrn = CrnTestUtil
                .getUserCrnBuilder()
                .setAccountId("test")
                .setResource("testUser")
                .build()
                .toString();

        FlowTriggerEventQueue flowTriggerEventQueue = ThreadBasedUserCrnProvider.doAs(actorCrn, () -> underTest.createFlowTriggerEventQueue(triggerEvent));

        assertEquals(2, flowTriggerEventQueue.getQueue().size());
        FlowChainConfigGraphGeneratorUtil.generateFor(underTest, FLOW_CONFIGS_PACKAGE, flowTriggerEventQueue);
    }
}