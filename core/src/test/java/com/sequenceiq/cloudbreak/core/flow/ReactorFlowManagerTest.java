package com.sequenceiq.cloudbreak.core.flow;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.model.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.cloud.Acceptable;
import com.sequenceiq.cloudbreak.core.flow2.service.ErrorHandlerAwareFlowEventFactory;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
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
    private ErrorHandlerAwareFlowEventFactory eventFactory;

    @InjectMocks
    private ReactorFlowManager flowManager;

    private Long stackId = 1L;

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
        when(eventFactory.createEvent(anyObject())).thenReturn(new Event<>(acceptable));
    }

    @Test
    public void shouldReturnTheNextFailureTransition() {
        InstanceGroupAdjustmentJson instanceGroupAdjustment = new InstanceGroupAdjustmentJson();
        HostGroupAdjustmentJson hostGroupAdjustment = new HostGroupAdjustmentJson();

        flowManager.triggerProvisioning(stackId);
        flowManager.triggerClusterInstall(stackId);
        flowManager.triggerClusterReInstall(stackId);
        flowManager.triggerStackStop(stackId);
        flowManager.triggerStackStart(stackId);
        flowManager.triggerClusterStop(stackId);
        flowManager.triggerClusterStart(stackId);
        flowManager.triggerTermination(stackId);
        flowManager.triggerForcedTermination(stackId);
        flowManager.triggerStackUpscale(stackId, instanceGroupAdjustment);
        flowManager.triggerStackDownscale(stackId, instanceGroupAdjustment);
        flowManager.triggerStackRemoveInstance(stackId, "instanceId");
        flowManager.triggerClusterUpscale(stackId, hostGroupAdjustment);
        flowManager.triggerClusterDownscale(stackId, hostGroupAdjustment);
        flowManager.triggerClusterSync(stackId);
        flowManager.triggerStackSync(stackId);
        flowManager.triggerFullSync(stackId);
        flowManager.triggerClusterCredentialReplace(stackId, "admin", "admin1");
        flowManager.triggerClusterCredentialUpdate(stackId, "admin1");
        flowManager.triggerClusterTermination(stackId);
        flowManager.triggerClusterUpgrade(stackId);
        flowManager.triggerManualRepairFlow(stackId);
        flowManager.triggerStackRepairFlow(stackId, new UnhealthyInstances());

        int count = 0;
        for (Method method : flowManager.getClass().getDeclaredMethods()) {
            if (method.getName().startsWith("trigger")) {
                count++;
            }
        }
        // Termination triggers flow cancellation
        count += 2;
        verify(reactor, times(count)).notify((Object) anyObject(), any(Event.class));
    }
}
