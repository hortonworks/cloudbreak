package com.sequenceiq.cloudbreak.cloud.gcp.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.services.sqladmin.SQLAdmin;
import com.google.api.services.sqladmin.model.DatabaseInstance;
import com.google.api.services.sqladmin.model.InstancesListResponse;
import com.google.api.services.sqladmin.model.Settings;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpSQLAdminFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus;

@ExtendWith(MockitoExtension.class)
public class GcpDatabaseServerCheckServiceTest {

    @Mock
    private GcpSQLAdminFactory gcpSQLAdminFactory;

    @Mock
    private GcpStackUtil gcpStackUtil;

    @InjectMocks
    private GcpDatabaseServerCheckService underTest;

    @Test
    public void testCheckWhenDbInstanceNotAvailableShouldReturnDeleted() throws IOException {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        DatabaseStack databaseStack = mock(DatabaseStack.class);
        DatabaseServer databaseServer = mock(DatabaseServer.class);
        SQLAdmin sqlAdmin = mock(SQLAdmin.class);
        SQLAdmin.Instances sqlAdminInstances = mock(SQLAdmin.Instances.class);
        SQLAdmin.Instances.List sqlAdminInstancesList = mock(SQLAdmin.Instances.List.class);
        InstancesListResponse instancesListResponse = mock(InstancesListResponse.class);

        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(cloudCredential.getName()).thenReturn("credential");
        when(databaseStack.getDatabaseServer()).thenReturn(databaseServer);
        when(databaseServer.getServerId()).thenReturn("test");
        when(gcpSQLAdminFactory.buildSQLAdmin(any(CloudCredential.class), anyString())).thenReturn(sqlAdmin);
        when(gcpStackUtil.getProjectId(any(CloudCredential.class))).thenReturn("project-id");
        when(sqlAdmin.instances()).thenReturn(sqlAdminInstances);
        when(sqlAdminInstances.list(anyString())).thenReturn(sqlAdminInstancesList);
        when(sqlAdminInstancesList.execute()).thenReturn(instancesListResponse);
        when(instancesListResponse.isEmpty()).thenReturn(true);

        ExternalDatabaseStatus check = underTest.check(authenticatedContext, databaseStack);

        assertEquals(ExternalDatabaseStatus.DELETED, check);
    }

    @Test
    public void testCheckWhenDbInstanceIsFailedShouldReturnDeleted() throws IOException {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        DatabaseStack databaseStack = mock(DatabaseStack.class);
        DatabaseServer databaseServer = mock(DatabaseServer.class);
        SQLAdmin sqlAdmin = mock(SQLAdmin.class);
        SQLAdmin.Instances sqlAdminInstances = mock(SQLAdmin.Instances.class);
        SQLAdmin.Instances.List sqlAdminInstancesList = mock(SQLAdmin.Instances.List.class);
        InstancesListResponse instancesListResponse = mock(InstancesListResponse.class);

        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(cloudCredential.getName()).thenReturn("credential");
        when(databaseStack.getDatabaseServer()).thenReturn(databaseServer);
        when(databaseServer.getServerId()).thenReturn("test");
        when(gcpSQLAdminFactory.buildSQLAdmin(any(CloudCredential.class), anyString())).thenReturn(sqlAdmin);
        when(gcpStackUtil.getProjectId(any(CloudCredential.class))).thenReturn("project-id");
        when(sqlAdmin.instances()).thenReturn(sqlAdminInstances);
        when(sqlAdminInstances.list(anyString())).thenReturn(sqlAdminInstancesList);
        when(sqlAdminInstancesList.execute()).thenReturn(instancesListResponse);
        when(instancesListResponse.isEmpty()).thenReturn(false);
        DatabaseInstance databaseInstance = new DatabaseInstance();
        databaseInstance.setName("test");
        databaseInstance.setState("FAILED");
        when(instancesListResponse.getItems()).thenReturn(List.of(databaseInstance));

        ExternalDatabaseStatus check = underTest.check(authenticatedContext, databaseStack);

        assertEquals(ExternalDatabaseStatus.DELETED, check);
    }

