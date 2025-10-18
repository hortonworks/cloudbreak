package com.sequenceiq.redbeams.flow.redbeams.stop.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.task.PollTaskFactory;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.redbeams.flow.redbeams.stop.event.StopDatabaseServerFailed;
import com.sequenceiq.redbeams.flow.redbeams.stop.event.StopDatabaseServerRequest;
import com.sequenceiq.redbeams.flow.redbeams.stop.event.StopDatabaseServerSuccess;
import com.sequenceiq.redbeams.service.rotate.CloudProviderCertRotator;

@ExtendWith(MockitoExtension.class)
class StopDatabaseServerHandlerTest {

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
    private CloudConnector cloudConnector;

    @Mock
    private Authenticator authenticator;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private ResourceConnector resourceConnector;

    @Mock
    private SyncPollingScheduler<ExternalDatabaseStatus> externalDatabaseStatusSyncPollingScheduler;

    @Mock
    private PollTaskFactory statusCheckFactory;

    @Mock
    private PollTask<ExternalDatabaseStatus> externalDatabaseStatusPollTask;

    @Mock
    private CloudProviderCertRotator cloudProviderCertRotator;

    @InjectMocks
    private StopDatabaseServerHandler victim;

    @BeforeEach
    void initTests() {
        when(cloudContext.getPlatformVariant()).thenReturn(cloudPlatformVariant);
        when(cloudPlatformConnectors.get(cloudPlatformVariant)).thenReturn(cloudConnector);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        when(cloudConnector.resources()).thenReturn(resourceConnector);
        when(dbStack.getDatabaseServer()).thenReturn(DatabaseServer.builder().withServerId("serverId").build());
    }

    @Test
    void shouldCallStopDatabaseAndNotifyEventBusWithoutWaitingForPermanentStatus() throws Exception {
        when(resourceConnector.getDatabaseServerStatus(authenticatedContext, dbStack)).thenReturn(ExternalDatabaseStatus.STARTED);
        doNothing().when(cloudProviderCertRotator).rotate(anyLong(), any(), any(), any(), anyBoolean());

        victim.accept(anEvent());

        verify(resourceConnector).stopDatabaseServer(authenticatedContext, dbStack);
        verify(eventBus).notify(eq(StopDatabaseServerSuccess.class.getSimpleName().toUpperCase(Locale.ROOT)), Mockito.any(Event.class));
        verifyNoInteractions(statusCheckFactory, externalDatabaseStatusSyncPollingScheduler);
    }

    @Test
    void shouldCallStopDatabaseAndNotifyEventBusWithWaitingForPermanentStatus() throws Exception {
        when(resourceConnector.getDatabaseServerStatus(authenticatedContext, dbStack)).thenReturn(ExternalDatabaseStatus.UPDATE_IN_PROGRESS);
        when(statusCheckFactory.newPollPermanentExternalDatabaseStateTask(authenticatedContext, dbStack))
                .thenReturn(externalDatabaseStatusPollTask);
        doNothing().when(cloudProviderCertRotator).rotate(anyLong(), any(), any(), any(), anyBoolean());
        when(externalDatabaseStatusSyncPollingScheduler.schedule(externalDatabaseStatusPollTask)).thenReturn(ExternalDatabaseStatus.STARTED);
        victim.accept(anEvent());

        verify(resourceConnector).stopDatabaseServer(authenticatedContext, dbStack);
        verify(eventBus).notify(eq(StopDatabaseServerSuccess.class.getSimpleName().toUpperCase(Locale.ROOT)), Mockito.any(Event.class));
    }

    @Test
    void shouldNotCallStopDatabaseButShouldNotifyEventBus() throws Exception {
        when(resourceConnector.getDatabaseServerStatus(authenticatedContext, dbStack)).thenReturn(ExternalDatabaseStatus.STOPPED);

        victim.accept(anEvent());

        verify(eventBus).notify(eq(StopDatabaseServerSuccess.class.getSimpleName().toUpperCase(Locale.ROOT)), Mockito.any(Event.class));
        verify(resourceConnector).getDatabaseServerStatus(any(AuthenticatedContext.class), eq(dbStack));
        verifyNoMoreInteractions(resourceConnector);
        verifyNoInteractions(statusCheckFactory, externalDatabaseStatusSyncPollingScheduler);
    }

    @Test
    void shouldCallStopDatabaseAndNotifyEventBusOnFailure() throws Exception {
        when(resourceConnector.getDatabaseServerStatus(authenticatedContext, dbStack)).thenReturn(ExternalDatabaseStatus.STARTED);
        doThrow(new Exception()).when(resourceConnector).stopDatabaseServer(authenticatedContext, dbStack);
        doNothing().when(cloudProviderCertRotator).rotate(anyLong(), any(), any(), any(), anyBoolean());

        victim.accept(anEvent());

        verify(eventBus).notify(eq(StopDatabaseServerFailed.class.getSimpleName().toUpperCase(Locale.ROOT)), Mockito.any(Event.class));
    }

    private Event<StopDatabaseServerRequest> anEvent() {
        return new Event<>(aStopDatabaseServerRequest());
    }

    private StopDatabaseServerRequest aStopDatabaseServerRequest() {
        return new StopDatabaseServerRequest(cloudContext, cloudCredential, dbStack);
    }
}

