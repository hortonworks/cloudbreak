package com.sequenceiq.cloudbreak.core.flow;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
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
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.core.flow2.service.ErrorHandlerAwareFlowEventFactory;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.service.cluster.event.ClusterDeleteRequest;
import com.sequenceiq.cloudbreak.service.cluster.event.ClusterUserNamePasswordUpdateRequest;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionRequest;
import com.sequenceiq.cloudbreak.service.stack.event.RemoveInstanceRequest;
import com.sequenceiq.cloudbreak.service.stack.event.StackDeleteRequest;
import com.sequenceiq.cloudbreak.service.stack.event.StackForcedDeleteRequest;

import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.core.dispatch.ThreadPoolExecutorDispatcher;

@RunWith(MockitoJUnitRunner.class)
public class ReactorFlowManagerTest {
    private static final Platform GCP_PLATFORM = Platform.platform(CloudConstants.GCP);

    @Mock
    private EventBus reactor;

    @Mock
    private ErrorHandlerAwareFlowEventFactory eventFactory;

    @InjectMocks
    private ReactorFlowManager flowManager;

    @Before
    public void setUp() throws Exception {
        reset(reactor);
        reset(eventFactory);
        when(reactor.notify((Object) anyObject(), any(Event.class))).thenReturn(new EventBus(new ThreadPoolExecutorDispatcher(1, 1)));
        when(eventFactory.createEvent(anyObject(), anyString())).thenReturn(new Event<Object>(String.class));
    }

    @Test
    public void shouldReturnTheNextFailureTransition() throws Exception {
        Long stackId = 1L;
        ProvisionRequest provisionRequest = new ProvisionRequest(GCP_PLATFORM, 1L);
        StackDeleteRequest stackDeleteRequest = new StackDeleteRequest(GCP_PLATFORM, 1L);
        StackForcedDeleteRequest stackForcedDeleteRequest = new StackForcedDeleteRequest(GCP_PLATFORM, 1L);
        RemoveInstanceRequest removeInstanceRequest = new RemoveInstanceRequest(GCP_PLATFORM, 1L, "instanceId");
        ClusterUserNamePasswordUpdateRequest clusterUserNamePasswordUpdateRequest =
                new ClusterUserNamePasswordUpdateRequest(1L, "admin", "admin1", GCP_PLATFORM);
        ClusterDeleteRequest clusterDeleteRequest = new ClusterDeleteRequest(1L, GCP_PLATFORM, 1L);
        InstanceGroupAdjustmentJson instanceGroupAdjustment = new InstanceGroupAdjustmentJson();
        HostGroupAdjustmentJson hostGroupAdjustment = new HostGroupAdjustmentJson();

        flowManager.triggerProvisioning(stackId);
        flowManager.triggerClusterInstall(stackId);
        flowManager.triggerClusterReInstall(provisionRequest);
        flowManager.triggerStackStop(stackId);
        flowManager.triggerStackStart(stackId);
        flowManager.triggerClusterStop(stackId);
        flowManager.triggerClusterStart(stackId);
        flowManager.triggerTermination(stackDeleteRequest);
        flowManager.triggerForcedTermination(stackForcedDeleteRequest);
        flowManager.triggerStackUpscale(stackId, instanceGroupAdjustment);
        flowManager.triggerStackDownscale(stackId, instanceGroupAdjustment);
        flowManager.triggerStackRemoveInstance(removeInstanceRequest);
        flowManager.triggerClusterUpscale(stackId, hostGroupAdjustment);
        flowManager.triggerClusterDownscale(stackId, hostGroupAdjustment);
        flowManager.triggerClusterSync(stackId);
        flowManager.triggerStackSync(stackId);
        flowManager.triggerFullSync(stackId);
        flowManager.triggerClusterUserNamePasswordUpdate(clusterUserNamePasswordUpdateRequest);
        flowManager.triggerClusterTermination(clusterDeleteRequest);

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
