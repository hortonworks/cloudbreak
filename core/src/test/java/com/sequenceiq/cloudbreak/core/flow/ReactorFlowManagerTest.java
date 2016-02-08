package com.sequenceiq.cloudbreak.core.flow;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.model.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.api.model.StatusRequest;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.common.type.ScalingType;
import com.sequenceiq.cloudbreak.core.flow.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.SecurityRule;
import com.sequenceiq.cloudbreak.service.cluster.event.ClusterStatusUpdateRequest;
import com.sequenceiq.cloudbreak.service.cluster.event.ClusterUserNamePasswordUpdateRequest;
import com.sequenceiq.cloudbreak.service.cluster.event.UpdateAmbariHostsRequest;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionRequest;
import com.sequenceiq.cloudbreak.service.stack.event.RemoveInstanceRequest;
import com.sequenceiq.cloudbreak.service.stack.event.StackDeleteRequest;
import com.sequenceiq.cloudbreak.service.stack.event.StackForcedDeleteRequest;
import com.sequenceiq.cloudbreak.service.stack.event.StackStatusUpdateRequest;
import com.sequenceiq.cloudbreak.service.stack.event.UpdateAllowedSubnetsRequest;
import com.sequenceiq.cloudbreak.service.stack.event.UpdateInstancesRequest;

import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.core.dispatch.ThreadPoolExecutorDispatcher;

@RunWith(MockitoJUnitRunner.class)
public class ReactorFlowManagerTest {

    private static final Platform GCP_PLATFORM = Platform.platform(CloudConstants.GCP);

    @Mock
    private EventBus reactor;

    @Mock
    private TransitionKeyService transitionKeyService;

    @Mock
    private ErrorHandlerAwareFlowEventFactory eventFactory;

    @InjectMocks
    private ReactorFlowManager flowManager;

    @Before
    public void setUp() throws Exception {
        reset(reactor);
        reset(eventFactory);
        when(reactor.notify(anyObject(), any(Event.class))).thenReturn(new EventBus(new ThreadPoolExecutorDispatcher(1, 1)));
        when(eventFactory.createEvent(anyObject(), anyString())).thenReturn(new Event<Object>(String.class));
    }

//    @Test
//    public void shouldReactorNotifyTheNextSuccessTransition() throws Exception {
//        when(transitionKeyService.successKey(any(Class.class))).thenReturn(FlowPhases.CLUSTER_INSTALL.name());
//        flowManager.triggerNext(ProvisioningSetupHandler.class, new ProvisioningContext.Builder().build(), true);

//        verify(reactor, times(1)).notify(anyObject(), any(Event.class));
//        verify(eventFactory, times(1)).createEvent(anyObject(), anyString());
//    }

//    @Test(expected = IllegalArgumentException.class)
//    public void shouldNotReactorNotifyWhenTransitionKeyNotFlowPhaseName() throws Exception {
//        when(transitionKeyService.successKey(any(Class.class))).thenReturn("hoax");
//        flowManager.triggerNext(ProvisioningSetupHandler.class, new ProvisioningContext.Builder().build(), true);
//    }

    @Test
    public void shouldReturnTheNextFailureTransition() throws Exception {
        ProvisionRequest provisionRequest =  new ProvisionRequest(GCP_PLATFORM, 1L);
        StackStatusUpdateRequest stackStatusUpdateRequest = new StackStatusUpdateRequest(GCP_PLATFORM, 1L, StatusRequest.STARTED);
        ClusterStatusUpdateRequest clusterStatusUpdateRequest = new ClusterStatusUpdateRequest(1L, StatusRequest.STARTED, GCP_PLATFORM);
        StackDeleteRequest stackDeleteRequest = new StackDeleteRequest(GCP_PLATFORM, 1L);
        StackForcedDeleteRequest stackForcedDeleteRequest = new StackForcedDeleteRequest(GCP_PLATFORM, 1L);
        UpdateInstancesRequest updateInstancesRequest = new UpdateInstancesRequest(GCP_PLATFORM, 1L, 1, "master", ScalingType.DOWNSCALE_ONLY_CLUSTER);
        RemoveInstanceRequest removeInstanceRequest = new RemoveInstanceRequest(GCP_PLATFORM, 1L, "instanceId");
        UpdateAmbariHostsRequest updateAmbariHostsRequest = new UpdateAmbariHostsRequest(1L, new HostGroupAdjustmentJson(),
                new ArrayList<HostMetadata>(), true, GCP_PLATFORM, ScalingType.DOWNSCALE_ONLY_CLUSTER);
        UpdateAllowedSubnetsRequest updateAllowedSubnetsRequest = new UpdateAllowedSubnetsRequest(GCP_PLATFORM, 1L, new ArrayList<SecurityRule>());
        ClusterUserNamePasswordUpdateRequest clusterUserNamePasswordUpdateRequest =
                new ClusterUserNamePasswordUpdateRequest(1L, "admin", "admin1", GCP_PLATFORM);

        flowManager.triggerProvisioning(provisionRequest);
        flowManager.triggerClusterInstall(provisionRequest);
        flowManager.triggerClusterReInstall(provisionRequest);
        flowManager.triggerStackStop(stackStatusUpdateRequest);
        flowManager.triggerStackStart(stackStatusUpdateRequest);
        flowManager.triggerStackStopRequested(stackStatusUpdateRequest);
        flowManager.triggerClusterStartRequested(clusterStatusUpdateRequest);
        flowManager.triggerClusterStop(clusterStatusUpdateRequest);
        flowManager.triggerClusterStart(clusterStatusUpdateRequest);
        flowManager.triggerTermination(stackDeleteRequest);
        flowManager.triggerForcedTermination(stackForcedDeleteRequest);
        flowManager.triggerStackUpscale(updateInstancesRequest);
        flowManager.triggerStackDownscale(updateInstancesRequest);
        flowManager.triggerStackRemoveInstance(removeInstanceRequest);
        flowManager.triggerClusterUpscale(updateAmbariHostsRequest);
        flowManager.triggerClusterDownscale(updateAmbariHostsRequest);
        flowManager.triggerUpdateAllowedSubnets(updateAllowedSubnetsRequest);
        flowManager.triggerClusterSync(clusterStatusUpdateRequest);
        flowManager.triggerStackSync(stackStatusUpdateRequest);
        flowManager.triggerClusterUserNamePasswordUpdate(clusterUserNamePasswordUpdateRequest);

        int count = -1;
        for (Method method : flowManager.getClass().getDeclaredMethods()) {
            if (method.getName().startsWith("trigger")) {
                count++;
            }
        }
        verify(reactor, times(count)).notify(anyObject(), any(Event.class));
    }

}