    @Test
    public void testCheckWhenDbInstanceIsUnknownStateShouldReturnDeleted() throws IOException {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        DatabaseStack databaseStack = mock(DatabaseStack.class);
        DatabaseServer databaseServer = mock(DatabaseServer.class);
        SQLAdmin sqlAdmin = mock(SQLAdmin.class);
        SQLAdmin.Instances sqlAdminInstances = mock(SQLAdmin.Instances.class);
        SQLAdmin.Instances.List sqlAdminInstancesList = mock(SQLAdmin.Instances.List.class);
        InstancesListResponse instancesListResponse = mock(InstancesListResponse.class);

        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(cloudCredential.getName()).thenReturn("credential");
        when(databaseStack.getDatabaseServer()).thenReturn(databaseServer);
        when(databaseServer.getServerId()).thenReturn("test");
        when(gcpSQLAdminFactory.buildSQLAdmin(any(CloudCredential.class), anyString())).thenReturn(sqlAdmin);
        when(gcpStackUtil.getProjectId(any(CloudCredential.class))).thenReturn("project-id");
        when(sqlAdmin.instances()).thenReturn(sqlAdminInstances);
        when(sqlAdminInstances.list(anyString())).thenReturn(sqlAdminInstancesList);
        when(sqlAdminInstancesList.execute()).thenReturn(instancesListResponse);
        when(instancesListResponse.isEmpty()).thenReturn(false);
        DatabaseInstance databaseInstance = new DatabaseInstance();
        databaseInstance.setName("test");
        databaseInstance.setState("UNKNOWN_STATE");
        when(instancesListResponse.getItems()).thenReturn(List.of(databaseInstance));

        ExternalDatabaseStatus check = underTest.check(authenticatedContext, databaseStack);

        assertEquals(ExternalDatabaseStatus.DELETED, check);
    }

    @Test
    public void testCheckWhenDbInstanceIsTESTShouldReturnDeleted() throws IOException {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        DatabaseStack databaseStack = mock(DatabaseStack.class);
        DatabaseServer databaseServer = mock(DatabaseServer.class);
        SQLAdmin sqlAdmin = mock(SQLAdmin.class);
        SQLAdmin.Instances sqlAdminInstances = mock(SQLAdmin.Instances.class);
        SQLAdmin.Instances.List sqlAdminInstancesList = mock(SQLAdmin.Instances.List.class);
        InstancesListResponse instancesListResponse = mock(InstancesListResponse.class);

        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(cloudCredential.getName()).thenReturn("credential");
        when(databaseStack.getDatabaseServer()).thenReturn(databaseServer);
        when(databaseServer.getServerId()).thenReturn("test");
        when(gcpSQLAdminFactory.buildSQLAdmin(any(CloudCredential.class), anyString())).thenReturn(sqlAdmin);
        when(gcpStackUtil.getProjectId(any(CloudCredential.class))).thenReturn("project-id");
        when(sqlAdmin.instances()).thenReturn(sqlAdminInstances);
        when(sqlAdminInstances.list(anyString())).thenReturn(sqlAdminInstancesList);
        when(sqlAdminInstancesList.execute()).thenReturn(instancesListResponse);
        when(instancesListResponse.isEmpty()).thenReturn(false);
        DatabaseInstance databaseInstance = new DatabaseInstance();
        databaseInstance.setName("test");
        databaseInstance.setState("TEST");
        when(instancesListResponse.getItems()).thenReturn(List.of(databaseInstance));

        ExternalDatabaseStatus check = underTest.check(authenticatedContext, databaseStack);

        assertEquals(ExternalDatabaseStatus.UPDATE_IN_PROGRESS, check);
    }

