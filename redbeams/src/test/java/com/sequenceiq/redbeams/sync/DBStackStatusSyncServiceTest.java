package com.sequenceiq.redbeams.sync;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;
import static com.sequenceiq.common.model.AzureDatabaseType.AZURE_DATABASE_TYPE_KEY;
import static com.sequenceiq.common.model.AzureDatabaseType.FLEXIBLE_SERVER;
import static com.sequenceiq.common.model.AzureDatabaseType.SINGLE_SERVER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.api.model.common.Status;
import com.sequenceiq.redbeams.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.redbeams.converter.spi.DBStackToDatabaseStackConverter;
import com.sequenceiq.redbeams.domain.stack.DBResource;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.DatabaseServer;
import com.sequenceiq.redbeams.dto.Credential;
import com.sequenceiq.redbeams.service.CredentialService;
import com.sequenceiq.redbeams.service.stack.DBResourceService;
import com.sequenceiq.redbeams.service.stack.DBStackService;
import com.sequenceiq.redbeams.service.stack.DBStackStatusUpdater;

@ExtendWith(MockitoExtension.class)
public class DBStackStatusSyncServiceTest {

    private static final String ENVIRONMENT_ID = "environment id";

    private static final String DB_NAME = "name";

    private static final Long DB_STACK_ID = 1234L;

    private static final String SINGLE_SERVER_RES_ID = "/subscriptions/12345678-90ab-cdef-1234-567890abcdef/resourceGroups/myResourceGroup/" +
            "providers/Microsoft.DBforPostgreSQL/servers/mySqlServer";

    private static final String FLEXIBLE_SERVER_RES_ID = "/subscriptions/12345678-90ab-cdef-1234-567890abcdef/resourceGroups/myResourceGroup/" +
            "providers/Microsoft.DBforPostgreSQL/flexibleServers/mySqlServer";

    private static final String DBSVR_NAME = "dbsvr-91755aa4-9175-401a-8d45-aa13bccae329";

    @Mock
    private DatabaseStack databaseStack;

    @Mock
    private CredentialService credentialService;

    @Mock
    private CredentialToCloudCredentialConverter credentialConverter;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private DBStackStatusUpdater dbStackStatusUpdater;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private CloudConnector cloudConnector;

    @Mock
    private Authenticator authenticator;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private ResourceConnector resourceConnector;

    @Mock
    private Credential credential;

    @Mock
    private DBStack dbStack;

    @Mock
    private Crn crn;

    @Mock
    private DBStackJobService dbStackJobService;

    @Mock
    private DBStackToDatabaseStackConverter databaseStackConverter;

    @Mock
    private DatabaseServer dbServer;

    @Mock
    private DBResourceService dbResourceService;

    @Mock
    private com.sequenceiq.cloudbreak.cloud.model.DatabaseServer databaseServer;

    @Mock
    private DBStackService dbStackService;

    private ArgumentCaptor<CloudContext> cloudContextArgumentCaptor;

    @InjectMocks
    private DBStackStatusSyncService victim;

    public static Stream<Arguments> provideTestData() {
        return Stream.of(
                //Status should not be updated as saved and current are the same
                Arguments.of(Status.AVAILABLE, ExternalDatabaseStatus.STARTED, null),
                Arguments.of(Status.STOPPED, ExternalDatabaseStatus.STOPPED, null),
                //ExternalDatabaseStatus should be converted to the correct DetailedDBStackStatus and update should be applied
                Arguments.of(Status.STOPPED, ExternalDatabaseStatus.STARTED, DetailedDBStackStatus.STARTED),
                Arguments.of(Status.AVAILABLE, ExternalDatabaseStatus.START_IN_PROGRESS, DetailedDBStackStatus.START_IN_PROGRESS),
                Arguments.of(Status.AVAILABLE, ExternalDatabaseStatus.STOPPED, DetailedDBStackStatus.STOPPED),
                Arguments.of(Status.AVAILABLE, ExternalDatabaseStatus.STOP_IN_PROGRESS, DetailedDBStackStatus.STOP_IN_PROGRESS),
                //UPDATE_IN_PROGRESS status covers all non handled statuses. In this case DB Stack status should not be updated.
                Arguments.of(Status.AVAILABLE, ExternalDatabaseStatus.UPDATE_IN_PROGRESS, DetailedDBStackStatus.UNKNOWN)
        );
    }

