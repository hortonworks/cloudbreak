package com.sequenceiq.redbeams.flow.redbeams.sslmigration.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
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
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandlerTestSupport;
import com.sequenceiq.redbeams.exception.RedbeamsException;
import com.sequenceiq.redbeams.flow.redbeams.sslmigration.event.RedbeamsSslMigrationFailed;
import com.sequenceiq.redbeams.flow.redbeams.sslmigration.event.RedbeamsSslMigrationHandlerRequest;
import com.sequenceiq.redbeams.flow.redbeams.sslmigration.event.RedbeamsSslMigrationHandlerSuccessResult;

@ExtendWith(MockitoExtension.class)
class RedbeamsSslMigrationHandlerTest {

    private static final Long RESOURCE_ID = 123L;

    private static final String SERVER_ID = "server-id-123";

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private Event<RedbeamsSslMigrationHandlerRequest> event;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudPlatformVariant cloudPlatformVariant;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private DatabaseStack databaseStack;

    @Mock
    private DatabaseServer databaseServer;

    @Mock
    private CloudConnector cloudConnector;

    @Mock
    private Authenticator authenticator;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private ResourceConnector resourceConnector;

    @InjectMocks
    private RedbeamsSslMigrationHandler underTest;

    @BeforeEach
    void setUp() {
        lenient().when(cloudContext.getId()).thenReturn(RESOURCE_ID);
        lenient().when(cloudContext.getPlatformVariant()).thenReturn(cloudPlatformVariant);
        lenient().when(databaseStack.getDatabaseServer()).thenReturn(databaseServer);
        lenient().when(databaseServer.getServerId()).thenReturn(SERVER_ID);
    }

    @Test
    void testSelector() {
        assertThat(underTest.selector()).isEqualTo("REDBEAMSSSLMIGRATIONHANDLERREQUEST");
    }

    @Test
    void testDefaultFailureEvent() {
        Exception exception = new Exception("Test exception");

        Selectable selectable = underTest.defaultFailureEvent(RESOURCE_ID, exception, event);

        verifyFailureEvent(selectable, exception);
    }

    @Test
    void testDoAcceptSuccess() throws Exception {
        RedbeamsSslMigrationHandlerRequest request = new RedbeamsSslMigrationHandlerRequest(
                RESOURCE_ID,
                cloudContext,
                cloudCredential,
                databaseStack);
        when(event.getData()).thenReturn(request);

        when(cloudPlatformConnectors.get(cloudPlatformVariant)).thenReturn(cloudConnector);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        when(cloudConnector.resources()).thenReturn(resourceConnector);
        when(resourceConnector.getDatabaseServerStatus(authenticatedContext, databaseStack))
                .thenReturn(ExternalDatabaseStatus.STARTED);

        Selectable selectable = new ExceptionCatcherEventHandlerTestSupport<>(underTest).doAccept(event);

        assertInstanceOf(RedbeamsSslMigrationHandlerSuccessResult.class, selectable);
        RedbeamsSslMigrationHandlerSuccessResult successResult = (RedbeamsSslMigrationHandlerSuccessResult) selectable;
        assertEquals(RESOURCE_ID, successResult.getResourceId());

        verify(resourceConnector).migrateDatabaseFromNonSslToSsl(authenticatedContext, databaseStack);
    }

    @Test
    void testDoAcceptFailureWhenDatabaseNotStarted() throws Exception {
        RedbeamsSslMigrationHandlerRequest request = new RedbeamsSslMigrationHandlerRequest(
                RESOURCE_ID,
                cloudContext,
                cloudCredential,
                databaseStack);
        when(event.getData()).thenReturn(request);

        when(cloudPlatformConnectors.get(cloudPlatformVariant)).thenReturn(cloudConnector);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        when(cloudConnector.resources()).thenReturn(resourceConnector);
        when(resourceConnector.getDatabaseServerStatus(authenticatedContext, databaseStack))
                .thenReturn(ExternalDatabaseStatus.STOPPED);

        Selectable selectable = new ExceptionCatcherEventHandlerTestSupport<>(underTest).doAccept(event);

        verifyFailureEvent(selectable);
        RedbeamsSslMigrationFailed failedEvent = (RedbeamsSslMigrationFailed) selectable;
        assertInstanceOf(RedbeamsException.class, failedEvent.getException());
        assertThat(failedEvent.getException().getMessage())
                .contains("Database server " + SERVER_ID + " is not in started status");

        verify(resourceConnector, never()).migrateDatabaseFromNonSslToSsl(any(), any());
    }

