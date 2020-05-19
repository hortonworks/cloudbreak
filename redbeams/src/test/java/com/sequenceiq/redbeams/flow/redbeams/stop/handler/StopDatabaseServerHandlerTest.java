package com.sequenceiq.redbeams.flow.redbeams.stop.handler;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.task.PollTaskFactory;
import com.sequenceiq.redbeams.flow.redbeams.stop.event.StopDatabaseServerFailed;
import com.sequenceiq.redbeams.flow.redbeams.stop.event.StopDatabaseServerRequest;
import com.sequenceiq.redbeams.flow.redbeams.stop.event.StopDatabaseServerSuccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.bus.Event;
import reactor.bus.EventBus;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class StopDatabaseServerHandlerTest {

    @Mock
    private DatabaseStack dbStack;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private EventBus eventBus;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private CloudPlatformVariant cloudPlatformVariant;

    @Mock
    private CloudConnector<Object> cloudConnector;

    @Mock
    private Authenticator authenticator;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private ResourceConnector<Object> resourceConnector;

    @Mock
    private SyncPollingScheduler<ExternalDatabaseStatus> externalDatabaseStatusSyncPollingScheduler;

    @Mock
    private PollTaskFactory statusCheckFactory;

    @Mock
    private PollTask<ExternalDatabaseStatus> externalDatabaseStatusPollTask;

    @InjectMocks
    private StopDatabaseServerHandler victim;

    @BeforeEach
    public void initTests() {
        when(cloudContext.getPlatformVariant()).thenReturn(cloudPlatformVariant);
        when(cloudPlatformConnectors.get(cloudPlatformVariant)).thenReturn(cloudConnector);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        when(cloudConnector.resources()).thenReturn(resourceConnector);
    }

    @Test
    public void shouldCallStopDatabaseAndNotifyEventBusWithoutWaitingForPermanentStatus() throws Exception {
        when(resourceConnector.getDatabaseServerStatus(authenticatedContext, dbStack)).thenReturn(ExternalDatabaseStatus.STARTED);

        victim.accept(anEvent());

        verify(resourceConnector).stopDatabaseServer(authenticatedContext, dbStack);
        verify(eventBus).notify(eq(StopDatabaseServerSuccess.class.getSimpleName().toUpperCase()), Mockito.any(Event.class));
        verifyZeroInteractions(statusCheckFactory, externalDatabaseStatusSyncPollingScheduler);
    }

    @Test
    public void shouldCallStopDatabaseAndNotifyEventBusWithWaitingForPermanentStatus() throws Exception {
        when(resourceConnector.getDatabaseServerStatus(authenticatedContext, dbStack)).thenReturn(ExternalDatabaseStatus.UPDATE_IN_PROGRESS);
        when(statusCheckFactory.newPollPermanentExternalDatabaseStateTask(authenticatedContext, dbStack))
                .thenReturn(externalDatabaseStatusPollTask);
        when(externalDatabaseStatusSyncPollingScheduler.schedule(externalDatabaseStatusPollTask)).thenReturn(ExternalDatabaseStatus.STARTED);
        victim.accept(anEvent());

        verify(resourceConnector).stopDatabaseServer(authenticatedContext, dbStack);
        verify(eventBus).notify(eq(StopDatabaseServerSuccess.class.getSimpleName().toUpperCase()), Mockito.any(Event.class));
    }

    @Test
    public void shouldNotCallStopDatabaseButShouldNotifyEventBus() throws Exception {
        when(resourceConnector.getDatabaseServerStatus(authenticatedContext, dbStack)).thenReturn(ExternalDatabaseStatus.STOPPED);

        victim.accept(anEvent());

        verify(eventBus).notify(eq(StopDatabaseServerSuccess.class.getSimpleName().toUpperCase()), Mockito.any(Event.class));
        verifyZeroInteractions(resourceConnector, statusCheckFactory, externalDatabaseStatusSyncPollingScheduler);
    }

    @Test
    public void shouldCallStopDatabaseAndNotifyEventBusOnFailure() throws Exception {
        when(resourceConnector.getDatabaseServerStatus(authenticatedContext, dbStack)).thenReturn(ExternalDatabaseStatus.STARTED);
        doThrow(new Exception()).when(resourceConnector).stopDatabaseServer(authenticatedContext, dbStack);

        victim.accept(anEvent());

        verify(eventBus).notify(eq(StopDatabaseServerFailed.class.getSimpleName().toUpperCase()), Mockito.any(Event.class));
    }

    private Event<StopDatabaseServerRequest> anEvent() {
        Event<StopDatabaseServerRequest> event = new Event<>(StopDatabaseServerRequest.class);
        event.setData(aStopDatabaseServerRequest());

        return event;
    }

    private StopDatabaseServerRequest aStopDatabaseServerRequest() {
        return new StopDatabaseServerRequest(cloudContext, cloudCredential, dbStack);
    }
}