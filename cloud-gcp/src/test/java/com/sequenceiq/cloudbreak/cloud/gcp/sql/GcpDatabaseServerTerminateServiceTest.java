package com.sequenceiq.cloudbreak.cloud.gcp.sql;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.sqladmin.SQLAdmin;
import com.google.api.services.sqladmin.model.DatabaseInstance;
import com.google.api.services.sqladmin.model.InstancesListResponse;
import com.google.api.services.sqladmin.model.Operation;
import com.google.api.services.sqladmin.model.Settings;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpSQLAdminFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.poller.DatabasePollerService;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;

@ExtendWith(MockitoExtension.class)
public class GcpDatabaseServerTerminateServiceTest {

    @Mock
    private DatabasePollerService databasePollerService;

    @Mock
    private GcpSQLAdminFactory gcpSQLAdminFactory;

    @Mock
    private GcpStackUtil gcpStackUtil;

    @Mock
    private CloudContext cloudContext;

    @InjectMocks
    private GcpDatabaseServerTerminateService underTest;

    @Test
    public void testTerminateWhenDatabaseIsPresentedShouldDeleteDatabase() throws Exception {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        DatabaseStack databaseStack = mock(DatabaseStack.class);
        DatabaseServer databaseServer = mock(DatabaseServer.class);
        PersistenceNotifier persistenceNotifier = mock(PersistenceNotifier.class);
        SQLAdmin sqlAdmin = mock(SQLAdmin.class);
        SQLAdmin.Instances sqlAdminInstances = mock(SQLAdmin.Instances.class);
        SQLAdmin.Instances.List sqlAdminInstancesList = mock(SQLAdmin.Instances.List.class);
        InstancesListResponse instancesListResponse = mock(InstancesListResponse.class);

        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(cloudCredential.getName()).thenReturn("credential");
        when(databaseStack.getDatabaseServer()).thenReturn(databaseServer);
        when(databaseServer.getServerId()).thenReturn("server-1");
        when(gcpSQLAdminFactory.buildSQLAdmin(any(CloudCredential.class), anyString())).thenReturn(sqlAdmin);
        when(gcpStackUtil.getProjectId(any(CloudCredential.class))).thenReturn("project-id");
        when(sqlAdmin.instances()).thenReturn(sqlAdminInstances);
        when(sqlAdminInstances.list(anyString())).thenReturn(sqlAdminInstancesList);
        when(sqlAdminInstancesList.execute()).thenReturn(instancesListResponse);

        DatabaseInstance databaseInstance = new DatabaseInstance();
        databaseInstance.setName("server-1");
        databaseInstance.setState("RUNNABLE");
        Settings settings = new Settings();
        settings.setActivationPolicy("ALWAYS");
        databaseInstance.setSettings(settings);
        when(instancesListResponse.getItems()).thenReturn(List.of(databaseInstance));

        SQLAdmin.Instances.Delete delete = mock(SQLAdmin.Instances.Delete.class);
        Operation operation = mock(Operation.class);

        when(sqlAdminInstances.delete(anyString(), anyString())).thenReturn(delete);
        when(delete.execute()).thenReturn(operation);
        when(delete.setPrettyPrint(anyBoolean())).thenReturn(delete);
        when(operation.getError()).thenReturn(null);
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(location(region("region"), availabilityZone("az1")));

        doNothing().when(databasePollerService).terminateDatabasePoller(any(AuthenticatedContext.class), anyList());

        List<CloudResource> terminate = underTest.terminate(authenticatedContext, databaseStack, persistenceNotifier);
        assertEquals(1, terminate.size());
        assertEquals("server-1", terminate.get(0).getName());
    }

