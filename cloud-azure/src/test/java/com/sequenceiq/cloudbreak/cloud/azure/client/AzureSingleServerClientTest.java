package com.sequenceiq.cloudbreak.cloud.azure.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.azure.core.http.HttpResponse;
import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.postgresql.PostgreSqlManager;
import com.azure.resourcemanager.postgresql.models.Server;
import com.azure.resourcemanager.postgresql.models.ServerState;
import com.azure.resourcemanager.postgresql.models.Servers;
import com.sequenceiq.cloudbreak.cloud.azure.util.AzureExceptionHandler;

@ExtendWith(MockitoExtension.class)
class AzureSingleServerClientTest {
    private static final String NEW_PASSWORD = "newPassword";

    private static final String RESOURCE_GROUP_NAME = "rg";

    private static final String SERVER_NAME = "serverName";

    @Spy
    private AzureExceptionHandler azureExceptionHandler;

    @Mock
    private PostgreSqlManager postgreSqlManager;

    @InjectMocks
    private AzureSingleServerClient underTest;

    @Test
    void testUpdateAdministratorLoginPassword() {
        Servers servers = mock(Servers.class);
        when(postgreSqlManager.servers()).thenReturn(servers);
        Server server = mock(Server.class);
        when(servers.getByResourceGroup(eq(RESOURCE_GROUP_NAME), eq(SERVER_NAME))).thenReturn(server);
        Server.Update update = mock(Server.Update.class);
        when(server.update()).thenReturn(update);
        when(update.withAdministratorLoginPassword(eq(NEW_PASSWORD))).thenReturn(update);
        when(update.apply()).thenReturn(server);
        underTest.updateAdministratorLoginPassword(RESOURCE_GROUP_NAME, SERVER_NAME, NEW_PASSWORD);
        verify(postgreSqlManager, times(1)).servers();
        verify(servers, times(1)).getByResourceGroup(eq(RESOURCE_GROUP_NAME), eq(SERVER_NAME));
        verify(server, times(1)).update();
        verify(update, times(1)).withAdministratorLoginPassword(eq(NEW_PASSWORD));
        verify(update, times(1)).apply();
    }

    @Test
    void testGetSingleServerStatus() {
        Servers servers = mock(Servers.class);
        when(postgreSqlManager.servers()).thenReturn(servers);
        Server server = mock(Server.class);
        when(server.userVisibleState()).thenReturn(ServerState.DISABLED);
        when(servers.getByResourceGroup(eq(RESOURCE_GROUP_NAME), eq(SERVER_NAME))).thenReturn(server);
        ServerState serverState = underTest.getSingleServerStatus(RESOURCE_GROUP_NAME, SERVER_NAME);
        assertEquals(ServerState.DISABLED, serverState);
    }

    @Test
    void testGetSingleServerStatusWhenServerIsNull() {
        Servers servers = mock(Servers.class);
        when(postgreSqlManager.servers()).thenReturn(servers);
        ServerState serverState = underTest.getSingleServerStatus(RESOURCE_GROUP_NAME, SERVER_NAME);
        assertEquals(AzureSingleServerClient.UNKNOWN, serverState);
    }

    @Test
    void testGetSingleServerStatusWhenNotFound() {
        Servers servers = mock(Servers.class);
        HttpResponse httpResponse = mock(HttpResponse.class);
        when(httpResponse.getStatusCode()).thenReturn(404);
        ManagementException managementException = new ManagementException("", httpResponse);
        when(servers.getByResourceGroup(eq(RESOURCE_GROUP_NAME), eq(SERVER_NAME))).thenThrow(managementException);
        when(postgreSqlManager.servers()).thenReturn(servers);
        ServerState serverState = underTest.getSingleServerStatus(RESOURCE_GROUP_NAME, SERVER_NAME);
        assertEquals(AzureSingleServerClient.UNKNOWN, serverState);
    }

    @Test
    void testGetSingleServerStatusWhenUnhandledException() {
        Servers servers = mock(Servers.class);
        HttpResponse httpResponse = mock(HttpResponse.class);
        ManagementException managementException = new ManagementException("", httpResponse);
        when(servers.getByResourceGroup(eq(RESOURCE_GROUP_NAME), eq(SERVER_NAME))).thenThrow(managementException);
        when(postgreSqlManager.servers()).thenReturn(servers);
        Assertions.assertThrows(ManagementException.class, () -> underTest.getSingleServerStatus(RESOURCE_GROUP_NAME, SERVER_NAME));
    }
}
