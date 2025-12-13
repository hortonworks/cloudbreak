package com.sequenceiq.cloudbreak.core.flow2.stack.migration.handler;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.aws.common.AwsAuthenticator;
import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContextBuilder;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.migration.aws.CreateResourcesRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.migration.aws.CreateResourcesResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;

@ExtendWith(MockitoExtension.class)
public class CreateResourcesHandlerTest {

    @InjectMocks
    private CreateResourcesHandler underTest;

    @Mock
    private EventBus eventBus;

    @Mock
    private PersistenceNotifier persistenceNotifier;

    @Mock
    private AwsAuthenticator awsAuthenticator;

    @Mock
    private AwsContextBuilder awsContextBuilder;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private CloudStack cloudStack;

    @Mock
    private AuthenticatedContext ac;

    @Mock
    private AwsContext awsContext;

    @Mock
    private Network network;

    @Test
    public void testAcceptWhenPersistSuccess() throws Exception {
        ReflectionTestUtils.setField(underTest, "resourceRecreators", emptyList());
        String hostGroupName = "hostGroupName";
        CreateResourcesRequest request = new CreateResourcesRequest(cloudContext, cloudCredential, cloudStack, hostGroupName);
        Event<CreateResourcesRequest> event = new Event<>(request);
        ArgumentCaptor<Event<CreateResourcesResult>> resultCaptor = ArgumentCaptor.forClass(Event.class);

        when(cloudStack.getNetwork()).thenReturn(network);
        when(awsAuthenticator.authenticate(cloudContext, cloudCredential)).thenReturn(ac);
        when(awsContextBuilder.contextInit(cloudContext, ac, network, true)).thenReturn(awsContext);

        underTest.accept(event);

        verify(eventBus).notify(eq("CREATERESOURCESRESULT"), resultCaptor.capture());
        Event<CreateResourcesResult> result = resultCaptor.getValue();
        assertNotNull(result);
    }

    @Test
    public void testAcceptWhenPersistNotSuccess() throws Exception {
        String hostGroupName = "hostGroupName";
        CreateResourcesRequest request = new CreateResourcesRequest(cloudContext, cloudCredential, cloudStack, hostGroupName);
        Event<CreateResourcesRequest> event = new Event<>(request);
        ArgumentCaptor<Event<CreateResourcesResult>> resultCaptor = ArgumentCaptor.forClass(Event.class);
        when(cloudStack.getNetwork()).thenReturn(network);
        when(awsAuthenticator.authenticate(cloudContext, cloudCredential)).thenReturn(ac);
        RuntimeException value = new RuntimeException();
        when(awsContextBuilder.contextInit(cloudContext, ac, network, true)).thenThrow(value);

        underTest.accept(event);

        verify(eventBus).notify(eq("AWSVARIANTMIGRATIONFAILEDEVENT"), resultCaptor.capture());
        Event<CreateResourcesResult> result = resultCaptor.getValue();
        assertNotNull(result);
        assertEquals(value, result.getData().getErrorDetails());
    }
}
