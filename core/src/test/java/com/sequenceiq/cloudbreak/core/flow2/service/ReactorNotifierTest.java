package com.sequenceiq.cloudbreak.core.flow2.service;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.sequenceiq.cloudbreak.exception.CloudbreakApiException;

public class ReactorNotifierTest {

    @Test
    public void testClusterUpscaleInMaintenanceMode() {
//        Stack stack = TestUtil.stack();
//        stack.setCluster(TestUtil.cluster());
//        stack.getCluster().setStatus(Status.MAINTENANCE_MODE_ENABLED);
//        when(stackService.getByIdWithTransaction(1L)).thenReturn(stack);

//        InstanceGroupAdjustmentV4Request instGroupAdjustment = new InstanceGroupAdjustmentV4Request();
        try {
//            underTest.triggerStackUpscale(1L, instGroupAdjustment, false);
        } catch (CloudbreakApiException e) {
            assertEquals("Operation not allowed in maintenance mode.", e.getMessage());
        }
//        verify(reactor, never()).notify(eq(FlowChainTriggers.FULL_UPSCALE_TRIGGER_EVENT), any(Event.class));
    }
}