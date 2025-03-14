package com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.handler.detachpublicips;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.SkuMigrationFailedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.SkuMigrationFlowEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class DetachPublicIpsHandlerTest {

    private static final Long STACK_ID = 1L;

    @InjectMocks
    private DetachPublicIpsHandler underTest;

    @Test
    public void testDoAccept() {
        StackView stack = mock(StackView.class);
        when(stack.getId()).thenReturn(STACK_ID);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        CloudConnector cloudConnector = mock(CloudConnector.class);
        CloudStack cloudStack = mock(CloudStack.class);
        CloudContext cloudContext = mock(CloudContext.class);
        Authenticator authenticator = mock(Authenticator.class);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        when(authenticator.authenticate(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        ResourceConnector resourceConnector = mock(ResourceConnector.class);
        when(cloudConnector.resources()).thenReturn(resourceConnector);

        DetachPublicIpsRequest request = new DetachPublicIpsRequest(stack, cloudContext, cloudCredential, cloudConnector, cloudStack);
        HandlerEvent<DetachPublicIpsRequest> handlerEvent = new HandlerEvent<>(new Event<>(request));
        Selectable selectable = underTest.doAccept(handlerEvent);

        verify(resourceConnector, times(1)).detachPublicIpAddressesForVMsIfNotPrivate(authenticatedContext, cloudStack);
        assertEquals(DetachPublicIpsResult.class, selectable.getClass());
    }

    @Test
    void testDoAcceptFailure() {
        CloudConnector cloudConnector = mock(CloudConnector.class);
        CloudContext cloudContext = mock(CloudContext.class);
        Authenticator authenticator = mock(Authenticator.class);
        when(authenticator.authenticate(any(), any())).thenThrow(new RuntimeException("error"));
        when(cloudConnector.authentication()).thenReturn(authenticator);

        DetachPublicIpsRequest request = new DetachPublicIpsRequest(mock(StackView.class), cloudContext,  mock(CloudCredential.class), cloudConnector,
                mock(CloudStack.class));
        HandlerEvent<DetachPublicIpsRequest> handlerEvent = new HandlerEvent<>(new Event<>(request));

        Selectable result = underTest.doAccept(handlerEvent);
        assertNotNull(result);
        assertEquals(SkuMigrationFailedEvent.class, result.getClass());
        assertEquals(SkuMigrationFlowEvent.SKU_MIGRATION_FAILED_EVENT.event(), result.getSelector());
    }
}