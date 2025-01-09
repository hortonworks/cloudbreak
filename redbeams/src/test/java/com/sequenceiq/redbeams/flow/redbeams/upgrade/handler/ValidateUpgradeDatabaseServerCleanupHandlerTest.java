package com.sequenceiq.redbeams.flow.redbeams.upgrade.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

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
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.RedbeamsValidateUpgradeCleanupFailedEvent;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.ValidateUpgradeDatabaseServerCleanupRequest;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.ValidateUpgradeDatabaseServerCleanupSuccess;
import com.sequenceiq.redbeams.service.stack.DBResourceService;

@ExtendWith(MockitoExtension.class)
class ValidateUpgradeDatabaseServerCleanupHandlerTest {

    private static final long RESOURCE_ID = 1L;

    @InjectMocks
    private ValidateUpgradeDatabaseServerCleanupHandler underTest;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private PersistenceNotifier persistenceNotifier;

    @Mock
    private DBResourceService dbResourceService;

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

    @Test
    void testSelector() {
        assertEquals("VALIDATEUPGRADEDATABASESERVERCLEANUPREQUEST", underTest.selector());
    }

    @Test
    void testDefaultFailureEvent() {
        Selectable actual = underTest.defaultFailureEvent(RESOURCE_ID, new Exception(), mock(Event.class));
        assertEquals("REDBEAMSVALIDATEUPGRADECLEANUPFAILEDEVENT", actual.selector());
    }

    @Test
    void testDoAcceptShouldReturnSuccessEvent() throws Exception {
        List<CloudResource> cloudResources = List.of(mock(CloudResource.class));
        when(cloudContext.getPlatformVariant()).thenReturn(cloudPlatformVariant);
        when(cloudPlatformConnectors.get(cloudPlatformVariant)).thenReturn(connector);
        when(dbResourceService.getAllAsCloudResource(RESOURCE_ID)).thenReturn(cloudResources);
        when(connector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        when(connector.resources()).thenReturn(resourceConnector);

        Selectable actual = underTest.doAccept(createEvent());

        assertInstanceOf(ValidateUpgradeDatabaseServerCleanupSuccess.class, actual);
        verify(resourceConnector).cleanupValidateUpgradeDatabaseServerResources(authenticatedContext, databaseStack, cloudResources, persistenceNotifier);
    }

    @Test
    void testDoAcceptShouldReturnFailureEvent() throws Exception {
        List<CloudResource> cloudResources = List.of(mock(CloudResource.class));
        when(cloudContext.getPlatformVariant()).thenReturn(cloudPlatformVariant);
        when(cloudPlatformConnectors.get(cloudPlatformVariant)).thenReturn(connector);
        when(dbResourceService.getAllAsCloudResource(RESOURCE_ID)).thenReturn(cloudResources);
        when(connector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        when(connector.resources()).thenReturn(resourceConnector);
        doThrow(CloudConnectorException.class)
                .when(resourceConnector).cleanupValidateUpgradeDatabaseServerResources(authenticatedContext, databaseStack, cloudResources, persistenceNotifier);

        Selectable actual = underTest.doAccept(createEvent());

        assertInstanceOf(RedbeamsValidateUpgradeCleanupFailedEvent.class, actual);
        verify(resourceConnector).cleanupValidateUpgradeDatabaseServerResources(authenticatedContext, databaseStack, cloudResources, persistenceNotifier);
    }

    private HandlerEvent<ValidateUpgradeDatabaseServerCleanupRequest> createEvent() {
        when(cloudContext.getId()).thenReturn(RESOURCE_ID);
        return new HandlerEvent<>(new Event<>(new ValidateUpgradeDatabaseServerCleanupRequest(cloudContext, cloudCredential, databaseStack)));
    }

}