    @Test
    public void testTerminateWhenDatabaseIsPresentedAndDeleteThrowExceptionShouldDeleteDatabaseFails() throws Exception {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        DatabaseStack databaseStack = mock(DatabaseStack.class);
        DatabaseServer databaseServer = mock(DatabaseServer.class);
        PersistenceNotifier persistenceNotifier = mock(PersistenceNotifier.class);
        SQLAdmin sqlAdmin = mock(SQLAdmin.class);
        SQLAdmin.Instances sqlAdminInstances = mock(SQLAdmin.Instances.class);
        SQLAdmin.Instances.List sqlAdminInstancesList = mock(SQLAdmin.Instances.List.class);
        InstancesListResponse instancesListResponse = mock(InstancesListResponse.class);

        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(cloudCredential.getName()).thenReturn("credential");
        when(databaseStack.getDatabaseServer()).thenReturn(databaseServer);
        when(databaseServer.getServerId()).thenReturn("server-1");
        when(gcpSQLAdminFactory.buildSQLAdmin(any(CloudCredential.class), anyString())).thenReturn(sqlAdmin);
        when(gcpStackUtil.getProjectId(any(CloudCredential.class))).thenReturn("project-id");
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(location(region("region"), availabilityZone("az1")));
        when(sqlAdmin.instances()).thenReturn(sqlAdminInstances);
        when(sqlAdminInstances.list(anyString())).thenReturn(sqlAdminInstancesList);
        when(sqlAdminInstancesList.execute()).thenReturn(instancesListResponse);

        DatabaseInstance databaseInstance = new DatabaseInstance();
        databaseInstance.setName("server-1");
        databaseInstance.setState("RUNNABLE");
        Settings settings = new Settings();
        settings.setActivationPolicy("ALWAYS");
        databaseInstance.setSettings(settings);
        when(instancesListResponse.getItems()).thenReturn(List.of(databaseInstance));

        SQLAdmin.Instances.Delete delete = mock(SQLAdmin.Instances.Delete.class);
        GoogleJsonResponseException googleJsonResponseException = mock(GoogleJsonResponseException.class);

        when(sqlAdminInstances.delete(anyString(), anyString())).thenReturn(delete);
        when(delete.execute()).thenThrow(googleJsonResponseException);
        when(delete.setPrettyPrint(anyBoolean())).thenReturn(delete);
        GoogleJsonError googleJsonError = mock(GoogleJsonError.class);
        when(googleJsonResponseException.getDetails()).thenReturn(googleJsonError);
        when(googleJsonError.getMessage()).thenReturn("error");

        GcpResourceException gcpResourceException = assertThrows(GcpResourceException.class,
                () -> underTest.terminate(authenticatedContext, databaseStack, persistenceNotifier));
        assertEquals("error: [ resourceType: GCP_DATABASE,  resourceName: server-1 ]",
                gcpResourceException.getMessage());
    }

    @Test
    public void testTerminateWhenDatabaseIsPresentedAndListThrowExceptionShouldDeleteDatabaseFails() throws Exception {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        DatabaseStack databaseStack = mock(DatabaseStack.class);
        DatabaseServer databaseServer = mock(DatabaseServer.class);
        PersistenceNotifier persistenceNotifier = mock(PersistenceNotifier.class);
        SQLAdmin sqlAdmin = mock(SQLAdmin.class);
        SQLAdmin.Instances sqlAdminInstances = mock(SQLAdmin.Instances.class);
        SQLAdmin.Instances.List sqlAdminInstancesList = mock(SQLAdmin.Instances.List.class);
        GoogleJsonResponseException googleJsonResponseException = mock(GoogleJsonResponseException.class);

        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(cloudCredential.getName()).thenReturn("credential");
        when(databaseStack.getDatabaseServer()).thenReturn(databaseServer);
        when(databaseServer.getServerId()).thenReturn("server-1");
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(location(region("region"), availabilityZone("az1")));
        when(gcpSQLAdminFactory.buildSQLAdmin(any(CloudCredential.class), anyString())).thenReturn(sqlAdmin);
        when(gcpStackUtil.getProjectId(any(CloudCredential.class))).thenReturn("project-id");
        when(sqlAdmin.instances()).thenReturn(sqlAdminInstances);
        when(sqlAdminInstances.list(anyString())).thenReturn(sqlAdminInstancesList);
        GoogleJsonError googleJsonError = mock(GoogleJsonError.class);
        when(googleJsonResponseException.getDetails()).thenReturn(googleJsonError);
        when(googleJsonError.getMessage()).thenReturn("error");
        when(sqlAdminInstancesList.execute()).thenThrow(googleJsonResponseException);

        GcpResourceException gcpResourceException = assertThrows(GcpResourceException.class,
                () -> underTest.terminate(authenticatedContext, databaseStack, persistenceNotifier));
        assertEquals("error: [ resourceType: GCP_DATABASE,  resourceName: server-1 ]",
                gcpResourceException.getMessage());
    }
}