package com.sequenceiq.redbeams.service.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterCache;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.TagResponse;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.NetworkV4StackRequest;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.aws.AwsNetworkV4Parameters;
import com.sequenceiq.redbeams.api.model.common.Status;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.DBStackStatus;
import com.sequenceiq.redbeams.domain.stack.DatabaseServer;
import com.sequenceiq.redbeams.domain.stack.Network;
import com.sequenceiq.redbeams.service.EnvironmentService;
import com.sequenceiq.redbeams.service.UuidGeneratorService;

@ExtendWith(MockitoExtension.class)
public class NetworkBuilderServiceTest {
    private static final Map<String, Object> SUBNET_ID_REQUEST_PARAMETERS = Map.of("netkey", "netvalue");

    private static final CloudPlatform AWS_CLOUD_PLATFORM = CloudPlatform.AWS;

    private static final String DATABASE_SERVER_ATTRIBUTES = "{ \"dbVersion\": \"10\", \"this\": \"that\" }";

    private static final Long NETWORK_ID = 12L;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private SubnetListerService subnetListerService;

    @Mock
    private SubnetChooserService subnetChooserService;

    @Mock
    private ProviderParameterCalculator providerParameterCalculator;

    @Mock
    private NetworkParameterAdder networkParameterAdder;

    @Mock
    private UuidGeneratorService uuidGeneratorService;

    @Mock
    private CloudParameterCache cloudParameterCache;

    @Mock
    private NetworkService networkService;

    @InjectMocks
    private NetworkBuilderService underTest;

    @Test
    public void testBuildNetworkWhenNoNetworkRequest() {
        // GIVEN
        DBStack dbStack = new DBStack();
        dbStack.setCloudPlatform(AWS_CLOUD_PLATFORM.name());
        List<CloudSubnet> cloudSubnets = List.of(
                new CloudSubnet("subnet-1", "", "az-a", ""),
                new CloudSubnet("subnet-2", "", "az-b", "")
        );
        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.builder()
                .withCloudPlatform(AWS_CLOUD_PLATFORM.name())
                .withTag(new TagResponse())
                .withNetwork(EnvironmentNetworkResponse.builder()
                        .withSubnetMetas(
                                Map.of(
                                        "subnet-1", cloudSubnets.get(0),
                                        "subnet-2", cloudSubnets.get(1)
                                )
                        )
                        .build())
                .build();
        when(uuidGeneratorService.randomUuid()).thenReturn("uuid");
        when(subnetListerService.listSubnets(any(), any())).thenReturn(cloudSubnets);
        when(subnetChooserService.chooseSubnets(any(), any())).thenReturn(cloudSubnets);
        when(networkParameterAdder.addSubnetIds(any(), any(), any())).thenReturn(SUBNET_ID_REQUEST_PARAMETERS);
        when(networkService.save(any(Network.class))).thenAnswer(invocation -> invocation.getArgument(0, Network.class));

        // WHEN
        Network network = underTest.buildNetwork(null, environment, dbStack);
        // THEN
        Map<String, Object> attributes = network.getAttributes().getMap();
        assertEquals("n-uuid", network.getName());
        assertEquals(1, attributes.size());
        assertEquals("netvalue", attributes.get("netkey"));

        ArgumentCaptor<List> subnetIdsCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List> azCaptor = ArgumentCaptor.forClass(List.class);
        verify(networkParameterAdder, times(1)).addSubnetIds(subnetIdsCaptor.capture(), azCaptor.capture(), any(CloudPlatform.class));
        verify(networkParameterAdder, times(1)).addParameters(eq(environment), eq(dbStack));
        assertEquals(Set.copyOf(subnetIdsCaptor.getValue()), Set.of("subnet-2", "subnet-1"));
        assertEquals(Set.copyOf(azCaptor.getValue()), Set.of("az-a", "az-b"));
    }

