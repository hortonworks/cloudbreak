package com.sequenceiq.cloudbreak.cloud.gcp.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.api.services.sqladmin.SQLAdmin;
import com.google.api.services.sqladmin.model.Operation;
import com.google.api.services.sqladmin.model.OperationError;
import com.google.api.services.sqladmin.model.OperationErrors;
import com.google.api.services.sqladmin.model.User;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpSQLAdminFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;

@ExtendWith(MockitoExtension.class)
class GcpDatabaseServerUpdateServiceTest {

    private static final String PROJECT_ID = "projectId";

    private static final String SERVER_ID = "serverId";

    private static final String ROOT_USER_NAME = "rootUserName";

    private static final String NEW_PASSWORD = "newPassword";

    @Mock
    private GcpSQLAdminFactory gcpSQLAdminFactory;

    @Mock
    private GcpStackUtil gcpStackUtil;

    @Mock
    private AuthenticatedContext ac;

    @InjectMocks
    private GcpDatabaseServerUpdateService underTest;

    @BeforeEach
    void setUp() {
        when(ac.getCloudCredential()).thenReturn(new CloudCredential("id", "name", "account"));
    }

    @Test
    void updateRootUserPasswordShouldThrowCloudConnectorExceptionWhenUserNotFound() throws IOException {
        DatabaseStack databaseStack = new DatabaseStack(null,
                DatabaseServer.builder().withServerId(SERVER_ID).withRootUserName(ROOT_USER_NAME).build(), new HashMap<>(), "null");
        SQLAdmin sqlAdmin = mock(SQLAdmin.class);
        when(gcpSQLAdminFactory.buildSQLAdmin(any(), anyString())).thenReturn(sqlAdmin);
        when(gcpStackUtil.getProjectId(any())).thenReturn(PROJECT_ID);

        SQLAdmin.Users users = mock(SQLAdmin.Users.class);
        when(sqlAdmin.users()).thenReturn(users);
        SQLAdmin.Users.Get get = mock(SQLAdmin.Users.Get.class);
        when(users.get(eq(PROJECT_ID), eq(SERVER_ID), eq(ROOT_USER_NAME))).thenReturn(get);
        User user = getUser();
        when(get.execute()).thenThrow(new RuntimeException("missing"));

        CloudConnectorException cloudConnectorException = assertThrows(CloudConnectorException.class,
                () -> underTest.updateRootUserPassword(ac, databaseStack, NEW_PASSWORD));

        assertEquals("missing", cloudConnectorException.getMessage());
        verify(gcpSQLAdminFactory, times(1)).buildSQLAdmin(any(), anyString());
        verify(gcpStackUtil, times(1)).getProjectId(any());
        verify(sqlAdmin, times(1)).users();
        verify(users, times(1)).get(eq(PROJECT_ID), eq(SERVER_ID), eq(ROOT_USER_NAME));
        verify(get, times(1)).execute();
    }

    @Test
    void updateRootUserPasswordShouldThrowCloudConnectorExceptionWhenOperationFailed() throws IOException {
        DatabaseStack databaseStack = new DatabaseStack(null,
                DatabaseServer.builder().withServerId(SERVER_ID).withRootUserName(ROOT_USER_NAME).build(), new HashMap<>(), "null");
        SQLAdmin sqlAdmin = mock(SQLAdmin.class);
        when(gcpSQLAdminFactory.buildSQLAdmin(any(), anyString())).thenReturn(sqlAdmin);
        when(gcpStackUtil.getProjectId(any())).thenReturn(PROJECT_ID);

        SQLAdmin.Users users = mock(SQLAdmin.Users.class);
        when(sqlAdmin.users()).thenReturn(users);
        SQLAdmin.Users.Get get = mock(SQLAdmin.Users.Get.class);
        when(users.get(eq(PROJECT_ID), eq(SERVER_ID), eq(ROOT_USER_NAME))).thenReturn(get);
        User user = getUser();
        when(get.execute()).thenReturn(user);
        SQLAdmin.Users.Update update = mock(SQLAdmin.Users.Update.class);
        when(users.update(eq(PROJECT_ID), eq(SERVER_ID), eq(user))).thenReturn(update);
        when(update.setName(anyString())).thenReturn(update);
        Operation operation = mock(Operation.class);
        when(update.execute()).thenReturn(operation);
        when(operation.getError()).thenReturn(getOperationErrors());

        CloudConnectorException cloudConnectorException = assertThrows(CloudConnectorException.class,
                () -> underTest.updateRootUserPassword(ac, databaseStack, NEW_PASSWORD));

        assertEquals("Failed to execute database operation: error,: [ resourceType: GCP_DATABASE,  resourceName: root user password ]",
                cloudConnectorException.getMessage());
        verify(gcpSQLAdminFactory, times(1)).buildSQLAdmin(any(), anyString());
        verify(gcpStackUtil, times(1)).getProjectId(any());
        verify(sqlAdmin, times(2)).users();
        verify(users, times(1)).get(eq(PROJECT_ID), eq(SERVER_ID), eq(ROOT_USER_NAME));
        verify(get, times(1)).execute();
        verify(users, times(1)).update(eq(PROJECT_ID), eq(SERVER_ID), eq(user));
        verify(update, times(1)).execute();
    }

    @Test
    void updateRootUserShouldSucceed() throws IOException {
        DatabaseStack databaseStack = new DatabaseStack(null,
                DatabaseServer.builder().withServerId(SERVER_ID).withRootUserName(ROOT_USER_NAME).build(), new HashMap<>(), "null");
        SQLAdmin sqlAdmin = mock(SQLAdmin.class);
        when(gcpSQLAdminFactory.buildSQLAdmin(any(), anyString())).thenReturn(sqlAdmin);
        when(gcpStackUtil.getProjectId(any())).thenReturn(PROJECT_ID);

        SQLAdmin.Users users = mock(SQLAdmin.Users.class);
        when(sqlAdmin.users()).thenReturn(users);
        SQLAdmin.Users.Get get = mock(SQLAdmin.Users.Get.class);
        when(users.get(eq(PROJECT_ID), eq(SERVER_ID), eq(ROOT_USER_NAME))).thenReturn(get);
        User user = getUser();
        when(get.execute()).thenReturn(user);
        SQLAdmin.Users.Update update = mock(SQLAdmin.Users.Update.class);
        when(users.update(eq(PROJECT_ID), eq(SERVER_ID), eq(user))).thenReturn(update);
        when(update.setName(anyString())).thenReturn(update);
        Operation operation = mock(Operation.class);
        when(update.execute()).thenReturn(operation);

        underTest.updateRootUserPassword(ac, databaseStack, NEW_PASSWORD);

        verify(gcpSQLAdminFactory, times(1)).buildSQLAdmin(any(), anyString());
        verify(gcpStackUtil, times(1)).getProjectId(any());
        verify(sqlAdmin, times(2)).users();
        verify(users, times(1)).get(eq(PROJECT_ID), eq(SERVER_ID), eq(ROOT_USER_NAME));
        verify(get, times(1)).execute();
        verify(users, times(1)).update(eq(PROJECT_ID), eq(SERVER_ID), eq(user));
        verify(update, times(1)).execute();
    }

    private static OperationErrors getOperationErrors() {
        OperationErrors operationErrors = new OperationErrors();
        OperationError operationError = new OperationError();
        operationError.setMessage("error");
        operationErrors.setErrors(List.of(operationError));
        return operationErrors;
    }

    private User getUser() {
        User user = new User();
        user.setName(ROOT_USER_NAME);
        return user;
    }
}