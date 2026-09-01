package com.sequenceiq.freeipa.flow.freeipa.migration.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationInitFailedEvent;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationLbWaitHandlerRequest;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationLbWaitResult;

@ExtendWith(MockitoExtension.class)
class MultiAzMigrationLbWaitHandlerTest {

    private static final long STACK_ID = 1L;

    private static final String OPERATION_ID = "op-1";

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @InjectMocks
    private MultiAzMigrationLbWaitHandler underTest;

    @Test
    void testSelector() {
        assertEquals(EventSelectorUtil.selector(MultiAzMigrationLbWaitHandlerRequest.class), underTest.selector());
    }

    @Test
    void testDefaultFailureEvent() {
        Exception e = new Exception("test");

        Selectable result = underTest.defaultFailureEvent(STACK_ID, e,
                new Event<>(new MultiAzMigrationLbWaitHandlerRequest(STACK_ID, OPERATION_ID, mock(), mock(), mock())));

        assertThat(result).isInstanceOf(MultiAzMigrationInitFailedEvent.class);
        MultiAzMigrationInitFailedEvent failure = (MultiAzMigrationInitFailedEvent) result;
        assertEquals(STACK_ID, failure.getResourceId());
        assertEquals(e, failure.getException());
    }

    @Test
    void testDoAcceptWaitsForLoadBalancer() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setCloudPlatform("AWS");
        stack.setPlatformvariant("AWS");

        CloudContext cloudContext = mock(CloudContext.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        CloudStack cloudStack = mock(CloudStack.class);

        MultiAzMigrationLbWaitHandlerRequest request = new MultiAzMigrationLbWaitHandlerRequest(
                STACK_ID, OPERATION_ID, cloudContext, cloudCredential, cloudStack);

        CloudConnector cloudConnector = mock(CloudConnector.class);
        ResourceConnector resourceConnector = mock(ResourceConnector.class);
        when(cloudConnector.resources()).thenReturn(resourceConnector);
        when(cloudConnector.authentication()).thenReturn(mock());
        when(cloudPlatformConnectors.get(any(CloudPlatformVariant.class))).thenReturn(cloudConnector);

        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        when(cloudConnector.authentication().authenticate(cloudContext, cloudCredential)).thenReturn(authenticatedContext);

        Selectable result = underTest.doAccept(new HandlerEvent<>(new Event<>(request)));

        assertThat(result).isInstanceOf(MultiAzMigrationLbWaitResult.class);
        MultiAzMigrationLbWaitResult waitResult = (MultiAzMigrationLbWaitResult) result;
        assertEquals(STACK_ID, waitResult.getResourceId());
        assertEquals(OPERATION_ID, waitResult.getOperationId());

        verify(resourceConnector).waitForLoadBalancers(authenticatedContext, cloudStack);
    }
}