    @Test
    public void testBuildNetworkWhenNetworkRequestGiven() {
        // GIVEN
        DBStack dbStack = new DBStack();
        NetworkV4StackRequest networkRequest = new NetworkV4StackRequest();
        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.builder().build();
        when(uuidGeneratorService.randomUuid()).thenReturn("uuid");
        AwsNetworkV4Parameters networkParams = new AwsNetworkV4Parameters();
        networkParams.setSubnetId("subnetid");
        when(providerParameterCalculator.get(networkRequest)).thenReturn(networkParams);
        when(networkService.save(any(Network.class))).thenAnswer(invocation -> invocation.getArgument(0, Network.class));
        // WHEN
        Network network = underTest.buildNetwork(networkRequest, environment, dbStack);
        // THEN
        Map<String, Object> attributes = network.getAttributes().getMap();
        assertEquals("n-uuid", network.getName());
        assertEquals("subnetid", attributes.get("subnetId"));

        verify(networkParameterAdder, times(0)).addSubnetIds(anyList(), anyList(), any(CloudPlatform.class));
        verify(networkParameterAdder, times(1)).addParameters(eq(environment), eq(dbStack));
    }

    @Test
    public void testUpdateNetworkSubnetsWhenSubnetsUpdateEnabled() {
        DBStack dbStack = getDbStack(Status.AVAILABLE);
        List<CloudSubnet> cloudSubnets = List.of(
                new CloudSubnet("subnet-1", "", "az-a", ""),
                new CloudSubnet("subnet-2", "", "az-b", "")
        );
        when(cloudParameterCache.isDbSubnetsUpdateEnabled(dbStack.getCloudPlatform())).thenReturn(true);
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        when(environmentService.getByCrn(anyString())).thenReturn(environment);
        when(subnetListerService.listSubnets(any(), any())).thenReturn(cloudSubnets);
        when(subnetChooserService.chooseSubnets(any(), any())).thenReturn(cloudSubnets);
        when(networkParameterAdder.addSubnetIds(any(), any(), any())).thenReturn(SUBNET_ID_REQUEST_PARAMETERS);
        createNetworkMock();
        ArgumentCaptor<Network> networkArgumentCaptor = ArgumentCaptor.forClass(Network.class);
        when(networkService.save(networkArgumentCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0, Network.class));

        underTest.updateNetworkSubnets(dbStack);

        Network network = networkArgumentCaptor.getValue();
        Map<String, Object> networkAttributes = network.getAttributes().getMap();
        assertEquals("netvalue", networkAttributes.get("netkey"));
        assertEquals("original-subnet", networkAttributes.get("subnetkey"));

        ArgumentCaptor<List> subnetIdsCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List> azCaptor = ArgumentCaptor.forClass(List.class);
        verify(networkParameterAdder, times(1)).addSubnetIds(subnetIdsCaptor.capture(), azCaptor.capture(), any(CloudPlatform.class));
        assertEquals(Set.copyOf(subnetIdsCaptor.getValue()), Set.of("subnet-2", "subnet-1"));
        assertEquals(Set.copyOf(azCaptor.getValue()), Set.of("az-a", "az-b"));
    }

    @Test
    public void testUpdateNetworkSubnetsWhenSubnetsUpdateDisabled() {
        DBStack dbStack = getDbStack(Status.AVAILABLE);
        when(cloudParameterCache.isDbSubnetsUpdateEnabled(dbStack.getCloudPlatform())).thenReturn(false);

        underTest.updateNetworkSubnets(dbStack);

        verify(environmentService, never()).getByCrn(anyString());
        verify(subnetListerService, never()).listSubnets(any(), any());
        verify(subnetChooserService, never()).chooseSubnets(any(), any());
        verify(networkParameterAdder, never()).addSubnetIds(any(), any(), any());
        verifyNoInteractions(networkService);
    }

    private DBStack getDbStack(Status status) {
        DBStack dbStack = new DBStack();
        dbStack.setId(1L);
        dbStack.setName("dbstack");
        dbStack.setCloudPlatform("AZURE");
        dbStack.setEnvironmentId("envcrn");
        dbStack.setDBStackStatus(getDbStackStatus(status));
        DatabaseServer databaseServer = new DatabaseServer();
        databaseServer.setAttributes(new Json(DATABASE_SERVER_ATTRIBUTES));
        dbStack.setDatabaseServer(databaseServer);
        dbStack.setNetwork(NETWORK_ID);
        return dbStack;
    }

    private DBStackStatus getDbStackStatus(Status status) {
        DBStackStatus dbStackStatus = new DBStackStatus();
        dbStackStatus.setStatus(status);
        return dbStackStatus;
    }

    private void createNetworkMock() {
        Network network = new Network();
        network.setAttributes(new Json(Map.of("subnetkey", "original-subnet")));
        network.setId(NETWORK_ID);
        when(networkService.getById(NETWORK_ID)).thenReturn(network);
    }
}
