package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.cloud.model.network.SubnetType.PUBLIC;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.BadRequestException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.Lists;
import com.microsoft.azure.CloudException;
import com.microsoft.azure.management.resources.Deployment;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClientService;
import com.sequenceiq.cloudbreak.cloud.azure.subnet.selector.AzureSubnetSelectorService;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionParameters;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedCloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkDeletionRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkSubnetRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.SubnetRequest;
import com.sequenceiq.cloudbreak.cloud.network.NetworkCidr;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.Response;

@RunWith(MockitoJUnitRunner.class)
public class AzureNetworkConnectorTest {

    private static final String ENV_NAME = "testEnv";

    private static final String STACK_NAME = ENV_NAME + "-1";

    private static final String SUBNET_CIDR_0 = "1.1.1.1/8";

    private static final String SUBNET_CIDR_1 = "2.2.2.2/8";

    private static final String SUBNET_ID_0 = "testEnv-0";

    private static final String SUBNET_ID_1 = "testEnv-1";

    private static final String TEMPLATE = "arm template";

    private static final String STACK = "testEnv-network-stack";

    private static final String PARAMETER = "";

    private static final Region REGION = Region.region("US_WEST_2");

    private static final String RESOURCE_GROUP = "resource-group";

    private static final String ENV_CRN = "someCrn";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private AzureNetworkConnector underTest;

    @Mock
    private AzureClientService azureClientService;

    @Mock
    private AzureClient azureClient;

    @Mock
    private AzureNetworkTemplateBuilder azureNetworkTemplateBuilder;

    @Mock
    private AzureUtils azureUtils;

    @Mock
    private AzureSubnetRequestProvider azureSubnetRequestProvider;

    @Mock
    private AzureSubnetSelectorService azureSubnetSelectorService;

    @Test
    public void testPlatformShouldReturnAzurePlatform() {
        Platform actual = underTest.platform();

        assertEquals(AzureConstants.PLATFORM, actual);
    }

    @Test
    public void testVariantShouldReturnAzurePlatform() {
        Variant actual = underTest.variant();

        assertEquals(AzureConstants.VARIANT, actual);
    }

    @Test
    public void testCreateNetworkWithSubnetsShouldReturnTheNetworkNameAndSubnetName() {
        String networkCidr = "0.0.0.0/16";
        Set<NetworkSubnetRequest> subnets = new HashSet<>(Arrays.asList(createSubnetRequest(SUBNET_CIDR_0), createSubnetRequest(SUBNET_CIDR_1)));
        Deployment templateDeployment = mock(Deployment.class);
        ResourceGroup resourceGroup = mock(ResourceGroup.class);
        Map<String, Map> outputs = createOutput();

        ArrayList<SubnetRequest> subnetRequests = Lists.newArrayList(
                publicSubnetRequest("10.0.1.0/24", 0),
                publicSubnetRequest("10.0.1.0/24", 1));


        NetworkCreationRequest networkCreationRequest = createNetworkRequest(networkCidr, subnets);
        when(resourceGroup.name()).thenReturn(ENV_NAME);
        when(azureSubnetRequestProvider.provide(anyString(), anyList(), anyList(), anyBoolean())).thenReturn(subnetRequests);
        when(azureUtils.generateResourceGroupNameByNameAndId(anyString(), anyString())).thenReturn(ENV_NAME);
        when(azureClientService.getClient(networkCreationRequest.getCloudCredential())).thenReturn(azureClient);
        when(azureNetworkTemplateBuilder.build(networkCreationRequest, subnetRequests, resourceGroup.name())).thenReturn(TEMPLATE);
        when(azureClient.createTemplateDeployment(ENV_NAME, STACK_NAME, TEMPLATE, PARAMETER)).thenReturn(templateDeployment);
        when(azureClient.createResourceGroup(ENV_NAME, REGION.value(), Collections.emptyMap())).thenReturn(resourceGroup);
        when(resourceGroup.name()).thenReturn(ENV_NAME);
        when(templateDeployment.outputs()).thenReturn(outputs);

        CreatedCloudNetwork actual = underTest.createNetworkWithSubnets(networkCreationRequest);

        assertEquals(ENV_NAME, actual.getNetworkId());
        assertTrue(actual.getSubnets().stream().anyMatch(cloudSubnet -> cloudSubnet.getSubnetId().equals(SUBNET_ID_0)));
        assertTrue(actual.getSubnets().stream().anyMatch(cloudSubnet -> cloudSubnet.getSubnetId().equals(SUBNET_ID_1)));
        assertTrue(actual.getSubnets().size() == 2);
    }

