package com.sequenceiq.cloudbreak.cloud.azure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import com.azure.core.http.HttpResponse;
import com.azure.core.http.rest.PagedIterable;
import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.postgresqlflexibleserver.PostgreSqlManager;
import com.azure.resourcemanager.postgresqlflexibleserver.models.CapabilityStatus;
import com.azure.resourcemanager.postgresqlflexibleserver.models.FlexibleServerCapability;
import com.azure.resourcemanager.postgresqlflexibleserver.models.LocationBasedCapabilities;
import com.azure.resourcemanager.postgresqlflexibleserver.models.Server;
import com.azure.resourcemanager.postgresqlflexibleserver.models.ServerState;
import com.azure.resourcemanager.postgresqlflexibleserver.models.ServerVersion;
import com.azure.resourcemanager.postgresqlflexibleserver.models.Servers;
import com.sequenceiq.cloudbreak.cloud.azure.resource.domain.AzureCoordinate;
import com.sequenceiq.cloudbreak.cloud.azure.util.AzureExceptionHandler;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.Region;

@ExtendWith(MockitoExtension.class)
class AzureFlexibleServerClientTest {

    private static final String RESOURCE_GROUP_NAME = "rg";

    private static final String SERVER_NAME = "serverName";

    private static final String NEW_PASSWORD = "newPassword";

    @Spy
    private AzureExceptionHandler azureExceptionHandler;

    @Mock
    private PostgreSqlManager postgreSqlFlexibleManager;

    @Mock
    private AzureListResultFactory azureListResultFactory;

    @InjectMocks
    private AzureFlexibleServerClient underTest;

    @BeforeEach
    void setUp() {
        lenient().when(azureListResultFactory.create(any())).thenAnswer((Answer<AzureListResult>) invocationOnMock -> {
            PagedIterable pagedIterable = invocationOnMock.getArgument(0, PagedIterable.class);
            return new AzureListResult(pagedIterable, azureExceptionHandler);
        });
        lenient().when(azureListResultFactory.list(any())).thenCallRealMethod();
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
        assertEquals(ServerState.DISABLED, serverState);
    }

    @Test
    void testGetFlexibleServerStatusWhenServerIsNull() {
        Servers servers = mock(Servers.class);
        when(postgreSqlFlexibleManager.servers()).thenReturn(servers);
        ServerState serverState = underTest.getFlexibleServerStatus(RESOURCE_GROUP_NAME, SERVER_NAME);
        assertEquals(AzureFlexibleServerClient.UNKNOWN, serverState);
    }

    @Test
    void testGetFlexibleServerStatusWhenNotFound() {
        Servers servers = mock(Servers.class);
        HttpResponse httpResponse = mock(HttpResponse.class);
        when(httpResponse.getStatusCode()).thenReturn(404);
        ManagementException managementException = new ManagementException("", httpResponse);
        when(servers.getByResourceGroup(eq(RESOURCE_GROUP_NAME), eq(SERVER_NAME))).thenThrow(managementException);
        when(postgreSqlFlexibleManager.servers()).thenReturn(servers);
        ServerState serverState = underTest.getFlexibleServerStatus(RESOURCE_GROUP_NAME, SERVER_NAME);
        assertEquals(AzureFlexibleServerClient.UNKNOWN, serverState);
    }

    @Test
    void testGetFlexibleServerStatusWhenUnhandledException() {
        Servers servers = mock(Servers.class);
        HttpResponse httpResponse = mock(HttpResponse.class);
        ManagementException managementException = new ManagementException("", httpResponse);
        when(servers.getByResourceGroup(eq(RESOURCE_GROUP_NAME), eq(SERVER_NAME))).thenThrow(managementException);
        when(postgreSqlFlexibleManager.servers()).thenReturn(servers);
        assertThrows(ManagementException.class, () -> underTest.getFlexibleServerStatus(RESOURCE_GROUP_NAME, SERVER_NAME));
    }

    @Test
    void testGetFlexibleServerCapability() {
        Map<Region, AzureCoordinate> regionMap = Map.of(
                Region.region("us-west-1"), azureCoordinate("us-west-1"),
                Region.region("us-west-2"), azureCoordinate("us-west-2"));
        LocationBasedCapabilities locationBasedCapabilities = mock(LocationBasedCapabilities.class);
        PagedIterable<FlexibleServerCapability> emptyCapabilities = mock(PagedIterable.class);
        when(emptyCapabilities.stream()).thenReturn(Stream.empty());
        PagedIterable<FlexibleServerCapability> capabilities = mock(PagedIterable.class);
        FlexibleServerCapability capability = mock(FlexibleServerCapability.class);
        FlexibleServerCapability disabledCapability = mock(FlexibleServerCapability.class);
        when(disabledCapability.status()).thenReturn(CapabilityStatus.DISABLED);
        when(capabilities.stream()).thenReturn(Stream.of(capability, disabledCapability));

        when(locationBasedCapabilities.execute("us-west-1key")).thenReturn(emptyCapabilities);
        when(locationBasedCapabilities.execute("us-west-2key")).thenReturn(capabilities);
        when(postgreSqlFlexibleManager.locationBasedCapabilities()).thenReturn(locationBasedCapabilities);

        Map<Region, Optional<FlexibleServerCapability>> actualCapabilityMap = underTest.getFlexibleServerCapabilityMap(regionMap);
        assertEquals(actualCapabilityMap.get(Region.region("us-west-1")), Optional.empty());
        assertEquals(actualCapabilityMap.get(Region.region("us-west-2")).get(), capability);
    }

