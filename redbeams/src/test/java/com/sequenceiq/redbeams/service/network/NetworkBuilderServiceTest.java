package com.sequenceiq.redbeams.service.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
import com.sequenceiq.redbeams.service.stack.DBStackService;

@ExtendWith(MockitoExtension.class)
public class NetworkBuilderServiceTest {
    private static final Map<String, Object> SUBNET_ID_REQUEST_PARAMETERS = Map.of("netkey", "netvalue");

    private static final CloudPlatform AWS_CLOUD_PLATFORM = CloudPlatform.AWS;

    private static final String DATABASE_SERVER_ATTRIBUTES = "{ \"dbVersion\": \"10\", \"this\": \"that\" }";

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private DBStackService dbStackService;

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

    @InjectMocks
    private NetworkBuilderService underTest;

    @Test
    public void testBuildNetworkWhenNoNetworkRequest() {
        // GIVEN
        DBStack dbStack = new DBStack();
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
        when(subnetChooserService.chooseSubnets(any(), any(), any())).thenReturn(cloudSubnets);
        when(networkParameterAdder.addSubnetIds(any(), any(), any(), any())).thenReturn(SUBNET_ID_REQUEST_PARAMETERS);
        // WHEN
        Network network = underTest.buildNetwork(null, environment, AWS_CLOUD_PLATFORM, dbStack);
        // THEN
        Map<String, Object> attributes = network.getAttributes().getMap();
        assertEquals("n-uuid", network.getName());
        assertEquals(1, attributes.size());
        assertEquals("netvalue", attributes.get("netkey"));

        ArgumentCaptor<List> subnetIdsCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List> azCaptor = ArgumentCaptor.forClass(List.class);
        verify(networkParameterAdder, times(1)).addSubnetIds(anyMap(), subnetIdsCaptor.capture(), azCaptor.capture(), any(CloudPlatform.class));
        verify(networkParameterAdder, times(1)).addParameters(anyMap(), eq(environment), eq(AWS_CLOUD_PLATFORM), eq(dbStack));
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
        // WHEN
        Network network = underTest.buildNetwork(networkRequest, environment, AWS_CLOUD_PLATFORM, dbStack);
        // THEN
        Map<String, Object> attributes = network.getAttributes().getMap();
        assertEquals("n-uuid", network.getName());
        assertEquals("subnetid", attributes.get("subnetId"));

        verify(networkParameterAdder, times(0)).addSubnetIds(anyMap(), anyList(), anyList(), any(CloudPlatform.class));
        verify(networkParameterAdder, times(1)).addParameters(anyMap(), eq(environment), eq(AWS_CLOUD_PLATFORM), eq(dbStack));
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
        when(subnetChooserService.chooseSubnets(any(), any(), any())).thenReturn(cloudSubnets);
        when(networkParameterAdder.addSubnetIds(any(), any(), any(), any())).thenReturn(SUBNET_ID_REQUEST_PARAMETERS);
        when(dbStackService.save(dbStack)).thenReturn(dbStack);

        DBStack updatedDbStack = underTest.updateNetworkSubnets(dbStack);

        Map<String, Object> networkAttributes = updatedDbStack.getNetwork().getAttributes().getMap();
        assertEquals("netvalue", networkAttributes.get("netkey"));
        assertEquals("original-subnet", networkAttributes.get("subnetkey"));

        ArgumentCaptor<List> subnetIdsCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List> azCaptor = ArgumentCaptor.forClass(List.class);
        verify(networkParameterAdder, times(1)).addSubnetIds(anyMap(), subnetIdsCaptor.capture(), azCaptor.capture(), any(CloudPlatform.class));
        assertEquals(Set.copyOf(subnetIdsCaptor.getValue()), Set.of("subnet-2", "subnet-1"));
        assertEquals(Set.copyOf(azCaptor.getValue()), Set.of("az-a", "az-b"));
    }

    @Test
    public void testUpdateNetworkSubnetsWhenSubnetsUpdateDisabled() {
        DBStack dbStack = getDbStack(Status.AVAILABLE);
        when(cloudParameterCache.isDbSubnetsUpdateEnabled(dbStack.getCloudPlatform())).thenReturn(false);

        DBStack updatedDbStack = underTest.updateNetworkSubnets(dbStack);

        verify(environmentService, never()).getByCrn(anyString());
        verify(subnetListerService, never()).listSubnets(any(), any());
        verify(subnetChooserService, never()).chooseSubnets(any(), any(), any());
        verify(networkParameterAdder, never()).addSubnetIds(any(), any(), any(), any());
        verify(dbStackService, never()).save(dbStack);

        Map<String, Object> networkAttributes = updatedDbStack.getNetwork().getAttributes().getMap();
        assertEquals("original-subnet", networkAttributes.get("subnetkey"));
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
        Network network = new Network();
        network.setAttributes(new Json(Map.of("subnetkey", "original-subnet")));
        dbStack.setNetwork(network);
        return dbStack;
    }

    private DBStackStatus getDbStackStatus(Status status) {
        DBStackStatus dbStackStatus = new DBStackStatus();
        dbStackStatus.setStatus(status);
        return dbStackStatus;
    }
}
