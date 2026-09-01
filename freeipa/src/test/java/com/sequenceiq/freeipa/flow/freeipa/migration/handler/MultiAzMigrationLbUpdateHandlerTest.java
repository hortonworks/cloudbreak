package com.sequenceiq.freeipa.flow.freeipa.migration.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

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
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationInitFailedEvent;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationLbUpdateHandlerRequest;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationLbUpdateResult;

@ExtendWith(MockitoExtension.class)
class MultiAzMigrationLbUpdateHandlerTest {

    private static final long STACK_ID = 1L;

    private static final String OPERATION_ID = "op-1";

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private PersistenceNotifier persistenceNotifier;

    @InjectMocks
    private MultiAzMigrationLbUpdateHandler underTest;

    @Test
    void testSelector() {
        assertEquals(EventSelectorUtil.selector(MultiAzMigrationLbUpdateHandlerRequest.class), underTest.selector());
    }

    @Test
    void testDefaultFailureEvent() {
        Exception e = new Exception("test");

        Selectable result = underTest.defaultFailureEvent(STACK_ID, e,
                new Event<>(new MultiAzMigrationLbUpdateHandlerRequest(STACK_ID, OPERATION_ID, mock(), mock(), mock())));

        assertThat(result).isInstanceOf(MultiAzMigrationInitFailedEvent.class);
        MultiAzMigrationInitFailedEvent failure = (MultiAzMigrationInitFailedEvent) result;
        assertEquals(STACK_ID, failure.getResourceId());
        assertEquals(e, failure.getException());
    }

    @Test
    void testDoAcceptSuccessfullyUpdatesLoadBalancer() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setCloudPlatform("AWS");
        stack.setPlatformvariant("AWS");

        CloudContext cloudContext = mock(CloudContext.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        CloudStack cloudStack = mock(CloudStack.class);

        MultiAzMigrationLbUpdateHandlerRequest request = new MultiAzMigrationLbUpdateHandlerRequest(
                STACK_ID, OPERATION_ID, cloudContext, cloudCredential, cloudStack);

        CloudConnector cloudConnector = mock(CloudConnector.class);
        ResourceConnector resourceConnector = mock(ResourceConnector.class);
        when(cloudConnector.resources()).thenReturn(resourceConnector);
        when(cloudConnector.authentication()).thenReturn(mock());

        when(cloudPlatformConnectors.get(any(CloudPlatformVariant.class))).thenReturn(cloudConnector);

        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        when(cloudConnector.authentication().authenticate(cloudContext, cloudCredential)).thenReturn(authenticatedContext);

        when(resourceConnector.updateLoadBalancers(authenticatedContext, cloudStack, persistenceNotifier))
                .thenReturn(List.of());

        Selectable result = underTest.doAccept(new HandlerEvent<>(new Event<>(request)));

        assertThat(result).isInstanceOf(MultiAzMigrationLbUpdateResult.class);
        MultiAzMigrationLbUpdateResult lbResult = (MultiAzMigrationLbUpdateResult) result;
        assertEquals(STACK_ID, lbResult.getResourceId());
        assertEquals(OPERATION_ID, lbResult.getOperationId());

        verify(resourceConnector).enableMultiAzOnLoadBalancers(authenticatedContext, cloudStack);
    }
}