    @Test
    void testDoAcceptFailureWhenAuthenticationFails() throws Exception {
        RedbeamsSslMigrationHandlerRequest request = new RedbeamsSslMigrationHandlerRequest(
                RESOURCE_ID,
                cloudContext,
                cloudCredential,
                databaseStack);
        when(event.getData()).thenReturn(request);

        Exception exception = new RuntimeException("Authentication failed");
        when(cloudPlatformConnectors.get(cloudPlatformVariant)).thenReturn(cloudConnector);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(cloudContext, cloudCredential)).thenThrow(exception);

        Selectable selectable = new ExceptionCatcherEventHandlerTestSupport<>(underTest).doAccept(event);

        verifyFailureEvent(selectable, exception);
        verify(resourceConnector, never()).migrateDatabaseFromNonSslToSsl(any(), any());
    }

    @Test
    void testDoAcceptFailureWhenMigrationFails() throws Exception {
        RedbeamsSslMigrationHandlerRequest request = new RedbeamsSslMigrationHandlerRequest(
                RESOURCE_ID,
                cloudContext,
                cloudCredential,
                databaseStack);
        when(event.getData()).thenReturn(request);

        when(cloudPlatformConnectors.get(cloudPlatformVariant)).thenReturn(cloudConnector);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        when(cloudConnector.resources()).thenReturn(resourceConnector);
        when(resourceConnector.getDatabaseServerStatus(authenticatedContext, databaseStack))
                .thenReturn(ExternalDatabaseStatus.STARTED);

        Exception exception = new RuntimeException("Migration failed");
        doThrow(exception).when(resourceConnector).migrateDatabaseFromNonSslToSsl(authenticatedContext, databaseStack);

        Selectable selectable = new ExceptionCatcherEventHandlerTestSupport<>(underTest).doAccept(event);

        verifyFailureEvent(selectable, exception);
    }

    @Test
    void testDoAcceptFailureWhenStatusCheckFails() throws Exception {
        RedbeamsSslMigrationHandlerRequest request = new RedbeamsSslMigrationHandlerRequest(
                RESOURCE_ID,
                cloudContext,
                cloudCredential,
                databaseStack);
        when(event.getData()).thenReturn(request);

        when(cloudPlatformConnectors.get(cloudPlatformVariant)).thenReturn(cloudConnector);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        when(cloudConnector.resources()).thenReturn(resourceConnector);

        Exception exception = new RuntimeException("Status check failed");
        when(resourceConnector.getDatabaseServerStatus(authenticatedContext, databaseStack)).thenThrow(exception);

        Selectable selectable = new ExceptionCatcherEventHandlerTestSupport<>(underTest).doAccept(event);

        verifyFailureEvent(selectable, exception);
        verify(resourceConnector, never()).migrateDatabaseFromNonSslToSsl(any(), any());
    }

    @Test
    void testDoAcceptSuccessWithDatabaseStatusUpdateInProgress() throws Exception {
        RedbeamsSslMigrationHandlerRequest request = new RedbeamsSslMigrationHandlerRequest(
                RESOURCE_ID,
                cloudContext,
                cloudCredential,
                databaseStack);
        when(event.getData()).thenReturn(request);

        when(cloudPlatformConnectors.get(cloudPlatformVariant)).thenReturn(cloudConnector);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        when(cloudConnector.resources()).thenReturn(resourceConnector);
        when(resourceConnector.getDatabaseServerStatus(authenticatedContext, databaseStack))
                .thenReturn(ExternalDatabaseStatus.UPDATE_IN_PROGRESS);

        Selectable selectable = new ExceptionCatcherEventHandlerTestSupport<>(underTest).doAccept(event);

        verifyFailureEvent(selectable);
        RedbeamsSslMigrationFailed failedEvent = (RedbeamsSslMigrationFailed) selectable;
        assertInstanceOf(RedbeamsException.class, failedEvent.getException());

        verify(resourceConnector, never()).migrateDatabaseFromNonSslToSsl(any(), any());
    }

    private void verifyFailureEvent(Selectable selectable) {
        assertInstanceOf(RedbeamsSslMigrationFailed.class, selectable);
        RedbeamsSslMigrationFailed failedEvent = (RedbeamsSslMigrationFailed) selectable;
        assertEquals(RESOURCE_ID, failedEvent.getResourceId());
    }

    private void verifyFailureEvent(Selectable selectable, Exception expectedException) {
        verifyFailureEvent(selectable);
        RedbeamsSslMigrationFailed failedEvent = (RedbeamsSslMigrationFailed) selectable;
        assertEquals(expectedException, failedEvent.getException());
    }
}
