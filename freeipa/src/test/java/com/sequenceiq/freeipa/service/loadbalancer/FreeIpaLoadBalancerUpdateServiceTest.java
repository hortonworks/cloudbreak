package com.sequenceiq.freeipa.service.loadbalancer;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.task.PollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.task.ResourcesStatePollerResult;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.update.LoadBalancerUpdateRequest;

@ExtendWith(MockitoExtension.class)
class FreeIpaLoadBalancerUpdateServiceTest {

    private static final Platform PLATFORM = Platform.platform("AWS");

    private static final Variant VARIANT = Variant.variant("AWS");

    private static final long STACK_ID = 1L;

    @InjectMocks
    private FreeIpaLoadBalancerUpdateService underTest;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private PersistenceNotifier persistenceNotifier;

    @Mock
    private SyncPollingScheduler<ResourcesStatePollerResult> syncPollingScheduler;

    @Mock
    private PollTaskFactory statusCheckFactory;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private CloudStack cloudStack;

    @Mock
    private CloudConnector connector;

    @Mock
    private Authenticator authenticator;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private ResourceConnector resourceConnector;

    @Mock
    private PollTask<ResourcesStatePollerResult> pollTask;

    @Test
    void testCreateLoadBalancerShouldCallTheCloudConnectorAndThePollingTaskAlreadyCompleted() throws Exception {
        CloudContext cloudContext = createContext();
        LoadBalancerUpdateRequest request = new LoadBalancerUpdateRequest(STACK_ID, cloudContext, cloudCredential, cloudStack);
        when(cloudPlatformConnectors.get(PLATFORM, VARIANT)).thenReturn(connector);
        when(connector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        when(connector.resources()).thenReturn(resourceConnector);
        when(resourceConnector.updateLoadBalancers(authenticatedContext, cloudStack, persistenceNotifier)).thenReturn(new ArrayList<>());
        when(statusCheckFactory.newPollResourcesStateTask(any(), any(), anyBoolean())).thenReturn(pollTask);
        when(pollTask.completed(any())).thenReturn(true);

        underTest.updateLoadBalancer(request);

        verifyNoInteractions(syncPollingScheduler);
    }

    @Test
    void testCreateLoadBalancerShouldCallTheCloudConnectorAndThePollingTaskIsScheduled() throws Exception {
        CloudContext cloudContext = createContext();
        LoadBalancerUpdateRequest request = new LoadBalancerUpdateRequest(STACK_ID, cloudContext, cloudCredential, cloudStack);
        when(cloudPlatformConnectors.get(PLATFORM, VARIANT)).thenReturn(connector);
        when(connector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        when(connector.resources()).thenReturn(resourceConnector);
        when(resourceConnector.updateLoadBalancers(authenticatedContext, cloudStack, persistenceNotifier)).thenReturn(new ArrayList<>());
        when(statusCheckFactory.newPollResourcesStateTask(any(), any(), anyBoolean())).thenReturn(pollTask);
        when(pollTask.completed(any())).thenReturn(false);

        underTest.updateLoadBalancer(request);

        verify(syncPollingScheduler).schedule(pollTask);
    }

    @Test
    void testCreateLoadBalancerShouldThrowException() throws Exception {
        CloudContext cloudContext = createContext();
        LoadBalancerUpdateRequest request = new LoadBalancerUpdateRequest(STACK_ID, cloudContext, cloudCredential, cloudStack);
        when(cloudPlatformConnectors.get(PLATFORM, VARIANT)).thenReturn(connector);
        when(connector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        when(connector.resources()).thenReturn(resourceConnector);
        when(resourceConnector.updateLoadBalancers(authenticatedContext, cloudStack, persistenceNotifier)).thenThrow(new RuntimeException("error"));

        assertThrows(CloudbreakServiceException.class, () -> underTest.updateLoadBalancer(request));

        verifyNoInteractions(statusCheckFactory, syncPollingScheduler);
    }

    private CloudContext createContext() {
        return CloudContext.Builder.builder().withPlatform(PLATFORM).withVariant(VARIANT).build();
    }

}