package com.sequenceiq.freeipa.flow.chain;

import static com.sequenceiq.freeipa.flow.graph.FlowOfflineStateGraphGenerator.FLOW_CONFIGS_PACKAGE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.environment.environment.dto.FreeIpaLoadBalancerType;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.graph.FlowChainConfigGraphGeneratorUtil;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.ProvisionTriggerEvent;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerProvisionCondition;

@ExtendWith(MockitoExtension.class)
class ProvisionFlowEventChainFactoryTest {

    @Mock
    private FreeIpaLoadBalancerProvisionCondition loadBalancerProvisionCondition;

    @InjectMocks
    private ProvisionFlowEventChainFactory underTest;

    @Test
    void createFlowTriggerEventQueue() {
        when(loadBalancerProvisionCondition.loadBalancerProvisionEnabled(any(), any())).thenReturn(true);
        ProvisionTriggerEvent triggerEvent = new ProvisionTriggerEvent(FlowChainTriggers.PROVISION_TRIGGER_EVENT, 0L, FreeIpaLoadBalancerType.INTERNAL_NLB);

        FlowTriggerEventQueue flowTriggerEventQueue = underTest.createFlowTriggerEventQueue(triggerEvent);

        assertNotNull(flowTriggerEventQueue);
        assertEquals(3, flowTriggerEventQueue.getQueue().size());
        FlowChainConfigGraphGeneratorUtil.generateFor(underTest, FLOW_CONFIGS_PACKAGE_NAME, flowTriggerEventQueue);
    }
}