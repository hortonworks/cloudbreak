package com.sequenceiq.cloudbreak.cloud.gcp.sql;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;

@ExtendWith(MockitoExtension.class)
public class GcpDatabaseServerUpgradeServiceTest {

    @Mock
    private DatabasePollerService databasePollerService;

    @Mock
    private GcpSQLAdminFactory gcpSQLAdminFactory;

    @Mock
    private GcpStackUtil gcpStackUtil;

    @Mock
    private CloudContext cloudContext;

    @InjectMocks
    private GcpDatabaseServerUpgradeService underTest;

    @Test
    public void testUpgradeShouldUpgradeDatabase() throws Exception {
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
        databaseInstance.setDatabaseVersion("POSTGRES_10");
        Settings settings = new Settings();
        settings.setActivationPolicy("ALWAYS");
        databaseInstance.setSettings(settings);
        when(instancesListResponse.getItems()).thenReturn(List.of(databaseInstance));

        SQLAdmin.Instances.Patch patch = mock(SQLAdmin.Instances.Patch.class);
        Operation operation = mock(Operation.class);

        when(sqlAdminInstances.patch(anyString(), anyString(), any())).thenReturn(patch);
        when(patch.execute()).thenReturn(operation);
        when(patch.setPrettyPrint(anyBoolean())).thenReturn(patch);
        when(operation.getError()).thenReturn(null);
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(location(region("region"), availabilityZone("az1")));

        doNothing().when(databasePollerService).upgradeDatabasePoller(any(AuthenticatedContext.class), anyList());

        underTest.upgrade(authenticatedContext, databaseStack, persistenceNotifier, TargetMajorVersion.VERSION_11);

        ArgumentCaptor<CloudResource> notifyUpdateArgumentCaptor = ArgumentCaptor.forClass(CloudResource.class);
        verify(persistenceNotifier).notifyUpdate(notifyUpdateArgumentCaptor.capture(), any());
        CloudResource updatedResource = notifyUpdateArgumentCaptor.getValue();
        assertEquals("server-1", updatedResource.getName());
    }

    @Test
    public void testUpgradeWhenDatabaseInstanceNotRunningShouldThrowException() throws Exception {
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
        databaseInstance.setDatabaseVersion("POSTGRES_10");
        Settings settings = new Settings();
        settings.setActivationPolicy("NEVER");
        databaseInstance.setSettings(settings);
        when(instancesListResponse.getItems()).thenReturn(List.of(databaseInstance));

        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(location(region("region"), availabilityZone("az1")));

        GcpResourceException actualException = assertThrows(GcpResourceException.class,
                () -> underTest.upgrade(authenticatedContext, databaseStack, persistenceNotifier, TargetMajorVersion.VERSION_11));
        assertThat(actualException).hasMessageStartingWith("The database instance is not running");
    }

    @Test
    public void testUpgradeWhenDatabaseVersionIsGreaterThanTargetShouldThrowException() throws Exception {
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
        databaseInstance.setDatabaseVersion("POSTGRES_12");
        Settings settings = new Settings();
        settings.setActivationPolicy("ALWAYS");
        databaseInstance.setSettings(settings);
        when(instancesListResponse.getItems()).thenReturn(List.of(databaseInstance));

        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(location(region("region"), availabilityZone("az1")));

        GcpResourceException actualException = assertThrows(GcpResourceException.class,
                () -> underTest.upgrade(authenticatedContext, databaseStack, persistenceNotifier, TargetMajorVersion.VERSION_11));
        assertThat(actualException).hasMessageStartingWith("Database upgrade is not possible from");
    }

    @Test
    public void testUpgradeWhenDatabaseVersionIsEqualWithTargetShouldDoNothing() throws Exception {
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
        databaseInstance.setDatabaseVersion("POSTGRES_11");
        Settings settings = new Settings();
        settings.setActivationPolicy("ALWAYS");
        databaseInstance.setSettings(settings);
        when(instancesListResponse.getItems()).thenReturn(List.of(databaseInstance));

        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(location(region("region"), availabilityZone("az1")));

        underTest.upgrade(authenticatedContext, databaseStack, persistenceNotifier, TargetMajorVersion.VERSION_11);

        verify(sqlAdmin.instances(), never()).patch(anyString(), anyString(), any());
    }

    @Test
    public void testUpgradeWhenDatabaseEngineIsNotPostgresShouldThrowException() throws Exception {
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
        databaseInstance.setDatabaseVersion("MYSQL_10");
        Settings settings = new Settings();
        settings.setActivationPolicy("ALWAYS");
        databaseInstance.setSettings(settings);
        when(instancesListResponse.getItems()).thenReturn(List.of(databaseInstance));

        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(location(region("region"), availabilityZone("az1")));

        GcpResourceException actualException = assertThrows(GcpResourceException.class,
                () -> underTest.upgrade(authenticatedContext, databaseStack, persistenceNotifier, TargetMajorVersion.VERSION_11));
        assertThat(actualException).hasMessageStartingWith("Database upgrade is not possible for engine");
    }

    @Test
    public void testUpgradeWhenDatabaseVersionPatternInvalidShouldThrowException() throws Exception {
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
        databaseInstance.setDatabaseVersion("MYSQL_VERSION");
        Settings settings = new Settings();
        settings.setActivationPolicy("ALWAYS");
        databaseInstance.setSettings(settings);
        when(instancesListResponse.getItems()).thenReturn(List.of(databaseInstance));

        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(location(region("region"), availabilityZone("az1")));

        GcpResourceException actualException = assertThrows(GcpResourceException.class,
                () -> underTest.upgrade(authenticatedContext, databaseStack, persistenceNotifier, TargetMajorVersion.VERSION_11));
        assertThat(actualException).hasMessageStartingWith("Database upgrade is not possible for engine");
    }

    @Test
    public void testUpgradeWhenPatchThrowsExceptionShouldThrowGcpResourceException() throws Exception {
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
        databaseInstance.setDatabaseVersion("POSTGRES_10");
        Settings settings = new Settings();
        settings.setActivationPolicy("ALWAYS");
        databaseInstance.setSettings(settings);
        when(instancesListResponse.getItems()).thenReturn(List.of(databaseInstance));

        GoogleJsonResponseException googleJsonResponseException = mock(GoogleJsonResponseException.class);
        GoogleJsonError googleJsonError = mock(GoogleJsonError.class);
        when(googleJsonError.getMessage()).thenReturn("googleJsonError");
        when(googleJsonResponseException.getDetails()).thenReturn(googleJsonError);
        when(sqlAdminInstances.patch(anyString(), anyString(), any())).thenThrow(googleJsonResponseException);
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(location(region("region"), availabilityZone("az1")));

        GcpResourceException actualException = assertThrows(GcpResourceException.class,
                () -> underTest.upgrade(authenticatedContext, databaseStack, persistenceNotifier, TargetMajorVersion.VERSION_11));

        assertThat(actualException).hasMessageStartingWith("googleJsonError");
    }
}
