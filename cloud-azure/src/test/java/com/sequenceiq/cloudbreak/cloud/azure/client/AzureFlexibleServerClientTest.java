package com.sequenceiq.cloudbreak.cloud.azure.client;

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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import com.azure.resourcemanager.postgresqlflexibleserver.models.Servers;
import com.sequenceiq.cloudbreak.cloud.azure.resource.domain.AzureCoordinate;
import com.sequenceiq.cloudbreak.cloud.azure.util.AzureExceptionHandler;
import com.sequenceiq.cloudbreak.cloud.model.Region;

@ExtendWith(MockitoExtension.class)
class AzureFlexibleServerClientTest {
    private static final String RESOURCE_GROUP_NAME = "rg";

    private static final String SERVER_NAME = "serverName";

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
        Assertions.assertEquals(ServerState.DISABLED, serverState);
    }

    @Test
    void testGetFlexibleServerStatusWhenServerIsNull() {
        Servers servers = mock(Servers.class);
        when(postgreSqlFlexibleManager.servers()).thenReturn(servers);
        ServerState serverState = underTest.getFlexibleServerStatus(RESOURCE_GROUP_NAME, SERVER_NAME);
        Assertions.assertEquals(AzureFlexibleServerClient.UNKNOWN, serverState);
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
        Assertions.assertNull(serverState);
    }

    @Test
    void testGetFlexibleServerStatusWhenUnhandledException() {
        Servers servers = mock(Servers.class);
        HttpResponse httpResponse = mock(HttpResponse.class);
        ManagementException managementException = new ManagementException("", httpResponse);
        when(servers.getByResourceGroup(eq(RESOURCE_GROUP_NAME), eq(SERVER_NAME))).thenThrow(managementException);
        when(postgreSqlFlexibleManager.servers()).thenReturn(servers);
        Assertions.assertThrows(ManagementException.class, () -> underTest.getFlexibleServerStatus(RESOURCE_GROUP_NAME, SERVER_NAME));
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
        Assertions.assertEquals(actualCapabilityMap.get(Region.region("us-west-1")), Optional.empty());
        Assertions.assertEquals(actualCapabilityMap.get(Region.region("us-west-2")).get(), capability);
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
        Assertions.assertEquals(actualCapabilityMap.get(Region.region("us-west-1")), Optional.empty());
        Assertions.assertEquals(actualCapabilityMap.get(Region.region("us-west-2")).get(), capability);
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
