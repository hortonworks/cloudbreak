package com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.handler;

import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.UPSCALE_UPDATE_LOAD_BALANCERS_FINISHED_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
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
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.UpscaleUpdateLoadBalancersFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.UpscaleUpdateLoadBalancersRequest;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class UpscaleUpdateLoadBalancersHandlerTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private PersistenceNotifier persistenceNotifier;

    @Mock
    private CloudConnector cloudConnector;

    @Mock
    private Authenticator authenticator;

    @Mock
    private ResourceConnector resourceConnector;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private CloudStack cloudStack;

    @InjectMocks
    private UpscaleUpdateLoadBalancersHandler underTest;

    @BeforeEach
    void setUp() {
        CloudPlatformVariant platformVariant = new CloudPlatformVariant("AWS", "AWS");
        when(cloudContext.getPlatformVariant()).thenReturn(platformVariant);
        when(cloudPlatformConnectors.get(platformVariant)).thenReturn(cloudConnector);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        when(cloudConnector.resources()).thenReturn(resourceConnector);
    }

    @Test
    void testDoAcceptWhenLoadBalancersUpdatedThenFinishedEventIsReturned() throws Exception {
        Selectable result = underTest.doAccept(handlerEvent());

        verify(resourceConnector).updateLoadBalancers(authenticatedContext, cloudStack, persistenceNotifier);
        assertInstanceOf(StackEvent.class, result);
        assertEquals(UPSCALE_UPDATE_LOAD_BALANCERS_FINISHED_EVENT.selector(), result.selector());
        assertEquals(STACK_ID, result.getResourceId());
    }

    @Test
    void testDoAcceptWhenUpdateLoadBalancersFailsThenFailedEventIsReturned() throws Exception {
        RuntimeException exception = new RuntimeException("update failed");
        when(resourceConnector.updateLoadBalancers(authenticatedContext, cloudStack, persistenceNotifier)).thenThrow(exception);

        Selectable result = underTest.doAccept(handlerEvent());

        UpscaleUpdateLoadBalancersFailed failed = assertInstanceOf(UpscaleUpdateLoadBalancersFailed.class, result);
        assertEquals(STACK_ID, failed.getResourceId());
        assertSame(exception, failed.getException());
    }

    private HandlerEvent<UpscaleUpdateLoadBalancersRequest> handlerEvent() {
        UpscaleUpdateLoadBalancersRequest request = new UpscaleUpdateLoadBalancersRequest(STACK_ID, cloudStack, cloudContext, cloudCredential);
        return new HandlerEvent<>(new Event<>(request));
    }
}
