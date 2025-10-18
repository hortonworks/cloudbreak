package com.sequenceiq.redbeams.flow.redbeams.start.handler;

import static com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus.STARTED;
import static com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus.STOPPED;
import static com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus.UPDATE_IN_PROGRESS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus;
import com.sequenceiq.cloudbreak.cloud.model.SpecialParameters;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.task.PollTaskFactory;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.service.executor.DelayedExecutorService;
import com.sequenceiq.redbeams.flow.redbeams.start.event.StartDatabaseServerFailed;
import com.sequenceiq.redbeams.flow.redbeams.start.event.StartDatabaseServerRequest;
import com.sequenceiq.redbeams.flow.redbeams.start.event.StartDatabaseServerSuccess;

@ExtendWith(MockitoExtension.class)
class StartDatabaseServerHandlerTest {

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
    private PlatformParameters platformParameters;

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
    private Optional<DelayedExecutorService> delayedExecutorService;

    @InjectMocks
    private StartDatabaseServerHandler victim;

    @BeforeEach
    void initTests() {
        when(cloudContext.getPlatformVariant()).thenReturn(cloudPlatformVariant);
        when(cloudPlatformConnectors.get(cloudPlatformVariant)).thenReturn(cloudConnector);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        when(cloudConnector.resources()).thenReturn(resourceConnector);
        lenient().when(delayedExecutorService.isPresent()).thenReturn(Boolean.FALSE);
        lenient().when(cloudConnector.parameters()).thenReturn(platformParameters);
        lenient().when(platformParameters.specialParameters())
                .thenReturn(new SpecialParameters(Map.of(PlatformParametersConsts.DELAY_DATABASE_START, Boolean.FALSE)));
        when(dbStack.getDatabaseServer()).thenReturn(DatabaseServer.builder().withServerId("serverId").build());
    }

    @Test
    void shouldCallStartDatabaseFromStoppedAndNotifyEventBusWithWaitingForPermanentStatus() throws Exception {
        when(resourceConnector.getDatabaseServerStatus(authenticatedContext, dbStack)).thenReturn(STOPPED, STARTED);
        when(statusCheckFactory.newPollPermanentExternalDatabaseStateTask(authenticatedContext, dbStack))
                .thenReturn(externalDatabaseStatusPollTask);
        when(externalDatabaseStatusSyncPollingScheduler.schedule(externalDatabaseStatusPollTask)).thenReturn(STARTED);

        victim.accept(anEvent());

        verify(resourceConnector, times(1)).startDatabaseServer(authenticatedContext, dbStack);
        verify(eventBus, times(1)).notify(eq(StartDatabaseServerSuccess.class.getSimpleName().toUpperCase(Locale.ROOT)), any(Event.class));
    }

    @Test
    void shouldCallStartDatabaseAndNotifyEventBusWithWaitingForPermanentStatus() throws Exception {
        when(resourceConnector.getDatabaseServerStatus(authenticatedContext, dbStack)).thenReturn(UPDATE_IN_PROGRESS);
        when(statusCheckFactory.newPollPermanentExternalDatabaseStateTask(authenticatedContext, dbStack))
                .thenReturn(externalDatabaseStatusPollTask);
        when(externalDatabaseStatusSyncPollingScheduler.schedule(externalDatabaseStatusPollTask)).thenReturn(STOPPED, STARTED);
        victim.accept(anEvent());

        verify(resourceConnector, times(1)).startDatabaseServer(authenticatedContext, dbStack);
        verify(eventBus, times(1)).notify(eq(StartDatabaseServerSuccess.class.getSimpleName().toUpperCase(Locale.ROOT)), any(Event.class));
    }

    @Test
    void shouldNotCallStartDatabaseButShouldNotifyEventBus() throws Exception {
        when(resourceConnector.getDatabaseServerStatus(authenticatedContext, dbStack)).thenReturn(ExternalDatabaseStatus.STARTED);

        victim.accept(anEvent());

        verify(eventBus).notify(eq(StartDatabaseServerSuccess.class.getSimpleName().toUpperCase(Locale.ROOT)), any(Event.class));
        verify(resourceConnector).getDatabaseServerStatus(any(AuthenticatedContext.class), eq(dbStack));
        verifyNoMoreInteractions(resourceConnector);
        verifyNoInteractions(statusCheckFactory, externalDatabaseStatusSyncPollingScheduler);
    }

    @Test
    void shouldCallStartDatabaseAndNotifyEventBusOnFailure() throws Exception {
        when(resourceConnector.getDatabaseServerStatus(authenticatedContext, dbStack)).thenReturn(STOPPED);
        doThrow(new Exception()).when(resourceConnector).startDatabaseServer(authenticatedContext, dbStack);

        victim.accept(anEvent());

        verify(eventBus).notify(eq(StartDatabaseServerFailed.class.getSimpleName().toUpperCase(Locale.ROOT)), any(Event.class));
    }

    private Event<StartDatabaseServerRequest> anEvent() {
        return new Event<>(aStartDatabaseServerRequest());
    }

    private StartDatabaseServerRequest aStartDatabaseServerRequest() {
        return new StartDatabaseServerRequest(cloudContext, cloudCredential, dbStack);
    }
}

