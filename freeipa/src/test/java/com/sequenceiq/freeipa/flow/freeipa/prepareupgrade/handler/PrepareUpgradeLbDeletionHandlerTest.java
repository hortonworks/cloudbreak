package com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

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
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.converter.cloud.ResourceToCloudResourceConverter;
import com.sequenceiq.freeipa.entity.Resource;
import com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.event.PrepareUpgradeFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.event.PrepareUpgradeLbDeletionRequest;
import com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.event.PrepareUpgradeLbDeletionSuccess;
import com.sequenceiq.freeipa.service.resource.ResourceService;

@ExtendWith(MockitoExtension.class)
class PrepareUpgradeLbDeletionHandlerTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private ResourceService resourceService;

    @Mock
    private ResourceToCloudResourceConverter resourceToCloudResourceConverter;

    @InjectMocks
    private PrepareUpgradeLbDeletionHandler underTest;

    @Test
    void testSelector() {
        assertEquals(EventSelectorUtil.selector(PrepareUpgradeLbDeletionRequest.class), underTest.selector());
    }

    @Test
    void testDefaultFailureEvent() {
        Exception e = new Exception("test");
        PrepareUpgradeLbDeletionRequest request = createRequest();

        Selectable result = underTest.defaultFailureEvent(STACK_ID, e, new Event<>(request));

        assertInstanceOf(PrepareUpgradeFailureEvent.class, result);
        assertEquals(STACK_ID, result.getResourceId());
    }

    @Test
    void testDoAcceptSuccessfulDeletion() throws Exception {
        PrepareUpgradeLbDeletionRequest request = createRequest();
        CloudContext cloudContext = request.getCloudContext();
        CloudConnector connector = mock(CloudConnector.class);
        Authenticator authenticator = mock(Authenticator.class);
        AuthenticatedContext ac = mock(AuthenticatedContext.class);
        ResourceConnector resourceConnector = mock(ResourceConnector.class);

        Resource elbResource = createResource(ResourceType.ELASTIC_LOAD_BALANCER, "elb-1");
        Resource listenerResource = createResource(ResourceType.ELASTIC_LOAD_BALANCER_LISTENER, "listener-1");
        Resource nonLbResource = createResource(ResourceType.AWS_INSTANCE, "instance-1");
        CloudResource cloudElb = mock(CloudResource.class);
        CloudResource cloudListener = mock(CloudResource.class);

        when(resourceService.findAllByStackId(STACK_ID)).thenReturn(List.of(elbResource, listenerResource, nonLbResource));
        when(resourceToCloudResourceConverter.convert(elbResource)).thenReturn(cloudElb);
        when(resourceToCloudResourceConverter.convert(listenerResource)).thenReturn(cloudListener);
        when(cloudPlatformConnectors.get(any(), any())).thenReturn(connector);
        when(connector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(eq(cloudContext), any())).thenReturn(ac);
        when(connector.resources()).thenReturn(resourceConnector);

        Selectable result = underTest.doAccept(new HandlerEvent<>(new Event<>(request)));

        assertInstanceOf(PrepareUpgradeLbDeletionSuccess.class, result);
        assertEquals(STACK_ID, result.getResourceId());
        verify(resourceConnector).terminate(eq(ac), any(), eq(List.of(cloudElb, cloudListener)));
    }

    @Test
    void testDoAcceptNoLbResources() throws Exception {
        PrepareUpgradeLbDeletionRequest request = createRequest();
        CloudConnector connector = mock(CloudConnector.class);
        Authenticator authenticator = mock(Authenticator.class);
        AuthenticatedContext ac = mock(AuthenticatedContext.class);
        ResourceConnector resourceConnector = mock(ResourceConnector.class);

        Resource nonLbResource = createResource(ResourceType.AWS_INSTANCE, "instance-1");
        when(resourceService.findAllByStackId(STACK_ID)).thenReturn(List.of(nonLbResource));
        when(cloudPlatformConnectors.get(any(), any())).thenReturn(connector);
        when(connector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(any(), any())).thenReturn(ac);

        Selectable result = underTest.doAccept(new HandlerEvent<>(new Event<>(request)));

        assertInstanceOf(PrepareUpgradeLbDeletionSuccess.class, result);
        assertEquals(STACK_ID, result.getResourceId());
        verifyNoInteractions(resourceConnector);
    }

    @Test
    void testDoAcceptTerminationThrowsException() throws Exception {
        PrepareUpgradeLbDeletionRequest request = createRequest();
        CloudConnector connector = mock(CloudConnector.class);
        Authenticator authenticator = mock(Authenticator.class);
        AuthenticatedContext ac = mock(AuthenticatedContext.class);
        ResourceConnector resourceConnector = mock(ResourceConnector.class);

        Resource elbResource = createResource(ResourceType.ELASTIC_LOAD_BALANCER, "elb-1");
        CloudResource cloudElb = mock(CloudResource.class);

        when(resourceService.findAllByStackId(STACK_ID)).thenReturn(List.of(elbResource));
        when(resourceToCloudResourceConverter.convert(elbResource)).thenReturn(cloudElb);
        when(cloudPlatformConnectors.get(any(), any())).thenReturn(connector);
        when(connector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(any(), any())).thenReturn(ac);
        when(connector.resources()).thenReturn(resourceConnector);
        when(resourceConnector.terminate(any(), any(), any())).thenThrow(new RuntimeException("Cloud API error"));

        Selectable result = underTest.doAccept(new HandlerEvent<>(new Event<>(request)));

        assertInstanceOf(PrepareUpgradeFailureEvent.class, result);
        assertEquals(STACK_ID, result.getResourceId());
    }

    private PrepareUpgradeLbDeletionRequest createRequest() {
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withPlatform("AWS")
                .withVariant("AWS")
                .build();
        return new PrepareUpgradeLbDeletionRequest(STACK_ID, cloudContext, new CloudCredential(), mock(CloudStack.class));
    }

    private Resource createResource(ResourceType type, String name) {
        Resource resource = new Resource();
        resource.setResourceType(type);
        resource.setResourceName(name);
        resource.setResourceStatus(CommonStatus.CREATED);
        return resource;
    }
}