    @Test
    void testGetFlexibleServerCapabilityThrowsException() {
        Map<Region, AzureCoordinate> regionMap = Map.of(
                Region.region("us-west-1"), azureCoordinate("us-west-1"),
                Region.region("us-west-2"), azureCoordinate("us-west-2"));
        LocationBasedCapabilities locationBasedCapabilities = mock(LocationBasedCapabilities.class);
        PagedIterable<FlexibleServerCapability> capabilities = mock(PagedIterable.class);
        FlexibleServerCapability capability = mock(FlexibleServerCapability.class);
        when(capabilities.stream()).thenReturn(Stream.of(capability));

        when(locationBasedCapabilities.execute("us-west-1key")).thenThrow(new ManagementException(null, null));
        when(locationBasedCapabilities.execute("us-west-2key")).thenReturn(capabilities);
        when(postgreSqlFlexibleManager.locationBasedCapabilities()).thenReturn(locationBasedCapabilities);

        Map<Region, Optional<FlexibleServerCapability>> actualCapabilityMap = underTest.getFlexibleServerCapabilityMap(regionMap);
        assertEquals(actualCapabilityMap.get(Region.region("us-west-1")), Optional.empty());
        assertEquals(actualCapabilityMap.get(Region.region("us-west-2")).get(), capability);
    }

    @Test
    void testUpdateAdministratorLoginPassword() {
        Servers servers = mock(Servers.class);
        when(postgreSqlFlexibleManager.servers()).thenReturn(servers);
        Server server = mock(Server.class);
        when(servers.getByResourceGroup(eq(RESOURCE_GROUP_NAME), eq(SERVER_NAME))).thenReturn(server);
        Server.Update update = mock(Server.Update.class);
        when(server.update()).thenReturn(update);
        when(update.withAdministratorLoginPassword(eq(NEW_PASSWORD))).thenReturn(update);
        when(update.apply()).thenReturn(server);
        underTest.updateAdministratorLoginPassword(RESOURCE_GROUP_NAME, SERVER_NAME, NEW_PASSWORD);
        verify(postgreSqlFlexibleManager, times(1)).servers();
        verify(servers, times(1)).getByResourceGroup(eq(RESOURCE_GROUP_NAME), eq(SERVER_NAME));
        verify(server, times(1)).update();
        verify(update, times(1)).withAdministratorLoginPassword(eq(NEW_PASSWORD));
        verify(update, times(1)).apply();
    }

    @Test
    void testUpdateAdministratorLoginPasswordWhenServerIsNull() {
        Servers servers = mock(Servers.class);
        when(postgreSqlFlexibleManager.servers()).thenReturn(servers);
        when(servers.getByResourceGroup(eq(RESOURCE_GROUP_NAME), eq(SERVER_NAME))).thenReturn(null);

        CloudConnectorException cloudConnectorException = assertThrows(CloudConnectorException.class,
                () -> underTest.updateAdministratorLoginPassword(RESOURCE_GROUP_NAME, SERVER_NAME, NEW_PASSWORD));
        verify(postgreSqlFlexibleManager, times(1)).servers();
        verify(servers, times(1)).getByResourceGroup(eq(RESOURCE_GROUP_NAME), eq(SERVER_NAME));
        assertThat(cloudConnectorException.getMessage()).contains("Flexible server not found with name");
    }

    // Successfully upgrades the server to the specified target version
    @ParameterizedTest
    @MethodSource("provideTargetVersions")
    void testDifferentUpgradePaths(String targetVersion, boolean assertException) {
        if (assertException) {
            CloudConnectorException cloudConnectorException = assertThrows(CloudConnectorException.class,
                    () -> underTest.upgrade(RESOURCE_GROUP_NAME, SERVER_NAME, targetVersion));
            assertEquals("Upgrading Azure PostgreSQL Flexible Server to version " + targetVersion + " is not supported",
                    cloudConnectorException.getMessage());

        } else {
            Servers servers = mock(Servers.class);
            when(postgreSqlFlexibleManager.servers()).thenReturn(servers);
            Server server = mock(Server.class);
            when(servers.getByResourceGroup(eq(RESOURCE_GROUP_NAME), eq(SERVER_NAME))).thenReturn(server);
            Server.Update update = mock(Server.Update.class);
            when(server.update()).thenReturn(update);
            when(update.withVersion(any(ServerVersion.class))).thenReturn(update);
            when(update.apply()).thenReturn(server);

            underTest.upgrade(RESOURCE_GROUP_NAME, SERVER_NAME, targetVersion);
            verify(postgreSqlFlexibleManager, times(1)).servers();
            verify(servers, times(1)).getByResourceGroup(eq(RESOURCE_GROUP_NAME), eq(SERVER_NAME));
            verify(server, times(1)).update();
            verify(update, times(1)).withVersion(eq(ServerVersion.fromString(targetVersion)));
            verify(update, times(1)).apply();
        }
}

    static Stream<Arguments> provideTargetVersions() {
        return Stream.of(
                Arguments.of(null, true),
                Arguments.of("", true),
                Arguments.of("9.6", true),
                Arguments.of("10", true),
                Arguments.of("11", false),
                Arguments.of("12", false),
                Arguments.of("13", false),
                Arguments.of("14", false),
                Arguments.of("15", false),
                Arguments.of("16", false),
                Arguments.of("target", true)
                );
    }

    private AzureCoordinate azureCoordinate(String name) {
        return AzureCoordinate.AzureCoordinateBuilder.builder()
                .longitude("1")
                .latitude("1")
                .displayName(name)
                .key(name + "key")
                .k8sSupported(false)
                .entitlements(List.of())
                .build();
    }
}