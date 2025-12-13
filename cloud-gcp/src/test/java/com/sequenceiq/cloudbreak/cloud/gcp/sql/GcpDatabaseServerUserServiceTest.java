package com.sequenceiq.cloudbreak.cloud.gcp.sql;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.api.services.sqladmin.SQLAdmin;
import com.google.api.services.sqladmin.model.Operation;
import com.google.api.services.sqladmin.model.User;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpSQLAdminFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.poller.DatabasePollerService;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class GcpDatabaseServerUserServiceTest {

    @InjectMocks
    private GcpDatabaseServerUserService gcpDatabaseServerUserService;

    @Mock
    private GcpStackUtil gcpStackUtil;

    @Mock
    private GcpSQLAdminFactory gcpSQLAdminFactory;

    @Mock
    private DatabasePollerService databasePollerService;

    @Mock
    private SQLAdmin sqlAdmin;

    @Mock
    private SQLAdmin.Users sqlAdminUsers;

    @Mock
    private SQLAdmin.Users.Insert sqlAdminUsersInsert;

    @BeforeEach
    void setUp() throws IOException {
        when(gcpSQLAdminFactory.buildSQLAdmin(any(), anyString())).thenReturn(sqlAdmin);
        when(sqlAdmin.users()).thenReturn(sqlAdminUsers);
        when(sqlAdminUsers.insert(anyString(), anyString(), any(User.class))).thenReturn(sqlAdminUsersInsert);
    }

    @Test
    void testCreateUser() throws IOException {
        AuthenticatedContext ac = mock(AuthenticatedContext.class);
        DatabaseStack stack = mock(DatabaseStack.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        DatabaseServer databaseServer = mock(DatabaseServer.class);
        CloudResource cloudResource = CloudResource.builder()
                .withAvailabilityZone("az")
                .withGroup("gr")
                .withInstanceId("1")
                .withName("name")
                .withStackAware(true)
                .withStatus(CommonStatus.CREATED)
                .withReference("ref")
                .withParameters(Map.of())
                .withType(ResourceType.GCP_DATABASE)
                .build();

        Operation operation = new Operation();

        when(ac.getCloudCredential()).thenReturn(cloudCredential);
        when(cloudCredential.getName()).thenReturn("credentialName");
        when(gcpStackUtil.getProjectId(cloudCredential)).thenReturn("projectId");
        when(stack.getDatabaseServer()).thenReturn(databaseServer);
        when(databaseServer.getRootUserName()).thenReturn("root");
        when(databaseServer.getRootPassword()).thenReturn("password");
        when(sqlAdminUsersInsert.execute()).thenReturn(operation);

        gcpDatabaseServerUserService.createUser(ac, stack, List.of(cloudResource), "instanceName");

        verify(gcpStackUtil).getProjectId(cloudCredential);
        verify(gcpSQLAdminFactory).buildSQLAdmin(cloudCredential, "credentialName");
        verify(sqlAdminUsers).insert("projectId", "instanceName", new User()
                .setProject("projectId")
                .setInstance("instanceName")
                .setName("root")
                .setPassword("password"));
        verify(databasePollerService).insertUserPoller(eq(ac), anyList());
    }

    @Test
    void testCreateUserThrowsException() throws IOException {
        AuthenticatedContext ac = mock(AuthenticatedContext.class);
        DatabaseStack stack = mock(DatabaseStack.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        DatabaseServer databaseServer = mock(DatabaseServer.class);
        CloudResource cloudResource = mock(CloudResource.class);

        when(ac.getCloudCredential()).thenReturn(cloudCredential);
        when(cloudCredential.getName()).thenReturn("credentialName");
        when(gcpStackUtil.getProjectId(cloudCredential)).thenReturn("projectId");
        when(stack.getDatabaseServer()).thenReturn(databaseServer);
        when(databaseServer.getRootUserName()).thenReturn("root");
        when(databaseServer.getRootPassword()).thenReturn("password");
        when(sqlAdminUsersInsert.execute()).thenThrow(new IOException("IO Exception"));

        assertThrows(CloudConnectorException.class, () -> {
            gcpDatabaseServerUserService.createUser(ac, stack, List.of(cloudResource), "instanceName");
        });

        verify(gcpStackUtil).getProjectId(cloudCredential);
        verify(gcpSQLAdminFactory).buildSQLAdmin(cloudCredential, "credentialName");
        verify(sqlAdminUsers).insert("projectId", "instanceName", new User()
                .setProject("projectId")
                .setInstance("instanceName")
                .setName("root")
                .setPassword("password"));
    }

}