    @Test
    public void testDeleteNetworkWithSubNetsShouldDeleteTheStackAndTheResourceGroup() {
        NetworkDeletionRequest networkDeletionRequest = createNetworkDeletionRequest();

        when(azureClient.getResourceGroup(networkDeletionRequest.getResourceGroup())).thenReturn(mock(ResourceGroup.class));
        when(azureClientService.getClient(networkDeletionRequest.getCloudCredential())).thenReturn(azureClient);

        underTest.deleteNetworkWithSubnets(networkDeletionRequest);

        verify(azureClient).deleteTemplateDeployment(RESOURCE_GROUP, STACK);
        verify(azureClient).deleteResourceGroup(RESOURCE_GROUP);
    }

    @Test(expected = CloudConnectorException.class)
    public void testDeleteNetworkWithSubNetsShouldThrowAnExceptionWhenTheStackDeletionFailed() {
        NetworkDeletionRequest networkDeletionRequest = createNetworkDeletionRequest();

        when(azureClient.getResourceGroup(networkDeletionRequest.getResourceGroup())).thenReturn(mock(ResourceGroup.class));
        when(azureClientService.getClient(networkDeletionRequest.getCloudCredential())).thenReturn(azureClient);
        doThrow(createCloudException()).when(azureClient).deleteTemplateDeployment(RESOURCE_GROUP, STACK);

        underTest.deleteNetworkWithSubnets(networkDeletionRequest);
    }

    @Test
    public void testDeleteNetworkWithSubNetsShouldDoNothingWhenResourceGroupNameIsNullInRequest() {
        NetworkDeletionRequest networkDeletionRequest = mock(NetworkDeletionRequest.class);
        when(networkDeletionRequest.getResourceGroup()).thenReturn(null);
        when(azureClientService.getClient(any())).thenReturn(azureClient);

        underTest.deleteNetworkWithSubnets(networkDeletionRequest);

        verify(azureClient, times(0)).getResourceGroup(any());
        verify(azureClient, times(0)).getResourceGroup(null);
        verify(azureClient, times(0)).deleteTemplateDeployment(any(), any());
        verify(azureClient, times(0)).deleteResourceGroup(any());
    }

    @Test
    public void testDeleteNetworkWithSubNetsShouldDoNothingWhenResourceGroupNameRefersToANonExistingResourceGroupInRequest() {
        NetworkDeletionRequest networkDeletionRequest = mock(NetworkDeletionRequest.class);
        String resourceGroupName = "someNotExistingResourceGroupName";
        when(networkDeletionRequest.getResourceGroup()).thenReturn(resourceGroupName);
        when(azureClientService.getClient(any())).thenReturn(azureClient);
        when(azureClient.getResourceGroup(resourceGroupName)).thenReturn(null);

        underTest.deleteNetworkWithSubnets(networkDeletionRequest);

        verify(azureClient, times(1)).getResourceGroup(any());
        verify(azureClient, times(1)).getResourceGroup(resourceGroupName);
        verify(azureClient, times(0)).deleteTemplateDeployment(any(), any());
        verify(azureClient, times(0)).deleteResourceGroup(any());
    }

    @Test
    public void testGetNetworkCidr() {
        String networkId = "vnet-1";
        String resourceGroupName = "resourceGroupName";
        String cidrBlock = "10.0.0.0/16";

        Network network = new Network(null, Map.of(AzureUtils.NETWORK_ID, networkId));
        CloudCredential credential = new CloudCredential();
        com.microsoft.azure.management.network.Network azureNetwork = mock(com.microsoft.azure.management.network.Network.class);

        when(azureClientService.getClient(credential)).thenReturn(azureClient);
        when(azureUtils.getCustomResourceGroupName(network)).thenReturn(resourceGroupName);
        when(azureUtils.getCustomNetworkId(network)).thenReturn(networkId);
        when(azureClient.getNetworkByResourceGroup(resourceGroupName, networkId)).thenReturn(azureNetwork);
        when(azureNetwork.addressSpaces()).thenReturn(List.of(cidrBlock));

        NetworkCidr result = underTest.getNetworkCidr(network, credential);
        assertEquals(cidrBlock, result.getCidr());
    }

    @Test
    public void testGetNetworkCidrWithoutResult() {
        String networkId = "vnet-1";
        String resourceGroupName = "resourceGroupName";

        Network network = new Network(null, Map.of(AzureUtils.NETWORK_ID, networkId));
        CloudCredential credential = new CloudCredential();
        com.microsoft.azure.management.network.Network azureNetwork = mock(com.microsoft.azure.management.network.Network.class);

        when(azureClientService.getClient(credential)).thenReturn(azureClient);
        when(azureUtils.getCustomResourceGroupName(network)).thenReturn(resourceGroupName);
        when(azureUtils.getCustomNetworkId(network)).thenReturn(networkId);
        when(azureClient.getNetworkByResourceGroup(resourceGroupName, networkId)).thenReturn(azureNetwork);
        when(azureNetwork.addressSpaces()).thenReturn(List.of());

        thrown.expect(BadRequestException.class);
        thrown.expectMessage(String.format("Network could not be fetch from Azure with resource group name: %s and network id: %s",
                resourceGroupName, networkId));

        underTest.getNetworkCidr(network, credential);
    }