    @Test
    public void testCheckWhenDbInstanceIsSuspendedShouldReturnStopped() throws IOException {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        DatabaseStack databaseStack = mock(DatabaseStack.class);
        DatabaseServer databaseServer = mock(DatabaseServer.class);
        SQLAdmin sqlAdmin = mock(SQLAdmin.class);
        SQLAdmin.Instances sqlAdminInstances = mock(SQLAdmin.Instances.class);
        SQLAdmin.Instances.List sqlAdminInstancesList = mock(SQLAdmin.Instances.List.class);
        InstancesListResponse instancesListResponse = mock(InstancesListResponse.class);

        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(cloudCredential.getName()).thenReturn("credential");
        when(databaseStack.getDatabaseServer()).thenReturn(databaseServer);
        when(databaseServer.getServerId()).thenReturn("test");
        when(gcpSQLAdminFactory.buildSQLAdmin(any(CloudCredential.class), anyString())).thenReturn(sqlAdmin);
        when(gcpStackUtil.getProjectId(any(CloudCredential.class))).thenReturn("project-id");
        when(sqlAdmin.instances()).thenReturn(sqlAdminInstances);
        when(sqlAdminInstances.list(anyString())).thenReturn(sqlAdminInstancesList);
        when(sqlAdminInstancesList.execute()).thenReturn(instancesListResponse);
        when(instancesListResponse.isEmpty()).thenReturn(false);
        DatabaseInstance databaseInstance = new DatabaseInstance();
        databaseInstance.setName("test");
        databaseInstance.setState("SUSPENDED");
        when(instancesListResponse.getItems()).thenReturn(List.of(databaseInstance));

        ExternalDatabaseStatus check = underTest.check(authenticatedContext, databaseStack);

        assertEquals(ExternalDatabaseStatus.STOPPED, check);
    }

    @Test
    public void testCheckWhenDbInstanceIsRunnableAlwaysShouldReturnStarted() throws IOException {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        DatabaseStack databaseStack = mock(DatabaseStack.class);
        DatabaseServer databaseServer = mock(DatabaseServer.class);
        SQLAdmin sqlAdmin = mock(SQLAdmin.class);
        SQLAdmin.Instances sqlAdminInstances = mock(SQLAdmin.Instances.class);
        SQLAdmin.Instances.List sqlAdminInstancesList = mock(SQLAdmin.Instances.List.class);
        InstancesListResponse instancesListResponse = mock(InstancesListResponse.class);

        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(cloudCredential.getName()).thenReturn("credential");
        when(databaseStack.getDatabaseServer()).thenReturn(databaseServer);
        when(databaseServer.getServerId()).thenReturn("test");
        when(gcpSQLAdminFactory.buildSQLAdmin(any(CloudCredential.class), anyString())).thenReturn(sqlAdmin);
        when(gcpStackUtil.getProjectId(any(CloudCredential.class))).thenReturn("project-id");
        when(sqlAdmin.instances()).thenReturn(sqlAdminInstances);
        when(sqlAdminInstances.list(anyString())).thenReturn(sqlAdminInstancesList);
        when(sqlAdminInstancesList.execute()).thenReturn(instancesListResponse);
        when(instancesListResponse.isEmpty()).thenReturn(false);
        DatabaseInstance databaseInstance = new DatabaseInstance();
        databaseInstance.setName("test");
        databaseInstance.setState("RUNNABLE");
        Settings settings = new Settings();
        settings.setActivationPolicy("ALWAYS");
        databaseInstance.setSettings(settings);
        when(instancesListResponse.getItems()).thenReturn(List.of(databaseInstance));

        ExternalDatabaseStatus check = underTest.check(authenticatedContext, databaseStack);

        assertEquals(ExternalDatabaseStatus.STARTED, check);
    }

