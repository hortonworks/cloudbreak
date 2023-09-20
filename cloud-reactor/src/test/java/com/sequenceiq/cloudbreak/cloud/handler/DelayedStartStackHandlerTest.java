package com.sequenceiq.cloudbreak.cloud.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.InstanceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.DelayedStartInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.StartInstancesResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.service.executor.DelayedExecutorService;

@ExtendWith(MockitoExtension.class)
class DelayedStartStackHandlerTest {

    private static final long DELAY = 6L;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private EventBus eventBus;

    @Mock
    private DelayedExecutorService delayedExecutorService;

    @InjectMocks
    private DelayedStartStackHandler underTest;

    @Test
    public void testDelayedStopMultipleInstance() throws ExecutionException, InterruptedException {
        CloudContext cloudContext = mock(CloudContext.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        CloudInstance cloudInstance1 = mock(CloudInstance.class);
        CloudInstance cloudInstance2 = mock(CloudInstance.class);
        CloudInstance cloudInstance3 = mock(CloudInstance.class);
        List<CloudInstance> cloudInstances = List.of(cloudInstance1, cloudInstance2, cloudInstance3);
        CloudConnector cloudConnector = mock(CloudConnector.class);
        when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
        Authenticator authenticator = mock(Authenticator.class);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        when(authenticator.authenticate(any(), any())).thenReturn(authenticatedContext);
        InstanceConnector instanceConnector = mock(InstanceConnector.class);
        when(cloudConnector.instances()).thenReturn(instanceConnector);
        when(delayedExecutorService.runWithDelay(any(Callable.class), eq(DELAY), eq(TimeUnit.SECONDS)))
                .thenAnswer(inv -> inv.getArgument(0, Callable.class).call());

        underTest.accept(new Event<>(new DelayedStartInstancesRequest(cloudContext, cloudCredential, List.of(), cloudInstances, DELAY)));

        ArgumentCaptor<List<CloudInstance>> instanceCaptor = ArgumentCaptor.forClass(List.class);
        verify(instanceConnector, times(3)).start(eq(authenticatedContext), eq(List.of()), instanceCaptor.capture());
        List<List<CloudInstance>> instanceCaptorAllValues = instanceCaptor.getAllValues();
        assertEquals(3, instanceCaptorAllValues.size());
        assertEquals(1, instanceCaptorAllValues.get(0).size());
        assertEquals(cloudInstance1, instanceCaptorAllValues.get(0).get(0));
        assertEquals(1, instanceCaptorAllValues.get(1).size());
        assertEquals(cloudInstance2, instanceCaptorAllValues.get(1).get(0));
        assertEquals(1, instanceCaptorAllValues.get(2).size());
        assertEquals(cloudInstance3, instanceCaptorAllValues.get(2).get(0));
        verify(delayedExecutorService, times(2)).runWithDelay(any(Callable.class), eq(DELAY), eq(TimeUnit.SECONDS));
        verify(eventBus).notify(eq(StartInstancesResult.class.getSimpleName().toUpperCase(Locale.ROOT)), any(Event.class));
    }

    @Test
    public void testDelayedStopOneInstance() {
        CloudContext cloudContext = mock(CloudContext.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        CloudInstance cloudInstance1 = mock(CloudInstance.class);
        List<CloudInstance> cloudInstances = List.of(cloudInstance1);
        CloudConnector cloudConnector = mock(CloudConnector.class);
        when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
        Authenticator authenticator = mock(Authenticator.class);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        when(authenticator.authenticate(any(), any())).thenReturn(authenticatedContext);
        InstanceConnector instanceConnector = mock(InstanceConnector.class);
        when(cloudConnector.instances()).thenReturn(instanceConnector);

        underTest.accept(new Event<>(new DelayedStartInstancesRequest(cloudContext, cloudCredential, List.of(), cloudInstances, DELAY)));

        ArgumentCaptor<List<CloudInstance>> instanceCaptor = ArgumentCaptor.forClass(List.class);
        verify(instanceConnector, times(1)).start(eq(authenticatedContext), eq(List.of()), instanceCaptor.capture());
        List<List<CloudInstance>> instanceCaptorAllValues = instanceCaptor.getAllValues();
        assertEquals(1, instanceCaptorAllValues.size());
        assertEquals(1, instanceCaptorAllValues.get(0).size());
        verifyNoInteractions(delayedExecutorService);
        verify(eventBus).notify(eq(StartInstancesResult.class.getSimpleName().toUpperCase(Locale.ROOT)), any(Event.class));
    }

    @Test
    public void testDelayedStopNoInstance() {
        CloudContext cloudContext = mock(CloudContext.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        List<CloudInstance> cloudInstances = List.of();
        CloudConnector cloudConnector = mock(CloudConnector.class);
        when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
        Authenticator authenticator = mock(Authenticator.class);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        when(authenticator.authenticate(any(), any())).thenReturn(authenticatedContext);

        underTest.accept(new Event<>(new DelayedStartInstancesRequest(cloudContext, cloudCredential, List.of(), cloudInstances, DELAY)));

        verifyNoInteractions(delayedExecutorService);
        verify(cloudConnector, never()).instances();
        verify(eventBus).notify(eq(StartInstancesResult.class.getSimpleName().toUpperCase(Locale.ROOT)), any(Event.class));
    }

    @Test
    public void testExceptionThrown() {
        CloudContext cloudContext = mock(CloudContext.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        List<CloudInstance> cloudInstances = List.of();
        CloudConnector cloudConnector = mock(CloudConnector.class);
        when(cloudPlatformConnectors.get(any())).thenThrow(new RuntimeException("test ex"));

        underTest.accept(new Event<>(new DelayedStartInstancesRequest(cloudContext, cloudCredential, List.of(), cloudInstances, DELAY)));

        verifyNoInteractions(delayedExecutorService);
        verify(cloudConnector, never()).instances();
        verify(eventBus).notify(eq(StartInstancesResult.class.getSimpleName().toUpperCase(Locale.ROOT) + "_ERROR"), any(Event.class));
    }

    @Test
    public void testAccept() {
        assertEquals(DelayedStartInstancesRequest.class, underTest.type());
    }
}