    @Test
    public void testGetNetworkCidrMoreThanOne() {
        String networkId = "vnet-1";
        String resourceGroupName = "resourceGroupName";
        String cidrBlock1 = "10.0.0.0/16";
        String cidrBlock2 = "10.23.0.0/16";

        Network network = new Network(null, Map.of(AzureUtils.NETWORK_ID, networkId));
        CloudCredential credential = new CloudCredential();
        com.microsoft.azure.management.network.Network azureNetwork = mock(com.microsoft.azure.management.network.Network.class);

        when(azureClientService.getClient(credential)).thenReturn(azureClient);
        when(azureUtils.getCustomResourceGroupName(network)).thenReturn(resourceGroupName);
        when(azureUtils.getCustomNetworkId(network)).thenReturn(networkId);
        when(azureClient.getNetworkByResourceGroup(resourceGroupName, networkId)).thenReturn(azureNetwork);
        when(azureNetwork.addressSpaces()).thenReturn(List.of(cidrBlock1, cidrBlock2));

        NetworkCidr result = underTest.getNetworkCidr(network, credential);
        assertEquals(cidrBlock1, result.getCidr());
    }

    @Test
    public void testGetNetworWhenAzureNetworkNull() {
        String networkId = "vnet-1";
        String resourceGroupName = "resourceGroupName";

        Network network = new Network(null, Map.of(AzureUtils.NETWORK_ID, networkId));
        CloudCredential credential = new CloudCredential();

        when(azureClientService.getClient(credential)).thenReturn(azureClient);
        when(azureUtils.getCustomResourceGroupName(network)).thenReturn(resourceGroupName);
        when(azureUtils.getCustomNetworkId(network)).thenReturn(networkId);
        when(azureClient.getNetworkByResourceGroup(resourceGroupName, networkId)).thenReturn(null);

        thrown.expect(BadRequestException.class);
        thrown.expectMessage(String.format("Network could not be fetch from Azure with resource group name: %s and network id: %s",
                resourceGroupName, networkId));
        underTest.getNetworkCidr(network, credential);
    }

    public void testSelectSubnetsThenSubnetSelectorIsCalled() {
        underTest.chooseSubnets(List.of(), SubnetSelectionParameters.builder().build());

        verify(azureSubnetSelectorService).select(anyList(), any());
    }

    private Map<String, Map> createOutput() {
        Map<String, Map> outputs = new HashMap<>();
        outputs.put("networkId", Collections.singletonMap("value", ENV_NAME));
        outputs.put("subnetId0", Collections.singletonMap("value", SUBNET_ID_0));
        outputs.put("subnetId1", Collections.singletonMap("value", SUBNET_ID_1));
        outputs.put("subnetCidr0", Collections.singletonMap("value", SUBNET_CIDR_0));
        outputs.put("subnetCidr1", Collections.singletonMap("value", SUBNET_CIDR_1));
        return outputs;
    }

    private NetworkCreationRequest createNetworkRequest(String networkCidr, Set<NetworkSubnetRequest> subnets) {
        return new NetworkCreationRequest.Builder()
                .withStackName(STACK_NAME)
                .withEnvName(ENV_NAME)
                .withEnvCrn(ENV_CRN)
                .withCloudCredential(new CloudCredential("1", "credential"))
                .withRegion(REGION)
                .withNetworkCidr(networkCidr)
                .withPublicSubnets(subnets)
                .withPrivateSubnets(subnets)
                .build();
    }

    private NetworkDeletionRequest createNetworkDeletionRequest() {
        return new NetworkDeletionRequest.Builder()
                .withRegion(REGION.value())
                .withStackName(STACK)
                .withCloudCredential(new CloudCredential("1", "credential"))
                .withResourceGroup(RESOURCE_GROUP)
                .build();
    }

    public SubnetRequest publicSubnetRequest(String cidr, int index) {
        SubnetRequest subnetRequest = new SubnetRequest();
        subnetRequest.setIndex(index);
        subnetRequest.setPublicSubnetCidr(cidr);
        subnetRequest.setSubnetGroup(index % 3);
        subnetRequest.setAvailabilityZone("az");
        return subnetRequest;
    }

    private CloudException createCloudException() {
        return new CloudException("error", Response.success(ResponseBody.create(MediaType.get("application/json; charset=utf-8"), "error body")));
    }

    private NetworkSubnetRequest createSubnetRequest(String s) {
        return new NetworkSubnetRequest(s, PUBLIC);
    }
}
