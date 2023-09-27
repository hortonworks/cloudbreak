package com.sequenceiq.cloudbreak.cloud.azure.client;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.azure.resourcemanager.postgresqlflexibleserver.PostgreSqlManager;
import com.azure.resourcemanager.postgresqlflexibleserver.models.Server;
import com.azure.resourcemanager.postgresqlflexibleserver.models.ServerState;
import com.azure.resourcemanager.postgresqlflexibleserver.models.Servers;
import com.sequenceiq.cloudbreak.cloud.azure.util.AzureExceptionHandler;

@ExtendWith(MockitoExtension.class)
class AzureFlexibleServerClientTest {
    private static final String RESOURCE_GROUP_NAME = "rg";

    private static final String SERVER_NAME = "serverName";

    @Mock
    private AzureExceptionHandler azureExceptionHandler;

    @Mock
    private PostgreSqlManager postgreSqlFlexibleManager;

    @InjectMocks
    private AzureFlexibleServerClient underTest;

    @BeforeEach
    void setUp() {
        lenient().when(azureExceptionHandler.handleException(any(Supplier.class))).thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get());
        lenient().doCallRealMethod().when(azureExceptionHandler).handleException(any(Runnable.class));
    }

    @Test
    void testStartFlexibleServer() {
        Servers servers = mock(Servers.class);
        when(postgreSqlFlexibleManager.servers()).thenReturn(servers);
        underTest.startFlexibleServer(RESOURCE_GROUP_NAME, SERVER_NAME);
        verify(postgreSqlFlexibleManager, times(1)).servers();
        verify(servers, times(1)).start(RESOURCE_GROUP_NAME, SERVER_NAME);
    }

    @Test
    void testStopFlexibleServer() {
        Servers servers = mock(Servers.class);
        when(postgreSqlFlexibleManager.servers()).thenReturn(servers);
        underTest.stopFlexibleServer(RESOURCE_GROUP_NAME, SERVER_NAME);
        verify(postgreSqlFlexibleManager, times(1)).servers();
        verify(servers, times(1)).stop(RESOURCE_GROUP_NAME, SERVER_NAME);
    }

    @Test
    void testGetFlexibleServerStatus() {
        Servers servers = mock(Servers.class);
        when(postgreSqlFlexibleManager.servers()).thenReturn(servers);
        Server server = mock(Server.class);
        when(server.state()).thenReturn(ServerState.DISABLED);
        when(servers.getByResourceGroup(eq(RESOURCE_GROUP_NAME), eq(SERVER_NAME))).thenReturn(server);
        ServerState serverState = underTest.getFlexibleServerStatus(RESOURCE_GROUP_NAME, SERVER_NAME);
        Assertions.assertEquals(ServerState.DISABLED, serverState);
    }

    @Test
    void testGetFlexibleServerStatusWhenServerIsNull() {
        Servers servers = mock(Servers.class);
        when(postgreSqlFlexibleManager.servers()).thenReturn(servers);
        ServerState serverState = underTest.getFlexibleServerStatus(RESOURCE_GROUP_NAME, SERVER_NAME);
        Assertions.assertNull(serverState);
    }
}
