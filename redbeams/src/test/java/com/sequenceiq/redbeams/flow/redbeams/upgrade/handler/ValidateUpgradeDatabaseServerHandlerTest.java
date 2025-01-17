package com.sequenceiq.redbeams.flow.redbeams.upgrade.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

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
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.common.converter.ResourceNameGenerator;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.redbeams.converter.spi.DBStackToDatabaseStackConverter;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.DatabaseServer;
import com.sequenceiq.redbeams.dto.UpgradeDatabaseMigrationParams;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.RedbeamsValidateUpgradeFailedEvent;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.ValidateUpgradeDatabaseServerRequest;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.ValidateUpgradeDatabaseServerSuccess;
import com.sequenceiq.redbeams.service.stack.DBStackService;
import com.sequenceiq.redbeams.service.upgrade.DatabaseAutoMigrationUpdater;
import com.sequenceiq.redbeams.service.validation.DatabaseEncryptionValidator;

@ExtendWith(MockitoExtension.class)
class ValidateUpgradeDatabaseServerHandlerTest {

    private static final long RESOURCE_ID = 1L;

    @InjectMocks
    private ValidateUpgradeDatabaseServerHandler underTest;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private DBStackService dbStackService;

    @Mock
    private PersistenceNotifier persistenceNotifier;

    @Mock
    private DatabaseEncryptionValidator databaseEncryptionValidator;

    @Mock
    private DBStackToDatabaseStackConverter databaseStackConverter;

    @Mock
    private DatabaseAutoMigrationUpdater databaseAutoMigrationUpdater;

    @Mock
    private ResourceNameGenerator nameGenerator;

    @Mock
    private DatabaseStack databaseStack;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudConnector connector;

    @Mock
    private CloudPlatformVariant cloudPlatformVariant;

    @Mock
    private Authenticator authenticator;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private ResourceConnector resourceConnector;

    @Mock
    private DBStack dbStack;

    @Mock
    private UpgradeDatabaseMigrationParams migrationParams;

    @Mock
    private TargetMajorVersion targetMajorVersion;

    @Mock
    private DatabaseServer databaseServer;

    @Test
    void testSelector() {
        assertEquals("VALIDATEUPGRADEDATABASESERVERREQUEST", underTest.selector());
    }

    @Test
    void testDefaultFailureEvent() {
        Selectable actual = underTest.defaultFailureEvent(RESOURCE_ID, new Exception(), mock(Event.class));
        assertEquals("REDBEAMSVALIDATEUPGRADEFAILEDEVENT", actual.selector());
    }

    @Test
    void testDoAcceptShouldReturnSuccessEventWithoutMigration() throws Exception {
        when(cloudContext.getPlatformVariant()).thenReturn(cloudPlatformVariant);
        when(cloudPlatformConnectors.get(cloudPlatformVariant)).thenReturn(connector);
        when(dbStackService.getById(0L)).thenReturn(dbStack);
        when(connector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        when(connector.resources()).thenReturn(resourceConnector);

        Selectable actual = underTest.doAccept(createEvent(false));

        assertInstanceOf(ValidateUpgradeDatabaseServerSuccess.class, actual);
        verify(resourceConnector).validateUpgradeDatabaseServer(authenticatedContext, databaseStack, targetMajorVersion);
        verify(resourceConnector, never()).launchValidateUpgradeDatabaseServerResources(eq(authenticatedContext), eq(databaseStack), eq(targetMajorVersion),
                any(), eq(persistenceNotifier));
        verifyNoInteractions(nameGenerator);
    }

    @Test
    void testDoAcceptShouldReturnSuccessEventWithMigration() throws Exception {
        when(cloudContext.getPlatformVariant()).thenReturn(cloudPlatformVariant);
        when(cloudPlatformConnectors.get(cloudPlatformVariant)).thenReturn(connector);
        when(dbStackService.getById(0L)).thenReturn(dbStack);
        when(dbStack.copy()).thenReturn(dbStack);
        databaseServer = mock(DatabaseServer.class);
        when(dbStack.getDatabaseServer()).thenReturn(databaseServer);
        when(connector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        when(connector.resources()).thenReturn(resourceConnector);
        when(databaseStackConverter.convert(dbStack)).thenReturn(databaseStack);
        when(resourceConnector.launchValidateUpgradeDatabaseServerResources(eq(authenticatedContext), eq(databaseStack), eq(targetMajorVersion), any(),
                eq(persistenceNotifier)))
                .thenReturn(List.of(mock(CloudResourceStatus.class)));
        when(targetMajorVersion.getMajorVersion()).thenReturn("11");
        when(dbStack.getEnvironmentId()).thenReturn("env_id");
        when(dbStack.getResourceCrn()).thenReturn("resource_id");

        Selectable actual = underTest.doAccept(createEvent(true));

        assertInstanceOf(ValidateUpgradeDatabaseServerSuccess.class, actual);
        verify(resourceConnector).validateUpgradeDatabaseServer(authenticatedContext, databaseStack, targetMajorVersion);
        verify(resourceConnector).launchValidateUpgradeDatabaseServerResources(eq(authenticatedContext), eq(databaseStack), eq(targetMajorVersion), any(),
                eq(persistenceNotifier));
        verify(nameGenerator).generateHashBasedName(APIResourceType.DATABASE_SERVER, Optional.of("resource_id11"));
    }

    @Test
    void testDoAcceptShouldReturnFailureEvent() throws Exception {
        when(cloudContext.getPlatformVariant()).thenReturn(cloudPlatformVariant);
        when(cloudPlatformConnectors.get(cloudPlatformVariant)).thenReturn(connector);
        when(dbStackService.getById(0L)).thenReturn(dbStack);
        when(connector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        when(connector.resources()).thenReturn(resourceConnector);
        doThrow(CloudConnectorException.class)
                .when(resourceConnector).validateUpgradeDatabaseServer(authenticatedContext, databaseStack, targetMajorVersion);

        Selectable actual = underTest.doAccept(createEvent(false));

        assertInstanceOf(RedbeamsValidateUpgradeFailedEvent.class, actual);
        verify(resourceConnector).validateUpgradeDatabaseServer(authenticatedContext, databaseStack, targetMajorVersion);
    }

    private HandlerEvent<ValidateUpgradeDatabaseServerRequest> createEvent(boolean withMigration) {
        ValidateUpgradeDatabaseServerRequest request = new ValidateUpgradeDatabaseServerRequest(cloudContext, cloudCredential, databaseStack,
                targetMajorVersion, withMigration ? migrationParams : null);
        return new HandlerEvent<>(new Event<>(request));
    }
}