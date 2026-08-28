package com.sequenceiq.freeipa.flow.freeipa.loadbalancer.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.azure.AzureConstants;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.converter.cloud.ResourceToCloudResourceConverter;
import com.sequenceiq.freeipa.entity.Resource;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.LoadBalancerDeletionFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.deletion.LoadBalancerCloudDeletionRequest;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.deletion.LoadBalancerCloudDeletionSuccess;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerService;
import com.sequenceiq.freeipa.service.resource.ResourceService;

@ExtendWith(MockitoExtension.class)
class LoadBalancerCloudDeletionHandlerTest {

    private static final Long STACK_ID = 1L;

    @InjectMocks
    private LoadBalancerCloudDeletionHandler underTest;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private ResourceService resourceService;

    @Mock
    private ResourceToCloudResourceConverter resourceToCloudResourceConverter;

    @Mock
    private FreeIpaLoadBalancerService freeIpaLoadBalancerService;

    @Test
    void doAcceptDeletesLbResourcesAndReturnsSuccess() throws Exception {
        CloudContext cloudContext = mock(CloudContext.class);
        when(cloudContext.getPlatform()).thenReturn(AzureConstants.PLATFORM);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        CloudStack cloudStack = mock(CloudStack.class);
        LoadBalancerCloudDeletionRequest request = new LoadBalancerCloudDeletionRequest(STACK_ID, cloudContext, cloudCredential, cloudStack);

        CloudConnector connector = mock(CloudConnector.class);
        Authenticator authenticator = mock(Authenticator.class);
        AuthenticatedContext ac = mock(AuthenticatedContext.class);
        ResourceConnector resourceConnector = mock(ResourceConnector.class);
        when(cloudPlatformConnectors.get(any(), any())).thenReturn(connector);
        when(connector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(cloudContext, cloudCredential)).thenReturn(ac);
        when(connector.resources()).thenReturn(resourceConnector);

        Resource lbResource = mock(Resource.class);
        CloudResource cloudResource = mock(CloudResource.class);
        List<Resource> lbResources = List.of(lbResource);
        when(resourceService.findAllByStackIdAndResourceTypeIn(STACK_ID, Set.of(ResourceType.AZURE_LOAD_BALANCER))).thenReturn(lbResources);
        when(resourceToCloudResourceConverter.convert(lbResource)).thenReturn(cloudResource);

        Selectable result = underTest.doAccept(new HandlerEvent<>(Event.wrap(request)));

        assertThat(result).isInstanceOf(LoadBalancerCloudDeletionSuccess.class);
        assertThat(result.getResourceId()).isEqualTo(STACK_ID);
        verify(resourceConnector).deleteLoadBalancers(ac, cloudStack, List.of(cloudResource));
        verify(resourceService).deleteAll(lbResources);
        verify(freeIpaLoadBalancerService).delete(STACK_ID);
    }

    @Test
    void doAcceptStillCallsDeleteWhenNoLbResourcesFound() throws Exception {
        CloudContext cloudContext = mock(CloudContext.class);
        when(cloudContext.getPlatform()).thenReturn(AzureConstants.PLATFORM);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        CloudStack cloudStack = mock(CloudStack.class);
        LoadBalancerCloudDeletionRequest request = new LoadBalancerCloudDeletionRequest(STACK_ID, cloudContext, cloudCredential, cloudStack);

        CloudConnector connector = mock(CloudConnector.class);
        Authenticator authenticator = mock(Authenticator.class);
        AuthenticatedContext ac = mock(AuthenticatedContext.class);
        ResourceConnector resourceConnector = mock(ResourceConnector.class);
        when(cloudPlatformConnectors.get(any(), any())).thenReturn(connector);
        when(connector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(cloudContext, cloudCredential)).thenReturn(ac);
        when(connector.resources()).thenReturn(resourceConnector);
        when(resourceService.findAllByStackIdAndResourceTypeIn(STACK_ID, Set.of(ResourceType.AZURE_LOAD_BALANCER))).thenReturn(List.of());

        Selectable result = underTest.doAccept(new HandlerEvent<>(Event.wrap(request)));

        assertThat(result).isInstanceOf(LoadBalancerCloudDeletionSuccess.class);
        verify(resourceConnector).deleteLoadBalancers(ac, cloudStack, List.of());
        verify(resourceService).deleteAll(List.of());
        verify(freeIpaLoadBalancerService).delete(STACK_ID);
    }

    @Test
    void doAcceptReturnsFailureEventOnAuthException() {
        LoadBalancerCloudDeletionRequest request = new LoadBalancerCloudDeletionRequest(STACK_ID, mock(CloudContext.class),
                mock(CloudCredential.class), mock(CloudStack.class));
        when(cloudPlatformConnectors.get(any(), any())).thenThrow(new RuntimeException("cloud error"));

        Selectable result = underTest.doAccept(new HandlerEvent<>(Event.wrap(request)));

        assertThat(result).isInstanceOf(LoadBalancerDeletionFailureEvent.class);
        assertThat(result.getResourceId()).isEqualTo(STACK_ID);
    }

    @Test
    void doAcceptReturnsFailureEventWhenDeleteLoadBalancersFails() throws Exception {
        CloudContext cloudContext = mock(CloudContext.class);
        when(cloudContext.getPlatform()).thenReturn(AzureConstants.PLATFORM);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        LoadBalancerCloudDeletionRequest request = new LoadBalancerCloudDeletionRequest(STACK_ID, cloudContext, cloudCredential, mock(CloudStack.class));

        CloudConnector connector = mock(CloudConnector.class);
        Authenticator authenticator = mock(Authenticator.class);
        AuthenticatedContext ac = mock(AuthenticatedContext.class);
        ResourceConnector resourceConnector = mock(ResourceConnector.class);
        when(cloudPlatformConnectors.get(any(), any())).thenReturn(connector);
        when(connector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(any(), any())).thenReturn(ac);
        when(connector.resources()).thenReturn(resourceConnector);
        when(resourceService.findAllByStackIdAndResourceTypeIn(eq(STACK_ID), any())).thenReturn(List.of());
        doThrow(new RuntimeException("delete failed")).when(resourceConnector).deleteLoadBalancers(any(), any(), any());

        Selectable result = underTest.doAccept(new HandlerEvent<>(Event.wrap(request)));

        assertThat(result).isInstanceOf(LoadBalancerDeletionFailureEvent.class);
        assertThat(result.getResourceId()).isEqualTo(STACK_ID);
        verify(resourceService, never()).deleteAll(any());
        verify(freeIpaLoadBalancerService, never()).delete(any());
    }
}
