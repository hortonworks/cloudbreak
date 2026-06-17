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
import com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.event.PrepareUpgradeFailureCleanupComplete;
import com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.event.PrepareUpgradeFailureCleanupRequest;
import com.sequenceiq.freeipa.service.resource.ResourceService;

@ExtendWith(MockitoExtension.class)
class PrepareUpgradeFailureCleanupHandlerTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private ResourceService resourceService;

    @Mock
    private ResourceToCloudResourceConverter resourceToCloudResourceConverter;

    @InjectMocks
    private PrepareUpgradeFailureCleanupHandler underTest;

    @Test
    void testSelector() {
        assertEquals(EventSelectorUtil.selector(PrepareUpgradeFailureCleanupRequest.class), underTest.selector());
    }

    @Test
    void testDefaultFailureEventReturnsComplete() {
        Exception e = new Exception("test");
        PrepareUpgradeFailureCleanupRequest request = createRequest();

        Selectable result = underTest.defaultFailureEvent(STACK_ID, e, new Event<>(request));

        assertInstanceOf(PrepareUpgradeFailureCleanupComplete.class, result);
        assertEquals(STACK_ID, result.getResourceId());
    }

    @Test
    void testDoAcceptSuccessfulCleanup() throws Exception {
        PrepareUpgradeFailureCleanupRequest request = createRequest();
        CloudContext cloudContext = request.getCloudContext();
        CloudConnector connector = mock(CloudConnector.class);
        Authenticator authenticator = mock(Authenticator.class);
        AuthenticatedContext ac = mock(AuthenticatedContext.class);
        ResourceConnector resourceConnector = mock(ResourceConnector.class);

        Resource elbResource = createResource(ResourceType.ELASTIC_LOAD_BALANCER, "elb-1");
        Resource tgResource = createResource(ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP, "tg-1");
        CloudResource cloudElb = mock(CloudResource.class);
        CloudResource cloudTg = mock(CloudResource.class);

        when(resourceService.findAllByStackId(STACK_ID)).thenReturn(List.of(elbResource, tgResource));
        when(resourceToCloudResourceConverter.convert(elbResource)).thenReturn(cloudElb);
        when(resourceToCloudResourceConverter.convert(tgResource)).thenReturn(cloudTg);
        when(cloudPlatformConnectors.get(any(), any())).thenReturn(connector);
        when(connector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(eq(cloudContext), any())).thenReturn(ac);
        when(connector.resources()).thenReturn(resourceConnector);

        Selectable result = underTest.doAccept(new HandlerEvent<>(new Event<>(request)));

        assertInstanceOf(PrepareUpgradeFailureCleanupComplete.class, result);
        assertEquals(STACK_ID, result.getResourceId());
        verify(resourceConnector).terminate(eq(ac), any(), eq(List.of(cloudElb, cloudTg)));
    }

    @Test
    void testDoAcceptNoLbResources() {
        PrepareUpgradeFailureCleanupRequest request = createRequest();
        CloudConnector connector = mock(CloudConnector.class);
        Authenticator authenticator = mock(Authenticator.class);
        AuthenticatedContext ac = mock(AuthenticatedContext.class);
        ResourceConnector resourceConnector = mock(ResourceConnector.class);

        when(resourceService.findAllByStackId(STACK_ID)).thenReturn(List.of());
        when(cloudPlatformConnectors.get(any(), any())).thenReturn(connector);
        when(connector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(any(), any())).thenReturn(ac);

        Selectable result = underTest.doAccept(new HandlerEvent<>(new Event<>(request)));

        assertInstanceOf(PrepareUpgradeFailureCleanupComplete.class, result);
        assertEquals(STACK_ID, result.getResourceId());
        verifyNoInteractions(resourceConnector);
    }

    @Test
    void testDoAcceptTerminationFailsStillReturnsComplete() throws Exception {
        PrepareUpgradeFailureCleanupRequest request = createRequest();
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

        assertInstanceOf(PrepareUpgradeFailureCleanupComplete.class, result);
        assertEquals(STACK_ID, result.getResourceId());
    }

    @Test
    void testDoAcceptAuthenticationFailsStillReturnsComplete() {
        PrepareUpgradeFailureCleanupRequest request = createRequest();
        CloudConnector connector = mock(CloudConnector.class);
        Authenticator authenticator = mock(Authenticator.class);

        when(cloudPlatformConnectors.get(any(), any())).thenReturn(connector);
        when(connector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(any(), any())).thenThrow(new RuntimeException("Auth failed"));

        Selectable result = underTest.doAccept(new HandlerEvent<>(new Event<>(request)));

        assertInstanceOf(PrepareUpgradeFailureCleanupComplete.class, result);
        assertEquals(STACK_ID, result.getResourceId());
    }

    private PrepareUpgradeFailureCleanupRequest createRequest() {
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withPlatform("AWS")
                .withVariant("AWS")
                .build();
        return new PrepareUpgradeFailureCleanupRequest(STACK_ID, cloudContext, new CloudCredential(), mock(CloudStack.class));
    }

    private Resource createResource(ResourceType type, String name) {
        Resource resource = new Resource();
        resource.setResourceType(type);
        resource.setResourceName(name);
        resource.setResourceStatus(CommonStatus.CREATED);
        return resource;
    }
}
