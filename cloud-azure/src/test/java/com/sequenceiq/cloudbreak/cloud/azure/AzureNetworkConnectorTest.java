package com.sequenceiq.cloudbreak.cloud.azure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.microsoft.azure.management.resources.Deployment;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClientService;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedCloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkCreationRequest;

@RunWith(MockitoJUnitRunner.class)
public class AzureNetworkConnectorTest {

    private static final String ENV_NAME = "testEnv";

    private static final String SUBNET_CIDR_0 = "1.1.1.1/8";

    private static final String SUBNET_CIDR_1 = "2.2.2.2/8";

    private static final String SUBNET_ID_0 = "testEnv-0";

    private static final String SUBNET_ID_1 = "testEnv-1";

    private static final String TEMPLATE = "arm template";

    private static final String STACK = "testEnv-network-stack";

    private static final String PARAMETER = "";

    @InjectMocks
    private AzureNetworkConnector underTest;

    @Mock
    private AzureClientService azureClientService;

    @Mock
    private AzureClient azureClient;

    @Mock
    private AzureNetworkTemplateBuilder azureNetworkTemplateBuilder;

    @Before
    public void before() {
    }

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
        Region region = Region.region("US_WEST_2");
        String networkCidr = "0.0.0.0/16";
        Set<String> subnetCidrs = new HashSet<>(Arrays.asList(SUBNET_CIDR_0, SUBNET_CIDR_1));
        Deployment templateDeployment = Mockito.mock(Deployment.class);
        ResourceGroup resourceGroup = Mockito.mock(ResourceGroup.class);
        Map<String, Map> outputs = createOutput();

        NetworkCreationRequest networkCreationRequest = createNetworkRequest(region, networkCidr, subnetCidrs);

        when(azureClientService.getClient(networkCreationRequest.getCloudCredential())).thenReturn(azureClient);
        when(azureNetworkTemplateBuilder.build(networkCreationRequest)).thenReturn(TEMPLATE);
        when(azureNetworkTemplateBuilder.buildParameters()).thenReturn(PARAMETER);
        when(azureClient.createTemplateDeployment(ENV_NAME, STACK, TEMPLATE, PARAMETER)).thenReturn(templateDeployment);
        when(azureClient.createResourceGroup(ENV_NAME, region.value(), Collections.emptyMap(), Collections.emptyMap())).thenReturn(resourceGroup);
        when(resourceGroup.name()).thenReturn(ENV_NAME);
        when(templateDeployment.outputs()).thenReturn(outputs);

        CreatedCloudNetwork actual = underTest.createNetworkWithSubnets(networkCreationRequest);

        assertEquals(ENV_NAME, actual.getNetworkId());
        assertTrue(actual.getSubnets().stream().anyMatch(cloudSubnet -> cloudSubnet.getSubnetId().equals(SUBNET_ID_0)));
        assertTrue(actual.getSubnets().stream().anyMatch(cloudSubnet -> cloudSubnet.getSubnetId().equals(SUBNET_ID_1)));
        assertTrue(actual.getSubnets().stream().anyMatch(cloudSubnet -> cloudSubnet.getCidr().equals(SUBNET_CIDR_0)));
        assertTrue(actual.getSubnets().stream().anyMatch(cloudSubnet -> cloudSubnet.getCidr().equals(SUBNET_CIDR_1)));
        assertTrue(actual.getSubnets().size() == 2);
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

    private NetworkCreationRequest createNetworkRequest(Region region, String networkCidr, Set<String> subnetCidrs) {
        return new NetworkCreationRequest.Builder()
                .withEnvName(ENV_NAME)
                .withCloudCredential(new CloudCredential("1", "credential"))
                .withRegion(region)
                .withNetworkCidr(networkCidr)
                .withSubnetCidrs(subnetCidrs)
                .build();
    }

}