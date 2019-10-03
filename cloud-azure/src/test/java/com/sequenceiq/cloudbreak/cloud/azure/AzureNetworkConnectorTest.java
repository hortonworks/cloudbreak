package com.sequenceiq.cloudbreak.cloud.azure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.BadRequestException;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.microsoft.azure.CloudException;
import com.microsoft.azure.management.resources.Deployment;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClientService;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedCloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkDeletionRequest;

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

    @Test
    public void testPlatformShouldReturnAzurePlatform() {
        Platform actual = underTest.platform();

        Assert.assertEquals(AzureConstants.PLATFORM, actual);
    }

    @Test
    public void testVariantShouldReturnAzurePlatform() {
        Variant actual = underTest.variant();

        Assert.assertEquals(AzureConstants.VARIANT, actual);
    }

    @Test
    public void testCreateNetworkWithSubnetsShouldReturnTheNetworkNameAndSubnetName() {
        String networkCidr = "0.0.0.0/16";
        Set<String> subnetCidrs = new HashSet<>(Arrays.asList(SUBNET_CIDR_0, SUBNET_CIDR_1));
        Deployment templateDeployment = mock(Deployment.class);
        ResourceGroup resourceGroup = mock(ResourceGroup.class);
        Map<String, Map> outputs = createOutput();

        NetworkCreationRequest networkCreationRequest = createNetworkRequest(networkCidr, subnetCidrs);

        when(azureClientService.getClient(networkCreationRequest.getCloudCredential())).thenReturn(azureClient);
        when(azureNetworkTemplateBuilder.build(networkCreationRequest)).thenReturn(TEMPLATE);
        when(azureClient.createTemplateDeployment(ENV_NAME, STACK_NAME, TEMPLATE, PARAMETER)).thenReturn(templateDeployment);
        when(azureClient.createResourceGroup(ENV_NAME, REGION.value(), Collections.emptyMap(), Collections.emptyMap())).thenReturn(resourceGroup);
        when(resourceGroup.name()).thenReturn(ENV_NAME);
        when(templateDeployment.outputs()).thenReturn(outputs);

        CreatedCloudNetwork actual = underTest.createNetworkWithSubnets(networkCreationRequest, "creatorUser");

        assertEquals(ENV_NAME, actual.getNetworkId());
        assertTrue(actual.getSubnets().stream().anyMatch(cloudSubnet -> cloudSubnet.getSubnetId().equals(SUBNET_ID_0)));
        assertTrue(actual.getSubnets().stream().anyMatch(cloudSubnet -> cloudSubnet.getSubnetId().equals(SUBNET_ID_1)));
        assertTrue(actual.getSubnets().stream().anyMatch(cloudSubnet -> cloudSubnet.getCidr().equals(SUBNET_CIDR_0)));
        assertTrue(actual.getSubnets().stream().anyMatch(cloudSubnet -> cloudSubnet.getCidr().equals(SUBNET_CIDR_1)));
        assertTrue(actual.getSubnets().size() == 2);
    }

    @Test
    public void testDeleteNetworkWithSubNetsShouldDeleteTheStackAndTheResourceGroup() {
        NetworkDeletionRequest networkDeletionRequest = createNetworkDeletionRequest();
        when(azureClientService.getClient(networkDeletionRequest.getCloudCredential())).thenReturn(azureClient);

        underTest.deleteNetworkWithSubnets(networkDeletionRequest);

        verify(azureClient).deleteTemplateDeployment(RESOURCE_GROUP, STACK);
        verify(azureClient).deleteResourceGroup(RESOURCE_GROUP);
    }

    @Test(expected = CloudConnectorException.class)
    public void testDeleteNetworkWithSubNetsShouldThrowAnExceptionWhenTheStackDeletionFailed() {
        NetworkDeletionRequest networkDeletionRequest = createNetworkDeletionRequest();

        when(azureClientService.getClient(networkDeletionRequest.getCloudCredential())).thenReturn(azureClient);
        doThrow(createCloudException()).when(azureClient).deleteTemplateDeployment(RESOURCE_GROUP, STACK);

        underTest.deleteNetworkWithSubnets(networkDeletionRequest);
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

        String result = underTest.getNetworkCidr(network, credential);
        assertEquals(cidrBlock, result);
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

        String result = underTest.getNetworkCidr(network, credential);
        assertEquals(cidrBlock1, result);
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

    private Map<String, Map> createOutput() {
        Map<String, Map> outputs = new HashMap<>();
        outputs.put("networkId", Collections.singletonMap("value", ENV_NAME));
        outputs.put("subnetId0", Collections.singletonMap("value", SUBNET_ID_0));
        outputs.put("subnetId1", Collections.singletonMap("value", SUBNET_ID_1));
        outputs.put("subnetCidr0", Collections.singletonMap("value", SUBNET_CIDR_0));
        outputs.put("subnetCidr1", Collections.singletonMap("value", SUBNET_CIDR_1));
        return outputs;
    }

    private NetworkCreationRequest createNetworkRequest(String networkCidr, Set<String> subnetCidrs) {
        return new NetworkCreationRequest.Builder()
                .withStackName(STACK_NAME)
                .withEnvName(ENV_NAME)
                .withCloudCredential(new CloudCredential("1", "credential"))
                .withRegion(REGION)
                .withNetworkCidr(networkCidr)
                .withSubnetCidrs(subnetCidrs)
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

    private CloudException createCloudException() {
        return new CloudException("error", Response.success(ResponseBody.create(MediaType.get("application/json; charset=utf-8"), "error body")));
    }

}
