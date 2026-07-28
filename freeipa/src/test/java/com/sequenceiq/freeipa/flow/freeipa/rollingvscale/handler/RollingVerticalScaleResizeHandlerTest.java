package com.sequenceiq.freeipa.flow.freeipa.rollingvscale.handler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.UpdateType;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.freeipa.flow.freeipa.rollingvscale.event.FreeIpaRollingVerticalScaleFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.rollingvscale.event.RollingVerticalScaleResizeRequest;
import com.sequenceiq.freeipa.flow.freeipa.rollingvscale.event.RollingVerticalScaleResizeResult;

@ExtendWith(MockitoExtension.class)
class RollingVerticalScaleResizeHandlerTest {

    private static final long STACK_ID = 1L;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private EventBus eventBus;

    @InjectMocks
    private RollingVerticalScaleResizeHandler underTest;

    private RollingVerticalScaleResizeRequest buildRequest() {
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withId(STACK_ID)
                .withPlatform("AWS")
                .withVariant("AWS")
                .build();
        CloudCredential credential = new CloudCredential("id", "name", Map.of(), "acc");
        CloudStack cloudStack = mock(CloudStack.class);
        return new RollingVerticalScaleResizeRequest(STACK_ID, cloudContext, credential, cloudStack, List.of(), "master");
    }

    @Test
    void testResizeCallsConnectorUpdate() throws Exception {
        RollingVerticalScaleResizeRequest request = buildRequest();

        CloudConnector connector = mock(CloudConnector.class);
        Authenticator authenticator = mock(Authenticator.class);
        AuthenticatedContext ac = mock(AuthenticatedContext.class);
        ResourceConnector resourceConnector = mock(ResourceConnector.class);

        when(cloudPlatformConnectors.get(any())).thenReturn(connector);
        when(connector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(any(), any())).thenReturn(ac);
        when(connector.resources()).thenReturn(resourceConnector);
        when(resourceConnector.update(any(), any(), any(), any(), any())).thenReturn(List.of());

        underTest.accept(new Event<>(request));

        verify(resourceConnector).update(eq(ac), any(), any(), eq(UpdateType.VERTICAL_SCALE), any());

        ArgumentCaptor<Event<?>> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(any(String.class), eventCaptor.capture());
        assertInstanceOf(RollingVerticalScaleResizeResult.class, eventCaptor.getValue().getData());
    }

    @Test
    void testResizeWrapsExceptionInRuntimeException() throws Exception {
        RollingVerticalScaleResizeRequest request = buildRequest();

        CloudConnector connector = mock(CloudConnector.class);
        Authenticator authenticator = mock(Authenticator.class);
        AuthenticatedContext ac = mock(AuthenticatedContext.class);
        ResourceConnector resourceConnector = mock(ResourceConnector.class);

        when(cloudPlatformConnectors.get(any())).thenReturn(connector);
        when(connector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(any(), any())).thenReturn(ac);
        when(connector.resources()).thenReturn(resourceConnector);
        when(resourceConnector.update(any(), any(), any(), any(), any())).thenThrow(new RuntimeException("cloud error"));

        assertDoesNotThrow(() -> underTest.accept(new Event<>(request)));

        ArgumentCaptor<Event<?>> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(any(String.class), eventCaptor.capture());
        assertInstanceOf(FreeIpaRollingVerticalScaleFailureEvent.class, eventCaptor.getValue().getData());
    }
}
