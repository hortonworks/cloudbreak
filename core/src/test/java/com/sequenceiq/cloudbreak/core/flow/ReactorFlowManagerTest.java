package com.sequenceiq.cloudbreak.core.flow;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.HostGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.InstanceGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.cloud.Acceptable;
import com.sequenceiq.cloudbreak.cloud.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.cloudbreak.controller.exception.CloudbreakApiException;
import com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainTriggers;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.repair.UnhealthyInstances;

import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.core.dispatch.ThreadPoolExecutorDispatcher;
import reactor.rx.Promise;

@RunWith(MockitoJUnitRunner.class)
public class ReactorFlowManagerTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private EventBus reactor;

    @Mock
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @Mock
    private StackService stackService;

    @InjectMocks
    private ReactorFlowManager underTest;

    @Before
    public void setUp() {
        reset(reactor);
        reset(eventFactory);
        when(reactor.notify((Object) anyObject(), any(Event.class))).thenReturn(new EventBus(new ThreadPoolExecutorDispatcher(1, 1)));
        Acceptable acceptable = new TestAcceptable();
        Stack stack = TestUtil.stack();
        stack.setCluster(TestUtil.cluster());
        when(stackService.getByIdWithTransaction(anyLong())).thenReturn(stack);
        when(stackService.getByIdWithListsInTransaction(anyLong())).thenReturn(stack);
        when(eventFactory.createEventWithErrHandler(anyObject())).thenReturn(new Event<>(acceptable));
    }

    @Test
    public void shouldReturnTheNextFailureTransition() {
        InstanceGroupAdjustmentV4Request instanceGroupAdjustment = new InstanceGroupAdjustmentV4Request();
        HostGroupAdjustmentV4Request hostGroupAdjustment = new HostGroupAdjustmentV4Request();
        Map<String, Set<Long>> instanceIdsByHostgroup = new HashMap<>();
        instanceIdsByHostgroup.put("hostrgroup", Collections.singleton(1L));

        underTest.triggerProvisioning(STACK_ID);
        underTest.triggerClusterInstall(STACK_ID);
        underTest.triggerClusterReInstall(STACK_ID);
        underTest.triggerStackStop(STACK_ID);
        underTest.triggerStackStart(STACK_ID);
        underTest.triggerClusterStop(STACK_ID);
        underTest.triggerClusterStart(STACK_ID);
        underTest.triggerTermination(STACK_ID, false, false);
        underTest.triggerTermination(STACK_ID, false, true);
        underTest.triggerStackUpscale(STACK_ID, instanceGroupAdjustment, true);
        underTest.triggerStackDownscale(STACK_ID, instanceGroupAdjustment);
        underTest.triggerStackRemoveInstance(STACK_ID, "hostgroup", 5L);
        underTest.triggerStackRemoveInstance(STACK_ID, "hostgroup", 5L, false);
        underTest.triggerStackRemoveInstances(STACK_ID, instanceIdsByHostgroup);
        underTest.triggerClusterUpscale(STACK_ID, hostGroupAdjustment);
        underTest.triggerClusterDownscale(STACK_ID, hostGroupAdjustment);
        underTest.triggerClusterSync(STACK_ID);
        underTest.triggerClusterSyncWithoutCheck(STACK_ID);
        underTest.triggerStackSync(STACK_ID);
        underTest.triggerFullSync(STACK_ID);
        underTest.triggerFullSyncWithoutCheck(STACK_ID);
        underTest.triggerClusterCredentialReplace(STACK_ID, "admin", "admin1");
        underTest.triggerClusterCredentialUpdate(STACK_ID, "admin1");
        underTest.triggerClusterTermination(STACK_ID, false, false);
        underTest.triggerClusterTermination(STACK_ID, true, false);
        underTest.triggerClusterUpgrade(STACK_ID);
        underTest.triggerManualRepairFlow(STACK_ID);
        underTest.triggerStackRepairFlow(STACK_ID, new UnhealthyInstances());
        underTest.triggerClusterRepairFlow(STACK_ID, new HashMap<>(), true);
        underTest.triggerEphemeralUpdate(STACK_ID);
        underTest.triggerStackImageUpdate(STACK_ID, "asdf", null, null);
        underTest.triggerMaintenanceModeValidationFlow(STACK_ID);

        // Not start from 0 because flow cancellations
        int count = 6;
        for (Method method : underTest.getClass().getDeclaredMethods()) {
            if (method.getName().startsWith("trigger")) {
                count++;
            }
        }
        verify(reactor, times(count)).notify((Object) anyObject(), any(Event.class));
    }

    @Test
    public void testClusterTerminationOnlyNotSecuredCluster() {
        underTest.triggerClusterTermination(1L, false, false);

        verify(reactor).notify(eq(FlowChainTriggers.TERMINATION_TRIGGER_EVENT), any(Event.class));
    }

    @Test
    public void testClusterTerminationOnlySecuredCluster() {
        Stack stack = TestUtil.stack();
        stack.setCluster(TestUtil.cluster());
        stack.getCluster().setKerberosConfig(new KerberosConfig());
        when(stackService.getByIdWithTransaction(anyLong())).thenReturn(stack);

        underTest.triggerClusterTermination(1L, false, false);

        verify(reactor).notify(eq(FlowChainTriggers.PROPER_TERMINATION_TRIGGER_EVENT), any(Event.class));
    }

    @Test
    public void testClusterTerminationNotSecuredClusterAndStack() {
        underTest.triggerClusterTermination(1L, true, false);

        verify(reactor).notify(eq(FlowChainTriggers.TERMINATION_TRIGGER_EVENT), any(Event.class));
    }

    @Test
    public void testClusterTerminationSecuredClusterAndStack() {
        Stack stack = TestUtil.stack();
        stack.setCluster(TestUtil.cluster());
        stack.getCluster().setKerberosConfig(new KerberosConfig());
        when(stackService.getByIdWithTransaction(anyLong())).thenReturn(stack);

        underTest.triggerClusterTermination(1L, true, false);

        verify(reactor).notify(eq(FlowChainTriggers.PROPER_TERMINATION_TRIGGER_EVENT), any(Event.class));
    }

    @Test
    public void testClusterUpscaleInMaintenanceMode() {
        Stack stack = TestUtil.stack();
        stack.setCluster(TestUtil.cluster());
        stack.getCluster().setStatus(Status.MAINTENANCE_MODE_ENABLED);
        when(stackService.getByIdWithTransaction(1L)).thenReturn(stack);

        InstanceGroupAdjustmentV4Request instGroupAdjustment = new InstanceGroupAdjustmentV4Request();
        try {
            underTest.triggerStackUpscale(1L, instGroupAdjustment, false);
        } catch (CloudbreakApiException e) {
            assertEquals("Operation not allowed in maintenance mode.", e.getMessage());
        }

        verify(reactor, never()).notify(eq(FlowChainTriggers.FULL_UPSCALE_TRIGGER_EVENT), any(Event.class));
    }

    @Test
    public void testtriggerStackImageUpdate() {
        long stackId = 1L;
        String imageID = "imageID";
        String imageCatalogName = "imageCatalogName";
        String imageCatalogUrl = "imageCatalogUrl";
        underTest.triggerStackImageUpdate(stackId, imageID, imageCatalogName, imageCatalogUrl);
        verify(reactor).notify(eq(FlowChainTriggers.STACK_IMAGE_UPDATE_TRIGGER_EVENT), any(Event.class));
    }

    @Test
    public void testTriggerMaintenanceModeValidationFlow() {
        long stackId = 1L;
        underTest.triggerMaintenanceModeValidationFlow(stackId);
        verify(reactor).notify(eq(FlowChainTriggers.CLUSTER_MAINTENANCE_MODE_VALIDATION_TRIGGER_EVENT), any(Event.class));
    }

    private static class TestAcceptable implements Acceptable {
        @Override
        public Promise<Boolean> accepted() {
            Promise<Boolean> a = new Promise<>();
            a.accept(true);
            return a;
        }

        @Override
        public Long getStackId() {
            return STACK_ID;
        }
    }
}