    @Test
    public void testCheckWhenDbInstanceIsRunnableNOTAlwaysShouldReturnStarted() throws IOException {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        DatabaseStack databaseStack = mock(DatabaseStack.class);
        DatabaseServer databaseServer = mock(DatabaseServer.class);
        SQLAdmin sqlAdmin = mock(SQLAdmin.class);
        SQLAdmin.Instances sqlAdminInstances = mock(SQLAdmin.Instances.class);
        SQLAdmin.Instances.List sqlAdminInstancesList = mock(SQLAdmin.Instances.List.class);
        InstancesListResponse instancesListResponse = mock(InstancesListResponse.class);

        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(cloudCredential.getName()).thenReturn("credential");
        when(databaseStack.getDatabaseServer()).thenReturn(databaseServer);
        when(databaseServer.getServerId()).thenReturn("test");
        when(gcpSQLAdminFactory.buildSQLAdmin(any(CloudCredential.class), anyString())).thenReturn(sqlAdmin);
        when(gcpStackUtil.getProjectId(any(CloudCredential.class))).thenReturn("project-id");
        when(sqlAdmin.instances()).thenReturn(sqlAdminInstances);
        when(sqlAdminInstances.list(anyString())).thenReturn(sqlAdminInstancesList);
        when(sqlAdminInstancesList.execute()).thenReturn(instancesListResponse);
        when(instancesListResponse.isEmpty()).thenReturn(false);
        DatabaseInstance databaseInstance = new DatabaseInstance();
        databaseInstance.setName("test");
        databaseInstance.setState("RUNNABLE");
        Settings settings = new Settings();
        settings.setActivationPolicy("NOT_ALWAYS");
        databaseInstance.setSettings(settings);
        when(instancesListResponse.getItems()).thenReturn(List.of(databaseInstance));

        ExternalDatabaseStatus check = underTest.check(authenticatedContext, databaseStack);

        assertEquals(ExternalDatabaseStatus.STOPPED, check);
    }

    @Test
    public void testCheckWhenDbInstanceDropTokenExceptionShouldReturnGcpResourceException() throws IOException {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        DatabaseStack databaseStack = mock(DatabaseStack.class);
        DatabaseServer databaseServer = mock(DatabaseServer.class);

        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(cloudCredential.getName()).thenReturn("credential");
        when(databaseStack.getDatabaseServer()).thenReturn(databaseServer);
        when(databaseServer.getServerId()).thenReturn("test");
        when(gcpSQLAdminFactory.buildSQLAdmin(any(CloudCredential.class), anyString()))
                .thenThrow(new GcpResourceException("error"));

        GcpResourceException gcpResourceException = assertThrows(GcpResourceException.class,
                () -> underTest.check(authenticatedContext, databaseStack));

        assertEquals("error", gcpResourceException.getMessage());
    }

    @Test
    public void testCheckWhenDbInstanceDropGcpResourceExceptionShouldReturnGcpResourceException() throws IOException {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        DatabaseStack databaseStack = mock(DatabaseStack.class);
        DatabaseServer databaseServer = mock(DatabaseServer.class);
        SQLAdmin sqlAdmin = mock(SQLAdmin.class);
        SQLAdmin.Instances sqlAdminInstances = mock(SQLAdmin.Instances.class);
        SQLAdmin.Instances.List sqlAdminInstancesList = mock(SQLAdmin.Instances.List.class);
        TokenResponseException tokenResponseException = mock(TokenResponseException.class);

        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(cloudCredential.getName()).thenReturn("credential");
        when(databaseStack.getDatabaseServer()).thenReturn(databaseServer);
        when(databaseServer.getServerId()).thenReturn("test");
        when(gcpSQLAdminFactory.buildSQLAdmin(any(CloudCredential.class), anyString())).thenReturn(sqlAdmin);
        when(gcpStackUtil.getProjectId(any(CloudCredential.class))).thenReturn("project-id");
        when(sqlAdmin.instances()).thenReturn(sqlAdminInstances);
        when(sqlAdminInstances.list(anyString())).thenReturn(sqlAdminInstancesList);
        when(sqlAdminInstancesList.execute()).thenThrow(tokenResponseException);
        when(gcpStackUtil.getMissingServiceAccountKeyError(any(TokenResponseException.class), anyString()))
                .thenReturn(new GcpResourceException("error"));

        GcpResourceException gcpResourceException = assertThrows(GcpResourceException.class,
                () -> underTest.check(authenticatedContext, databaseStack));

        assertEquals("error", gcpResourceException.getMessage());
    }
}