package com.sequenceiq.cloudbreak.core.flow;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.model.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.cloud.Acceptable;
import com.sequenceiq.cloudbreak.cloud.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainTriggers;
import com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationEvent;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.repair.UnhealthyInstances;

import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.core.dispatch.ThreadPoolExecutorDispatcher;
import reactor.rx.Promise;

@RunWith(MockitoJUnitRunner.class)
public class ReactorFlowManagerTest {

    @Mock
    private EventBus reactor;

    @Mock
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @Mock
    private StackService stackService;

    @InjectMocks
    private ReactorFlowManager underTest;

    private final Long stackId = 1L;

    @Before
    public void setUp() {
        reset(reactor);
        reset(eventFactory);
        when(reactor.notify((Object) anyObject(), any(Event.class))).thenReturn(new EventBus(new ThreadPoolExecutorDispatcher(1, 1)));
        Acceptable acceptable = new Acceptable() {
            @Override
            public Promise<Boolean> accepted() {
                Promise<Boolean> a = new Promise<>();
                a.accept(true);
                return a;
            }

            @Override
            public Long getStackId() {
                return stackId;
            }
        };
        Stack stack = TestUtil.stack();
        stack.setCluster(TestUtil.cluster());
        when(stackService.get(anyLong())).thenReturn(stack);
        when(stackService.getById(anyLong())).thenReturn(TestUtil.stack());
        when(stackService.getByIdView(anyLong())).thenReturn(TestUtil.stackView());
        when(eventFactory.createEventWithErrHandler(anyObject())).thenReturn(new Event<>(acceptable));
    }

    @Test
    public void shouldReturnTheNextFailureTransition() {
        InstanceGroupAdjustmentJson instanceGroupAdjustment = new InstanceGroupAdjustmentJson();
        HostGroupAdjustmentJson hostGroupAdjustment = new HostGroupAdjustmentJson();

        underTest.triggerProvisioning(stackId);
        underTest.triggerClusterInstall(stackId);
        underTest.triggerClusterReInstall(stackId);
        underTest.triggerStackStop(stackId);
        underTest.triggerStackStart(stackId);
        underTest.triggerClusterStop(stackId);
        underTest.triggerClusterStart(stackId);
        underTest.triggerTermination(stackId, false, false);
        underTest.triggerTermination(stackId, false, true);
        underTest.triggerStackUpscale(stackId, instanceGroupAdjustment, true);
        underTest.triggerStackDownscale(stackId, instanceGroupAdjustment);
        underTest.triggerStackRemoveInstance(stackId, "hostgroup", 5L);
        underTest.triggerClusterUpscale(stackId, hostGroupAdjustment);
        underTest.triggerClusterDownscale(stackId, hostGroupAdjustment);
        underTest.triggerClusterSync(stackId);
        underTest.triggerStackSync(stackId);
        underTest.triggerFullSync(stackId);
        underTest.triggerClusterCredentialReplace(stackId, "admin", "admin1");
        underTest.triggerClusterCredentialUpdate(stackId, "admin1");
        underTest.triggerClusterTermination(stackId, false, false);
        underTest.triggerClusterTermination(stackId, true, false);
        underTest.triggerClusterUpgrade(stackId);
        underTest.triggerManualRepairFlow(stackId);
        underTest.triggerStackRepairFlow(stackId, new UnhealthyInstances());
        underTest.triggerClusterRepairFlow(stackId, new HashMap<>(), true);
        underTest.triggerEphemeralUpdate(stackId);

        // Not start from 0 because flow cancellations
        int count = 5;
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

        verify(reactor).notify(eq(ClusterTerminationEvent.TERMINATION_EVENT.event()), any(Event.class));
    }

    @Test
    public void testClusterTerminationOnlySecuredCluster() {
        Stack stack = TestUtil.stack();
        stack.setCluster(TestUtil.cluster());
        stack.getCluster().setSecure(true);
        when(stackService.get(anyLong())).thenReturn(stack);

        underTest.triggerClusterTermination(1L, false, false);

        verify(reactor).notify(eq(ClusterTerminationEvent.PROPER_TERMINATION_EVENT.event()), any(Event.class));
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
        stack.getCluster().setSecure(true);
        when(stackService.get(anyLong())).thenReturn(stack);

        underTest.triggerClusterTermination(1L, true, false);

        verify(reactor).notify(eq(FlowChainTriggers.PROPER_TERMINATION_TRIGGER_EVENT), any(Event.class));
    }
}