    @BeforeEach
    public void initTests() {
        MockitoAnnotations.initMocks(this);
        cloudContextArgumentCaptor = ArgumentCaptor.forClass(CloudContext.class);

        when(dbStack.getEnvironmentId()).thenReturn(ENVIRONMENT_ID);
        when(dbStack.getResourceCrn()).thenReturn(CrnTestUtil.getDatabaseServerCrnBuilder()
                .setAccountId("acc")
                .setResource("resource")
                .build().toString());
        when(credentialService.getCredentialByEnvCrn(ENVIRONMENT_ID)).thenReturn(credential);
        when(credentialConverter.convert(credential)).thenReturn(cloudCredential);
        when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(cloudContextArgumentCaptor.capture(), Mockito.eq(cloudCredential))).thenReturn(authenticatedContext);
        when(cloudConnector.resources()).thenReturn(resourceConnector);
        when(databaseStackConverter.convert(dbStack)).thenReturn(databaseStack);
    }

    @ParameterizedTest
    @MethodSource("provideTestData")
    public void testStatusUpdate(Status savedStatus, ExternalDatabaseStatus externalDatabaseStatus, DetailedDBStackStatus newDetailedDBStackStatus)
            throws Exception {
        when(resourceConnector.getDatabaseServerStatus(authenticatedContext, databaseStack)).thenReturn(externalDatabaseStatus);
        when(dbStack.getId()).thenReturn(DB_STACK_ID);
        when(dbStack.getStatus()).thenReturn(savedStatus);
        when(dbStack.getOwnerCrn()).thenReturn(crn);

        victim.sync(dbStack);

        if (newDetailedDBStackStatus != null && newDetailedDBStackStatus.getStatus() != null) {
            verify(dbStackStatusUpdater).updateStatus(DB_STACK_ID, newDetailedDBStackStatus);
        } else {
            verifyNoInteractions(dbStackStatusUpdater);
        }
        verifyNoInteractions(dbStackJobService);
    }

    @Test
    public void shouldCheckSingleToFlexibleMigration()
            throws Exception {
        ArgumentCaptor<DatabaseStack> databaseStackCaptor = ArgumentCaptor.forClass(DatabaseStack.class);
        when(resourceConnector.getDatabaseServerStatus(eq(authenticatedContext), databaseStackCaptor.capture()))
                .thenReturn(ExternalDatabaseStatus.DELETED)
                .thenReturn(ExternalDatabaseStatus.STARTED);
        when(dbStack.getCloudPlatform()).thenReturn(AZURE.name());
        when(dbStack.getId()).thenReturn(DB_STACK_ID);
        when(dbStack.getStatus()).thenReturn(Status.AVAILABLE);
        when(dbStack.getOwnerCrn()).thenReturn(crn);
        when(dbStack.getName()).thenReturn(DB_NAME);
        when(databaseServer.getParameters()).thenReturn(Map.of(AZURE_DATABASE_TYPE_KEY, AzureDatabaseType.SINGLE_SERVER.name()));
        when(databaseStack.getDatabaseServer()).thenReturn(databaseServer);
        when(dbStack.getDatabaseServer()).thenReturn(dbServer);
        when(dbServer.getName()).thenReturn(DBSVR_NAME);
        DBResource dbResource = new DBResource.Builder().withReference(SINGLE_SERVER_RES_ID).build();
        when(dbResourceService.findByStackAndNameAndType(eq(DB_STACK_ID), eq(DBSVR_NAME), eq(ResourceType.AZURE_DATABASE))).thenReturn(Optional.of(dbResource));
        victim.sync(dbStack);

        assertEquals(FLEXIBLE_SERVER.name(), databaseStackCaptor.getAllValues().get(1).getDatabaseServer().getParameters().get(AZURE_DATABASE_TYPE_KEY));
        verify(dbStackService).save(any(DBStack.class));
        ArgumentCaptor<DBResource> dbResourceArgumentCaptor = ArgumentCaptor.forClass(DBResource.class);
        verify(dbResourceService).save(dbResourceArgumentCaptor.capture());
        assertEquals(FLEXIBLE_SERVER_RES_ID, dbResourceArgumentCaptor.getValue().getResourceReference());
        verifyNoMoreInteractions(dbStackStatusUpdater);
        verifyNoInteractions(dbStackJobService);
    }

    @Test
    public void shouldCheckSingleToFlexibleMigrationSingleStillRunning()
            throws Exception {
        ArgumentCaptor<DatabaseStack> databaseStackCaptor = ArgumentCaptor.forClass(DatabaseStack.class);
        when(resourceConnector.getDatabaseServerStatus(eq(authenticatedContext), databaseStackCaptor.capture()))
                .thenReturn(ExternalDatabaseStatus.STARTED)
                .thenReturn(ExternalDatabaseStatus.STARTED);
        when(dbStack.getCloudPlatform()).thenReturn(AZURE.name());
        when(dbStack.getId()).thenReturn(DB_STACK_ID);
        when(dbStack.getStatus()).thenReturn(Status.AVAILABLE);
        when(dbStack.getOwnerCrn()).thenReturn(crn);
        when(dbStack.getName()).thenReturn(DB_NAME);
        when(databaseServer.getParameters()).thenReturn(Map.of(AZURE_DATABASE_TYPE_KEY, AzureDatabaseType.SINGLE_SERVER.name()));
        when(databaseStack.getDatabaseServer()).thenReturn(databaseServer);
        when(dbStack.getDatabaseServer()).thenReturn(dbServer);
        when(dbServer.getName()).thenReturn(DBSVR_NAME);
        DBResource dbResource = new DBResource.Builder().withReference(SINGLE_SERVER_RES_ID).build();
        when(dbResourceService.findByStackAndNameAndType(eq(DB_STACK_ID), eq(DBSVR_NAME), eq(ResourceType.AZURE_DATABASE))).thenReturn(Optional.of(dbResource));
        victim.sync(dbStack);

        assertEquals(FLEXIBLE_SERVER.name(), databaseStackCaptor.getAllValues().get(1).getDatabaseServer().getParameters().get(AZURE_DATABASE_TYPE_KEY));
        verify(dbStackService).save(any(DBStack.class));
        ArgumentCaptor<DBResource> dbResourceArgumentCaptor = ArgumentCaptor.forClass(DBResource.class);
        verify(dbResourceService).save(dbResourceArgumentCaptor.capture());
        assertEquals(FLEXIBLE_SERVER_RES_ID, dbResourceArgumentCaptor.getValue().getResourceReference());
        verifyNoMoreInteractions(dbStackStatusUpdater);
        verifyNoInteractions(dbStackJobService);
    }

    @Test
    public void shouldCheckFlexibleToSingleRollback()
            throws Exception {
        ArgumentCaptor<DatabaseStack> databaseStackCaptor = ArgumentCaptor.forClass(DatabaseStack.class);
        when(resourceConnector.getDatabaseServerStatus(eq(authenticatedContext), databaseStackCaptor.capture()))
                .thenReturn(ExternalDatabaseStatus.DELETED)
                .thenReturn(ExternalDatabaseStatus.STARTED);
        when(dbStack.getCloudPlatform()).thenReturn(AZURE.name());
        when(dbStack.getId()).thenReturn(DB_STACK_ID);
        when(dbStack.getStatus()).thenReturn(Status.AVAILABLE);
        when(dbStack.getOwnerCrn()).thenReturn(crn);
        when(dbStack.getName()).thenReturn(DB_NAME);
        when(databaseServer.getParameters()).thenReturn(Map.of(AZURE_DATABASE_TYPE_KEY, AzureDatabaseType.SINGLE_SERVER.name()));
        when(databaseStack.getDatabaseServer()).thenReturn(databaseServer);
        when(dbStack.getDatabaseServer()).thenReturn(dbServer);
        when(dbServer.getName()).thenReturn(DBSVR_NAME);
        DBResource dbResource = new DBResource.Builder().withReference(FLEXIBLE_SERVER_RES_ID).build();
        when(dbResourceService.findByStackAndNameAndType(eq(DB_STACK_ID), eq(DBSVR_NAME), eq(ResourceType.AZURE_DATABASE))).thenReturn(Optional.of(dbResource));
        victim.sync(dbStack);

        assertEquals(SINGLE_SERVER.name(), databaseStackCaptor.getAllValues().get(1).getDatabaseServer().getParameters().get(AZURE_DATABASE_TYPE_KEY));
        verify(dbStackService).save(any(DBStack.class));
        ArgumentCaptor<DBResource> dbResourceArgumentCaptor = ArgumentCaptor.forClass(DBResource.class);
        verify(dbResourceService).save(dbResourceArgumentCaptor.capture());
        assertEquals(SINGLE_SERVER_RES_ID, dbResourceArgumentCaptor.getValue().getResourceReference());
        verifyNoMoreInteractions(dbStackStatusUpdater);
        verifyNoInteractions(dbStackJobService);
    }

    @Test
    public void shouldBeDeletedIfNeitherSingleNorFlexible()
            throws Exception {
        ArgumentCaptor<DatabaseStack> databaseStackCaptor = ArgumentCaptor.forClass(DatabaseStack.class);
        when(resourceConnector.getDatabaseServerStatus(eq(authenticatedContext), databaseStackCaptor.capture()))
                .thenReturn(ExternalDatabaseStatus.DELETED)
                .thenReturn(ExternalDatabaseStatus.DELETED);
        when(dbStack.getCloudPlatform()).thenReturn(AZURE.name());
        when(dbStack.getId()).thenReturn(DB_STACK_ID);
        when(dbStack.getStatus()).thenReturn(Status.AVAILABLE);
        when(dbStack.getOwnerCrn()).thenReturn(crn);
        when(dbStack.getName()).thenReturn(DB_NAME);
        when(databaseServer.getParameters()).thenReturn(Map.of(AZURE_DATABASE_TYPE_KEY, AzureDatabaseType.SINGLE_SERVER.name()));
        when(databaseStack.getDatabaseServer()).thenReturn(databaseServer);
        when(dbStack.getDatabaseServer()).thenReturn(dbServer);
        when(dbServer.getName()).thenReturn(DBSVR_NAME);
        DBResource dbResource = new DBResource.Builder().withReference(SINGLE_SERVER_RES_ID).build();
        when(dbResourceService.findByStackAndNameAndType(eq(DB_STACK_ID), eq(DBSVR_NAME), eq(ResourceType.AZURE_DATABASE))).thenReturn(Optional.of(dbResource));
        victim.sync(dbStack);

        assertEquals(FLEXIBLE_SERVER.name(), databaseStackCaptor.getAllValues().get(1).getDatabaseServer().getParameters().get(AZURE_DATABASE_TYPE_KEY));
        verifyNoInteractions(dbStackService);
        verifyNoMoreInteractions(dbResourceService);
        verify(dbStackStatusUpdater).updateStatus(DB_STACK_ID, DetailedDBStackStatus.DELETE_COMPLETED);
        verify(dbStackJobService).unschedule(DB_STACK_ID, DB_NAME);
    }

    @Test
    public void shouldSetStatusAndUnscheduleInCaseOfStopCompleted()
            throws Exception {
        when(resourceConnector.getDatabaseServerStatus(authenticatedContext, databaseStack)).thenReturn(ExternalDatabaseStatus.DELETED);
        when(dbStack.getId()).thenReturn(DB_STACK_ID);
        when(dbStack.getStatus()).thenReturn(Status.DELETE_IN_PROGRESS);
        when(dbStack.getOwnerCrn()).thenReturn(crn);
        when(dbStack.getName()).thenReturn(DB_NAME);

        victim.sync(dbStack);

        verify(dbStackStatusUpdater).updateStatus(DB_STACK_ID, DetailedDBStackStatus.DELETE_COMPLETED);
        verify(dbStackJobService).unschedule(DB_STACK_ID, DB_NAME);
    }
}
