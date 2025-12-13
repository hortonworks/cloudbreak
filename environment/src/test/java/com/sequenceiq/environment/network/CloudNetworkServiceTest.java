package com.sequenceiq.environment.network;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetworks;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.common.api.type.DeploymentRestriction;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.Region;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.validation.network.NetworkTestUtils;
import com.sequenceiq.environment.network.dto.AwsParams;
import com.sequenceiq.environment.network.dto.AzureParams;
import com.sequenceiq.environment.network.dto.GcpParams;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.platformresource.PlatformParameterService;
import com.sequenceiq.environment.platformresource.PlatformResourceRequest;

@ExtendWith(MockitoExtension.class)
class CloudNetworkServiceTest {

    private static final byte AMOUNT_OF_SUBNETS = 2;

    private static final String TEST_SUBNET_ID = "test-subnet-id";

    private static final Set<String> DEFAULT_TEST_SUBNET_ID_SET = Set.of(TEST_SUBNET_ID);

    private static final String TEST_PUBLIC_SUBNET_ID = "test-public-subnet-id";

    private static final Set<String> DEFAULT_TEST_PUBLIC_SUBNET_ID_SET = Set.of(TEST_PUBLIC_SUBNET_ID);

    private static final String DEFAULT_TEST_VPC_ID = "test-vpc-id";

    private static final String AWS_CLOUD_PLATFORM = "AWS";

    private static final String AZURE_CLOUD_PLATFORM = "AZURE";

    private static final String DEFAULT_TEST_REGION_NAME = "somewhere-over-the-rainbo";

    @Mock
    private PlatformParameterService platformParameterService;

    @Mock
    private CloudNetworks cloudNetworks;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private EnvironmentDto testEnvironmentDto;

    @Mock
    private NetworkDto testNetworkDto;

    @Mock
    private Environment testEnvironment;

    @InjectMocks
    private CloudNetworkService underTest;

    @BeforeEach
    void setUp() {
        Region testRegion = new Region();
        testRegion.setName(DEFAULT_TEST_REGION_NAME);
        testRegion.setDisplayName(DEFAULT_TEST_REGION_NAME);

        lenient().when(cloudNetworks.getCloudNetworkResponses()).thenReturn(cloudSubnetCreator());
        lenient().when(platformParameterService.getCloudNetworks(any(PlatformResourceRequest.class))).thenReturn(cloudNetworks);
        lenient().when(testEnvironmentDto.getRegions()).thenReturn(Set.of(testRegion));
        lenient().when(testEnvironment.getRegionSet()).thenReturn(Set.of(testRegion));
    }

    @Test
    @DisplayName("when retrieveSubnetMetadata has called with EnvironmentDto and with a null NetworkDto then empty map should return")
    void testRetrieveSubnetMetadataByEnvironmentDtoWhenNetworkIsNullThenEmptyMapReturns() {
        Map<String, CloudSubnet> result = underTest.retrieveSubnetMetadata(testEnvironmentDto, null);
        Map<String, CloudSubnet> gatewayResult = underTest.retrieveEndpointGatewaySubnetMetadata(testEnvironmentDto, null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertNotNull(gatewayResult);
        assertTrue(gatewayResult.isEmpty());
    }

    @Test
    @DisplayName("when retrieveSubnetMetadata has called with EnvironmentDto and empty subnetIds, then empty map should return")
    void testRetrieveSubnetMetadataByEnvironmentDtoWhenNetworkIsNotNullButSubnetIdSetIsEmptyThenEmptyMapReturns() {
        when(testNetworkDto.getSubnetIds()).thenReturn(Collections.emptySet());
        when(testNetworkDto.getEndpointGatewaySubnetIds()).thenReturn(Collections.emptySet());
        Map<String, CloudSubnet> result = underTest.retrieveSubnetMetadata(testEnvironmentDto, testNetworkDto);
        Map<String, CloudSubnet> gatewayResult = underTest.retrieveEndpointGatewaySubnetMetadata(testEnvironmentDto, testNetworkDto);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertNotNull(gatewayResult);
        assertTrue(gatewayResult.isEmpty());
    }

    @Test
    @DisplayName("when retrieveSubnetMetadata has called with EnvironmentDto and the platform is AWS then we should fetch the cloud networks from the provider")
    void testRetrieveSubnetMetadataByEnvironmentDtoWhenPlatformIsAwsThenWeShouldFetchTheCloudNetworksFromProvider() {
        AwsParams awsParams = NetworkTestUtils.getAwsParams(DEFAULT_TEST_VPC_ID);

        CloudSubnet cloudSubnet = new CloudSubnet.Builder()
                .id(TEST_SUBNET_ID)
                .name("someSubnet")
                .build();
        CloudNetwork cloudNetwork = new CloudNetwork("someCloudNetwork", "someCloudNetwork", Set.of(cloudSubnet),
                Collections.emptyMap());
        Map<String, Set<CloudNetwork>> cloudNetworksFromProvider = new LinkedHashMap<>();
        cloudNetworksFromProvider.put(DEFAULT_TEST_REGION_NAME, Set.of(cloudNetwork));
        CloudSubnet cloudSubnet2 = new CloudSubnet.Builder()
                .id(TEST_SUBNET_ID)
                .name("someSubnet")
                .build();
        CloudNetwork cloudNetwork2 = new CloudNetwork("someCloudNetwork", "someCloudNetwork", Set.of(cloudSubnet2),
                Collections.emptyMap());
        Map<String, Set<CloudNetwork>> cloudNetworksFromProvider2 = new LinkedHashMap<>();
        cloudNetworksFromProvider2.put(DEFAULT_TEST_REGION_NAME, Set.of(cloudNetwork2));

        when(testNetworkDto.getSubnetIds()).thenReturn(DEFAULT_TEST_SUBNET_ID_SET);
        when(testNetworkDto.getEndpointGatewaySubnetIds()).thenReturn(DEFAULT_TEST_SUBNET_ID_SET);
        when(testNetworkDto.getAws()).thenReturn(awsParams);
        when(testEnvironmentDto.getCloudPlatform()).thenReturn(AWS_CLOUD_PLATFORM);
        when(cloudNetworks.getCloudNetworkResponses()).thenReturn(cloudNetworksFromProvider);

        Map<String, CloudSubnet> result = underTest.retrieveSubnetMetadata(testEnvironmentDto, testNetworkDto);

        when(cloudNetworks.getCloudNetworkResponses()).thenReturn(cloudNetworksFromProvider2);
        Map<String, CloudSubnet> gatewayResult = underTest.retrieveEndpointGatewaySubnetMetadata(testEnvironmentDto, testNetworkDto);

        byte expectedAmountOfResultCloudSubnet = 1;

        assertNotNull(result);
        assertEquals(expectedAmountOfResultCloudSubnet, result.size(), "The amount of result CloudSubnet(s) must be: " + expectedAmountOfResultCloudSubnet);
        assertEquals(TEST_SUBNET_ID, result.get(result.keySet().iterator().next()).getId());

        assertNotNull(gatewayResult);
        assertEquals(expectedAmountOfResultCloudSubnet, gatewayResult.size(),
                "The amount of result CloudSubnet(s) for the gateway endpoint must be: " + expectedAmountOfResultCloudSubnet);
        assertEquals(TEST_SUBNET_ID, gatewayResult.get(gatewayResult.keySet().iterator().next()).getId());

        verify(platformParameterService, times(2)).getCloudNetworks(any());
        assertThat(result.get(TEST_SUBNET_ID).getDeploymentRestrictions()).containsExactlyElementsOf(DeploymentRestriction.ALL);
        assertThat(gatewayResult.get(TEST_SUBNET_ID).getDeploymentRestrictions()).containsExactlyElementsOf(DeploymentRestriction.ENDPOINT_ACCESS_GATEWAYS);
    }

    @Test
    @DisplayName("when retrieveSubnetMetadata has called with EnvironmentDto and the platform is AWS and no endpoint subnets are provided " +
            "then an empty endpoint map should return")
    void testRetrieveSubnetMetadataByEnvironmentDtoWhenPlatformIsAwsAndNoEndpointSubnetsAreProvided() {
        AwsParams awsParams = NetworkTestUtils.getAwsParams(DEFAULT_TEST_VPC_ID);

        CloudSubnet cloudSubnet = new CloudSubnet.Builder()
                .id(TEST_SUBNET_ID)
                .name("someSubnet")
                .build();
        CloudNetwork cloudNetwork = new CloudNetwork("someCloudNetwork", TEST_SUBNET_ID, Set.of(cloudSubnet),
                Collections.emptyMap());
        Map<String, Set<CloudNetwork>> cloudNetworksFromProvider = new LinkedHashMap<>();
        cloudNetworksFromProvider.put(DEFAULT_TEST_REGION_NAME, Set.of(cloudNetwork));

        when(testNetworkDto.getSubnetIds()).thenReturn(DEFAULT_TEST_SUBNET_ID_SET);
        when(testNetworkDto.getEndpointGatewaySubnetIds()).thenReturn(Set.of());
        when(testNetworkDto.getAws()).thenReturn(awsParams);
        when(testEnvironmentDto.getCloudPlatform()).thenReturn(AWS_CLOUD_PLATFORM);
        when(cloudNetworks.getCloudNetworkResponses()).thenReturn(cloudNetworksFromProvider);

        Map<String, CloudSubnet> result = underTest.retrieveSubnetMetadata(testEnvironmentDto, testNetworkDto);
        Map<String, CloudSubnet> gatewayResult = underTest.retrieveEndpointGatewaySubnetMetadata(testEnvironmentDto, testNetworkDto);

        byte expectedAmountOfResultCloudSubnet = 1;
        byte expectedAmountOfResultCloudEndpointSubnet = 0;

        assertNotNull(result);
        assertEquals(expectedAmountOfResultCloudSubnet, result.size(), "The amount of result CloudSubnet(s) must be: " + expectedAmountOfResultCloudSubnet);
        assertEquals(TEST_SUBNET_ID, result.get(result.keySet().iterator().next()).getId());

        assertNotNull(gatewayResult);
        assertEquals(expectedAmountOfResultCloudEndpointSubnet, gatewayResult.size(),
                "The amount of result CloudSubnet(s) for the gateway endpoint must be: " + expectedAmountOfResultCloudEndpointSubnet);

        verify(platformParameterService, times(1)).getCloudNetworks(any());
        assertThat(result.get(TEST_SUBNET_ID).getDeploymentRestrictions()).containsExactlyElementsOf(DeploymentRestriction.ALL);
    }

    @Test
    @DisplayName("when retrieveSubnetMetadata has called with EnvironmentDto and the platform is AWS and endpoint subnets are provided that do not match the " +
            "environment subnets, then endpoint subnet information unique those subnets should be returned")
    void testRetrieveSubnetMetadataByEnvironmentDtoWhenPlatformIsAwsAndDifferentEndpointSubnetsAreProvided() {
        AwsParams awsParams = NetworkTestUtils.getAwsParams(DEFAULT_TEST_VPC_ID);

        CloudSubnet cloudSubnet = new CloudSubnet.Builder()
                .id(TEST_SUBNET_ID)
                .name("someSubnet")
                .build();
        CloudNetwork cloudNetwork = new CloudNetwork("someCloudNetwork", TEST_SUBNET_ID, Set.of(cloudSubnet),
                Collections.emptyMap());
        Map<String, Set<CloudNetwork>> cloudNetworksFromProvider = new LinkedHashMap<>();
        cloudNetworksFromProvider.put(DEFAULT_TEST_REGION_NAME, Set.of(cloudNetwork));

        CloudSubnet publicCloudSubnet = new CloudSubnet.Builder()
                .id(TEST_PUBLIC_SUBNET_ID)
                .name("someSubnet")
                .build();
        CloudNetwork publicCloudNetwork = new CloudNetwork("someCloudNetwork", TEST_PUBLIC_SUBNET_ID, Set.of(publicCloudSubnet),
                Collections.emptyMap());
        Map<String, Set<CloudNetwork>> publicCloudNetworksFromProvider = new LinkedHashMap<>();
        publicCloudNetworksFromProvider.put(DEFAULT_TEST_REGION_NAME, Set.of(publicCloudNetwork));

        when(testNetworkDto.getSubnetIds()).thenReturn(DEFAULT_TEST_SUBNET_ID_SET);
        when(testNetworkDto.getEndpointGatewaySubnetIds()).thenReturn(DEFAULT_TEST_PUBLIC_SUBNET_ID_SET);
        when(testNetworkDto.getAws()).thenReturn(awsParams);
        when(testEnvironmentDto.getCloudPlatform()).thenReturn(AWS_CLOUD_PLATFORM);
        when(cloudNetworks.getCloudNetworkResponses()).thenReturn(cloudNetworksFromProvider);

        Map<String, CloudSubnet> result = underTest.retrieveSubnetMetadata(testEnvironmentDto, testNetworkDto);

        when(cloudNetworks.getCloudNetworkResponses()).thenReturn(publicCloudNetworksFromProvider);

        Map<String, CloudSubnet> gatewayResult = underTest.retrieveEndpointGatewaySubnetMetadata(testEnvironmentDto, testNetworkDto);

        byte expectedAmountOfResultCloudSubnet = 1;

        assertNotNull(result);
        assertEquals(expectedAmountOfResultCloudSubnet, result.size(), "The amount of result CloudSubnet(s) must be: " + expectedAmountOfResultCloudSubnet);
        assertEquals(TEST_SUBNET_ID, result.get(result.keySet().iterator().next()).getId());

        assertNotNull(gatewayResult);
        assertEquals(expectedAmountOfResultCloudSubnet, gatewayResult.size(),
                "The amount of result CloudSubnet(s) for the gateway endpoint must be: " + expectedAmountOfResultCloudSubnet);
        assertEquals(TEST_PUBLIC_SUBNET_ID, gatewayResult.get(gatewayResult.keySet().iterator().next()).getId());

        verify(platformParameterService, times(2)).getCloudNetworks(any());
        assertThat(result.get(TEST_SUBNET_ID).getDeploymentRestrictions())
                .containsExactlyElementsOf(DeploymentRestriction.NON_ENDPOINT_ACCESS_GATEWAYS);
        assertThat(gatewayResult.get(TEST_PUBLIC_SUBNET_ID).getDeploymentRestrictions())
                .containsExactlyElementsOf(DeploymentRestriction.ENDPOINT_ACCESS_GATEWAYS);
    }

    @Test
    @DisplayName("when retrieveSubnetMetadata has called with EnvironmentDto and the platform is AZURE, then we should fetch the cloud networks from the " +
            "provider")
    void testRetrieveSubnetMetadataByEnvironmentDtoWhenPlatformIsAzureThenWeShouldFetchTheCloudNetworksFromProvider() {
        AzureParams azureParams = NetworkTestUtils.getAzureParams();

        CloudSubnet cloudSubnet = new CloudSubnet.Builder()
                .id(TEST_SUBNET_ID)
                .name("someSubnet")
                .build();
        CloudNetwork cloudNetwork = new CloudNetwork("someCloudNetwork", TEST_SUBNET_ID, Set.of(cloudSubnet),
                Collections.emptyMap());
        Map<String, Set<CloudNetwork>> cloudNetworksFromProvider = new LinkedHashMap<>();
        cloudNetworksFromProvider.put(DEFAULT_TEST_REGION_NAME, Set.of(cloudNetwork));
        CloudSubnet cloudSubnet2 = new CloudSubnet.Builder()
                .id(TEST_SUBNET_ID)
                .name("someSubnet")
                .build();
        CloudNetwork cloudNetwork2 = new CloudNetwork("someCloudNetwork", "someCloudNetwork", Set.of(cloudSubnet2),
                Collections.emptyMap());
        Map<String, Set<CloudNetwork>> cloudNetworksFromProvider2 = new LinkedHashMap<>();
        cloudNetworksFromProvider2.put(DEFAULT_TEST_REGION_NAME, Set.of(cloudNetwork2));

        when(testNetworkDto.getSubnetIds()).thenReturn(DEFAULT_TEST_SUBNET_ID_SET);
        when(testNetworkDto.getEndpointGatewaySubnetIds()).thenReturn(DEFAULT_TEST_SUBNET_ID_SET);
        when(testNetworkDto.getAzure()).thenReturn(azureParams);
        when(testEnvironmentDto.getCloudPlatform()).thenReturn(AZURE_CLOUD_PLATFORM);
        when(cloudNetworks.getCloudNetworkResponses()).thenReturn(cloudNetworksFromProvider);

        Map<String, CloudSubnet> result = underTest.retrieveSubnetMetadata(testEnvironmentDto, testNetworkDto);

        when(cloudNetworks.getCloudNetworkResponses()).thenReturn(cloudNetworksFromProvider2);
        Map<String, CloudSubnet> gatewayResult = underTest.retrieveEndpointGatewaySubnetMetadata(testEnvironmentDto, testNetworkDto);

        byte expectedAmountOfResultCloudSubnet = 1;

        assertNotNull(result);
        assertEquals(expectedAmountOfResultCloudSubnet, result.size(), "The amount of result CloudSubnet(s) must be: " + expectedAmountOfResultCloudSubnet);
        assertEquals(TEST_SUBNET_ID, result.get(result.keySet().iterator().next()).getId());

        assertNotNull(gatewayResult);
        assertEquals(expectedAmountOfResultCloudSubnet, gatewayResult.size(),
                "The amount of result CloudSubnet(s) for the gateway endpoint must be: " + expectedAmountOfResultCloudSubnet);
        assertEquals(TEST_SUBNET_ID, gatewayResult.get(gatewayResult.keySet().iterator().next()).getId());

        verify(platformParameterService, times(2)).getCloudNetworks(any());
        assertThat(result.get(TEST_SUBNET_ID).getDeploymentRestrictions())
                .containsExactlyElementsOf(DeploymentRestriction.ALL);
        assertThat(gatewayResult.get(TEST_SUBNET_ID).getDeploymentRestrictions())
                .containsExactlyElementsOf(DeploymentRestriction.ENDPOINT_ACCESS_GATEWAYS);
    }

    @Test
    @DisplayName("when retrieveSubnetMetadata has called with EnvironmentDto and the platform is Azure and no endpoint subnets are provided " +
            "then an empty endpoint map should return")
    void testRetrieveSubnetMetadataByEnvironmentDtoWhenPlatformIsAzureAndNoEndpointSubnetsAreProvided() {
        AzureParams azureParams = NetworkTestUtils.getAzureParams();

        CloudSubnet cloudSubnet = new CloudSubnet.Builder()
                .id(TEST_SUBNET_ID)
                .name("someSubnet")
                .build();
        CloudNetwork cloudNetwork = new CloudNetwork("someCloudNetwork", TEST_SUBNET_ID, Set.of(cloudSubnet),
                Collections.emptyMap());
        Map<String, Set<CloudNetwork>> cloudNetworksFromProvider = new LinkedHashMap<>();
        cloudNetworksFromProvider.put(DEFAULT_TEST_REGION_NAME, Set.of(cloudNetwork));

        when(testNetworkDto.getSubnetIds()).thenReturn(DEFAULT_TEST_SUBNET_ID_SET);
        when(testNetworkDto.getEndpointGatewaySubnetIds()).thenReturn(Set.of());
        when(testNetworkDto.getAzure()).thenReturn(azureParams);
        when(testEnvironmentDto.getCloudPlatform()).thenReturn(AZURE_CLOUD_PLATFORM);
        when(cloudNetworks.getCloudNetworkResponses()).thenReturn(cloudNetworksFromProvider);

        Map<String, CloudSubnet> result = underTest.retrieveSubnetMetadata(testEnvironmentDto, testNetworkDto);
        Map<String, CloudSubnet> gatewayResult = underTest.retrieveEndpointGatewaySubnetMetadata(testEnvironmentDto, testNetworkDto);

        byte expectedAmountOfResultCloudSubnet = 1;
        byte expectedAmountOfResultCloudEndpointSubnet = 0;

        assertNotNull(result);
        assertEquals(expectedAmountOfResultCloudSubnet, result.size(), "The amount of result CloudSubnet(s) must be: " + expectedAmountOfResultCloudSubnet);
        assertEquals(TEST_SUBNET_ID, result.get(result.keySet().iterator().next()).getId());

        assertNotNull(gatewayResult);
        assertEquals(expectedAmountOfResultCloudEndpointSubnet, gatewayResult.size(),
                "The amount of result CloudSubnet(s) for the gateway endpoint must be: " + expectedAmountOfResultCloudEndpointSubnet);

        verify(platformParameterService, times(1)).getCloudNetworks(any());
        assertThat(result.get(TEST_SUBNET_ID).getDeploymentRestrictions())
                .containsExactlyElementsOf(DeploymentRestriction.ALL);
    }

    @Test
    @DisplayName("when retrieveSubnetMetadata has called with EnvironmentDto and the platform is Azure and endpoint subnets are provided that " +
            "do not match the environment subnets, then endpoint subnet information unique those subnets should be returned")
    void testRetrieveSubnetMetadataByEnvironmentDtoWhenPlatformIsAzureAndDifferentEndpointSubnetsAreProvided() {
        AzureParams azureParams = NetworkTestUtils.getAzureParams();

        CloudSubnet cloudSubnet = new CloudSubnet.Builder()
                .id(TEST_SUBNET_ID)
                .name("someSubnet")
                .build();
        CloudNetwork cloudNetwork = new CloudNetwork("someCloudNetwork", TEST_SUBNET_ID, Set.of(cloudSubnet),
                Collections.emptyMap());
        Map<String, Set<CloudNetwork>> cloudNetworksFromProvider = new LinkedHashMap<>();
        cloudNetworksFromProvider.put(DEFAULT_TEST_REGION_NAME, Set.of(cloudNetwork));

        CloudSubnet publicCloudSubnet = new CloudSubnet.Builder()
                .id(TEST_PUBLIC_SUBNET_ID)
                .name("someSubnet")
                .build();
        CloudNetwork publicCloudNetwork = new CloudNetwork("someCloudNetwork", TEST_PUBLIC_SUBNET_ID, Set.of(publicCloudSubnet),
                Collections.emptyMap());
        Map<String, Set<CloudNetwork>> publicCloudNetworksFromProvider = new LinkedHashMap<>();
        publicCloudNetworksFromProvider.put(DEFAULT_TEST_REGION_NAME, Set.of(publicCloudNetwork));

        when(testNetworkDto.getSubnetIds()).thenReturn(DEFAULT_TEST_SUBNET_ID_SET);
        when(testNetworkDto.getEndpointGatewaySubnetIds()).thenReturn(DEFAULT_TEST_PUBLIC_SUBNET_ID_SET);
        when(testNetworkDto.getAzure()).thenReturn(azureParams);
        when(testEnvironmentDto.getCloudPlatform()).thenReturn(AZURE_CLOUD_PLATFORM);
        when(cloudNetworks.getCloudNetworkResponses()).thenReturn(cloudNetworksFromProvider);

        Map<String, CloudSubnet> result = underTest.retrieveSubnetMetadata(testEnvironmentDto, testNetworkDto);

        when(cloudNetworks.getCloudNetworkResponses()).thenReturn(publicCloudNetworksFromProvider);

        Map<String, CloudSubnet> gatewayResult = underTest.retrieveEndpointGatewaySubnetMetadata(testEnvironmentDto, testNetworkDto);

        byte expectedAmountOfResultCloudSubnet = 1;

        assertNotNull(result);
        assertEquals(expectedAmountOfResultCloudSubnet, result.size(), "The amount of result CloudSubnet(s) must be: " + expectedAmountOfResultCloudSubnet);
        assertEquals(TEST_SUBNET_ID, result.get(result.keySet().iterator().next()).getId());

        assertNotNull(gatewayResult);
        assertEquals(expectedAmountOfResultCloudSubnet, gatewayResult.size(),
                "The amount of result CloudSubnet(s) for the gateway endpoint must be: " + expectedAmountOfResultCloudSubnet);
        assertEquals(TEST_PUBLIC_SUBNET_ID, gatewayResult.get(gatewayResult.keySet().iterator().next()).getId());

        verify(platformParameterService, times(2)).getCloudNetworks(any());
        assertThat(result.get(TEST_SUBNET_ID).getDeploymentRestrictions())
                .containsExactlyElementsOf(DeploymentRestriction.NON_ENDPOINT_ACCESS_GATEWAYS);
        assertThat(gatewayResult.get(TEST_PUBLIC_SUBNET_ID).getDeploymentRestrictions())
                .containsExactlyElementsOf(DeploymentRestriction.ENDPOINT_ACCESS_GATEWAYS);
    }

    @Test
    @DisplayName("when retrieveSubnetMetadata has called with EnvironmentDto and the platform is neither AWS or AZURE then the result will be combined " +
            "from the network ids and no need for fetching anything from the provider")
    void testRetrieveSubnetMetadataByEnvironmentDtoWhenPlatformIsNeitherAzureOrAwsThenNoNeedForFetchingAnythingFromProvider() {
        when(testNetworkDto.getSubnetIds()).thenReturn(DEFAULT_TEST_SUBNET_ID_SET);
        when(testNetworkDto.getEndpointGatewaySubnetIds()).thenReturn(DEFAULT_TEST_PUBLIC_SUBNET_ID_SET);

        Map<String, CloudSubnet> result = underTest.retrieveSubnetMetadata(testEnvironmentDto, testNetworkDto);
        Map<String, CloudSubnet> gatewayResult = underTest.retrieveEndpointGatewaySubnetMetadata(testEnvironmentDto, testNetworkDto);

        byte expectedAmountOfResultCloudSubnet = 1;

        assertNotNull(result);
        assertEquals(expectedAmountOfResultCloudSubnet, result.size(), "The amount of result CloudSubnet(s) must be: " + expectedAmountOfResultCloudSubnet);
        assertEquals(TEST_SUBNET_ID, result.get(result.keySet().iterator().next()).getId());

        assertNotNull(gatewayResult);
        assertEquals(expectedAmountOfResultCloudSubnet, gatewayResult.size(),
                "The amount of result CloudSubnet(s) for the gateway endpoint must be: " + expectedAmountOfResultCloudSubnet);
        assertEquals(TEST_PUBLIC_SUBNET_ID, gatewayResult.get(gatewayResult.keySet().iterator().next()).getId());

        verify(platformParameterService, times(0)).getCloudNetworks(any());
    }

    @Test
    @DisplayName("when retrieveSubnetMetadata has called with Environment and empty subnetIds, then empty map should return")
    void testRetrieveSubnetMetadataByEnvironmentWhenNetworkIsNotNullButSubnetIdSetIsEmptyThenEmptyMapReturns() {
        when(testNetworkDto.getSubnetIds()).thenReturn(Collections.emptySet());
        when(testNetworkDto.getEndpointGatewaySubnetIds()).thenReturn(Collections.emptySet());
        Map<String, CloudSubnet> result = underTest.retrieveSubnetMetadata(testEnvironment, testNetworkDto);
        Map<String, CloudSubnet> gatewayResult = underTest.retrieveEndpointGatewaySubnetMetadata(testEnvironment, testNetworkDto);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertNotNull(gatewayResult);
        assertTrue(gatewayResult.isEmpty());
    }

    @Test
    @DisplayName("when retrieveSubnetMetadata has called with Environment and with a null NetworkDto then empty map should return")
    void retrieveSubnetMetadataByEnvironmentWhenNetworkIsNullThenEmptyMapReturns() {
        Map<String, CloudSubnet> result = underTest.retrieveSubnetMetadata(testEnvironment, null);
        Map<String, CloudSubnet> gatewayResult = underTest.retrieveEndpointGatewaySubnetMetadata(testEnvironment, null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertNotNull(gatewayResult);
        assertTrue(gatewayResult.isEmpty());
    }

    @Test
    @DisplayName("when retrieveSubnetMetadata has called with Environment and the platform is AWS then we should fetch the cloud networks from the provider")
    void testRetrieveSubnetMetadataByEnvironmentWhenPlatformIsAwsThenWeShouldFetchTheCloudNetworksFromProvider() {
        AwsParams awsParams = NetworkTestUtils.getAwsParams(DEFAULT_TEST_VPC_ID);

        CloudSubnet cloudSubnet = new CloudSubnet.Builder()
                .id(TEST_SUBNET_ID)
                .name("someSubnet")
                .build();
        CloudNetwork cloudNetwork = new CloudNetwork("someCloudNetwork", TEST_SUBNET_ID, Set.of(cloudSubnet),
                Collections.emptyMap());
        Map<String, Set<CloudNetwork>> cloudNetworksFromProvider = new LinkedHashMap<>();
        cloudNetworksFromProvider.put(DEFAULT_TEST_REGION_NAME, Set.of(cloudNetwork));
        CloudSubnet cloudSubnet2 = new CloudSubnet.Builder()
                .id(TEST_SUBNET_ID)
                .name("someSubnet")
                .build();
        CloudNetwork cloudNetwork2 = new CloudNetwork("someCloudNetwork", "someCloudNetwork", Set.of(cloudSubnet2),
                Collections.emptyMap());
        Map<String, Set<CloudNetwork>> cloudNetworksFromProvider2 = new LinkedHashMap<>();
        cloudNetworksFromProvider2.put(DEFAULT_TEST_REGION_NAME, Set.of(cloudNetwork2));

        when(testNetworkDto.getSubnetIds()).thenReturn(DEFAULT_TEST_SUBNET_ID_SET);
        when(testNetworkDto.getEndpointGatewaySubnetIds()).thenReturn(DEFAULT_TEST_SUBNET_ID_SET);
        when(testNetworkDto.getAws()).thenReturn(awsParams);
        when(testEnvironment.getCloudPlatform()).thenReturn(AWS_CLOUD_PLATFORM);
        when(cloudNetworks.getCloudNetworkResponses()).thenReturn(cloudNetworksFromProvider);
        Map<String, CloudSubnet> result = underTest.retrieveSubnetMetadata(testEnvironment, testNetworkDto);

        when(cloudNetworks.getCloudNetworkResponses()).thenReturn(cloudNetworksFromProvider2);
        Map<String, CloudSubnet> gatewayResult = underTest.retrieveEndpointGatewaySubnetMetadata(testEnvironment, testNetworkDto);

        byte expectedAmountOfResultCloudSubnet = 1;

        assertNotNull(result);
        assertEquals(expectedAmountOfResultCloudSubnet, result.size(), "The amount of result CloudSubnet(s) must be: " + expectedAmountOfResultCloudSubnet);
        assertEquals(TEST_SUBNET_ID, result.get(result.keySet().iterator().next()).getId());

        assertNotNull(gatewayResult);
        assertEquals(expectedAmountOfResultCloudSubnet, gatewayResult.size(),
                "The amount of result CloudSubnet(s) for the gateway endpoint must be: " + expectedAmountOfResultCloudSubnet);
        assertEquals(TEST_SUBNET_ID, gatewayResult.get(gatewayResult.keySet().iterator().next()).getId());

        verify(platformParameterService, times(2)).getCloudNetworks(any());
        assertThat(result.get(TEST_SUBNET_ID).getDeploymentRestrictions())
                .containsExactlyElementsOf(DeploymentRestriction.ALL);
        assertThat(gatewayResult.get(TEST_SUBNET_ID).getDeploymentRestrictions())
                .containsExactlyElementsOf(DeploymentRestriction.ENDPOINT_ACCESS_GATEWAYS);
    }

    @Test
    @DisplayName("when retrieveSubnetMetadata has called with Environment and the platform is AZURE, then we should fetch the cloud networks from the " +
            "provider")
    void testRetrieveSubnetMetadataByEnvironmentWhenPlatformIsAzureThenWeShouldFetchTheCloudNetworksFromProvider() {
        AzureParams azureParams = NetworkTestUtils.getAzureParams();

        CloudSubnet cloudSubnet = new CloudSubnet.Builder()
                .id(TEST_SUBNET_ID)
                .name("someSubnet")
                .build();
        CloudNetwork cloudNetwork = new CloudNetwork("someCloudNetwork", TEST_SUBNET_ID, Set.of(cloudSubnet),
                Collections.emptyMap());
        Map<String, Set<CloudNetwork>> cloudNetworksFromProvider = new LinkedHashMap<>();
        cloudNetworksFromProvider.put(DEFAULT_TEST_REGION_NAME, Set.of(cloudNetwork));
        CloudSubnet cloudSubnet2 = new CloudSubnet.Builder()
                .id(TEST_SUBNET_ID)
                .name("someSubnet")
                .build();
        CloudNetwork cloudNetwork2 = new CloudNetwork("someCloudNetwork", "someCloudNetwork", Set.of(cloudSubnet2),
                Collections.emptyMap());
        Map<String, Set<CloudNetwork>> cloudNetworksFromProvider2 = new LinkedHashMap<>();
        cloudNetworksFromProvider2.put(DEFAULT_TEST_REGION_NAME, Set.of(cloudNetwork2));

        when(testNetworkDto.getSubnetIds()).thenReturn(DEFAULT_TEST_SUBNET_ID_SET);
        when(testNetworkDto.getEndpointGatewaySubnetIds()).thenReturn(DEFAULT_TEST_SUBNET_ID_SET);
        when(testNetworkDto.getAzure()).thenReturn(azureParams);
        when(testEnvironment.getCloudPlatform()).thenReturn(AZURE_CLOUD_PLATFORM);
        when(cloudNetworks.getCloudNetworkResponses()).thenReturn(cloudNetworksFromProvider);
        Map<String, CloudSubnet> result = underTest.retrieveSubnetMetadata(testEnvironment, testNetworkDto);

        when(cloudNetworks.getCloudNetworkResponses()).thenReturn(cloudNetworksFromProvider2);
        Map<String, CloudSubnet> gatewayResult = underTest.retrieveEndpointGatewaySubnetMetadata(testEnvironment, testNetworkDto);

        byte expectedAmountOfResultCloudSubnet = 1;

        assertNotNull(result);
        assertEquals(expectedAmountOfResultCloudSubnet, result.size(), "The amount of result CloudSubnet(s) must be: " + expectedAmountOfResultCloudSubnet);
        assertEquals(TEST_SUBNET_ID, result.get(result.keySet().iterator().next()).getId());

        assertNotNull(gatewayResult);
        assertEquals(expectedAmountOfResultCloudSubnet, gatewayResult.size(),
                "The amount of result CloudSubnet(s) for the gateway endpoint must be: " + expectedAmountOfResultCloudSubnet);
        assertEquals(TEST_SUBNET_ID, gatewayResult.get(gatewayResult.keySet().iterator().next()).getId());

        verify(platformParameterService, times(2)).getCloudNetworks(any());
        assertThat(result.get(TEST_SUBNET_ID).getDeploymentRestrictions())
                .containsExactlyElementsOf(DeploymentRestriction.ALL);
        assertThat(gatewayResult.get(TEST_SUBNET_ID).getDeploymentRestrictions())
                .containsExactlyElementsOf(DeploymentRestriction.ENDPOINT_ACCESS_GATEWAYS);
    }

    @Test
    @DisplayName("when retrieveSubnetMetadata has called with Environment and the platform is neither AWS or AZURE then the result will be combined " +
            "from the network ids and no need for fetching anything from the provider")
    void testRetrieveSubnetMetadataByEnvironmentWhenPlatformIsNeitherAzureOrAwsThenNoNeedForFetchingAnythingFromProvider() {
        when(testNetworkDto.getSubnetIds()).thenReturn(DEFAULT_TEST_SUBNET_ID_SET);
        when(testNetworkDto.getEndpointGatewaySubnetIds()).thenReturn(DEFAULT_TEST_SUBNET_ID_SET);

        Map<String, CloudSubnet> result = underTest.retrieveSubnetMetadata(testEnvironment, testNetworkDto);
        Map<String, CloudSubnet> gatewayResult = underTest.retrieveEndpointGatewaySubnetMetadata(testEnvironment, testNetworkDto);

        byte expectedAmountOfResultCloudSubnet = 1;

        assertNotNull(result);
        assertEquals(expectedAmountOfResultCloudSubnet, result.size(), "The amount of result CloudSubnet(s) must be: " + expectedAmountOfResultCloudSubnet);
        assertEquals(TEST_SUBNET_ID, result.get(result.keySet().iterator().next()).getId());

        assertNotNull(gatewayResult);
        assertEquals(expectedAmountOfResultCloudSubnet, gatewayResult.size(),
                "The amount of result CloudSubnet(s) for the gateway endpoint must be: " + expectedAmountOfResultCloudSubnet);
        assertEquals(TEST_SUBNET_ID, gatewayResult.get(gatewayResult.keySet().iterator().next()).getId());

        verify(platformParameterService, times(0)).getCloudNetworks(any());
    }

    @Test
    @DisplayName("when retrieveSubnetMetadata has called with EnvironmentDto and the platform is GCP, then we should fetch the cloud networks from the " +
            "provider with the dedicated GCP related filters in place")
    void testRetrieveSubnetMetadataByEnvironmentDtoWhenPlatformIsGcpThenWeShouldFetchTheCloudNetworksFromProviderWithSpecificFilters() {
        Set<String> availabilityZones = Set.of("gcp-region1-zone1");
        String networkId = "networkid";
        String sharedProjectId = "sharedProjectId";
        Boolean noPublicIp = Boolean.TRUE;
        GcpParams gcpParams = GcpParams.builder()
                .withAvailabilityZones(availabilityZones)
                .withNetworkId(networkId)
                .withSharedProjectId(sharedProjectId)
                .withNoPublicIp(noPublicIp)
                .build();

        String testSubnetName = "someSubnet";
        CloudSubnet cloudSubnet = new CloudSubnet.Builder()
                .id(TEST_SUBNET_ID)
                .name(testSubnetName)
                .build();
        CloudNetwork cloudNetwork = new CloudNetwork("someCloudNetwork", TEST_SUBNET_ID, Set.of(cloudSubnet),
                Collections.emptyMap());
        Map<String, Set<CloudNetwork>> cloudNetworksFromProvider = new LinkedHashMap<>();
        cloudNetworksFromProvider.put(DEFAULT_TEST_REGION_NAME, Set.of(cloudNetwork));
        CloudSubnet cloudSubnet2 = new CloudSubnet.Builder()
                .id(TEST_SUBNET_ID)
                .name(testSubnetName)
                .build();
        CloudNetwork cloudNetwork2 = new CloudNetwork("someCloudNetwork", "someCloudNetwork", Set.of(cloudSubnet2),
                Collections.emptyMap());
        Map<String, Set<CloudNetwork>> cloudNetworksFromProvider2 = new LinkedHashMap<>();
        cloudNetworksFromProvider2.put(DEFAULT_TEST_REGION_NAME, Set.of(cloudNetwork2));

        when(testNetworkDto.getSubnetIds()).thenReturn(DEFAULT_TEST_SUBNET_ID_SET);
        when(testNetworkDto.getEndpointGatewaySubnetIds()).thenReturn(DEFAULT_TEST_SUBNET_ID_SET);
        when(testNetworkDto.getGcp()).thenReturn(gcpParams);
        when(testEnvironmentDto.getCloudPlatform()).thenReturn(CloudPlatform.GCP.name());
        when(testEnvironmentDto.getSecurityAccess().getCidr()).thenReturn("10.0.0.0/0");
        when(cloudNetworks.getCloudNetworkResponses()).thenReturn(cloudNetworksFromProvider);

        Map<String, CloudSubnet> result = underTest.retrieveSubnetMetadata(testEnvironmentDto, testNetworkDto);

        when(cloudNetworks.getCloudNetworkResponses()).thenReturn(cloudNetworksFromProvider2);
        Map<String, CloudSubnet> gatewayResult = underTest.retrieveEndpointGatewaySubnetMetadata(testEnvironmentDto, testNetworkDto);

        byte expectedAmountOfResultCloudSubnet = 1;

        assertNotNull(result);
        assertEquals(expectedAmountOfResultCloudSubnet, result.size(), "The amount of result CloudSubnet(s) must be: " + expectedAmountOfResultCloudSubnet);
        assertEquals(TEST_SUBNET_ID, result.get(result.keySet().iterator().next()).getId());

        assertNotNull(gatewayResult);
        assertEquals(expectedAmountOfResultCloudSubnet, gatewayResult.size(),
                "The amount of result CloudSubnet(s) for the gateway endpoint must be: " + expectedAmountOfResultCloudSubnet);
        assertEquals(TEST_SUBNET_ID, gatewayResult.get(gatewayResult.keySet().iterator().next()).getId());

        verify(platformParameterService, times(2)).getCloudNetworks(any());
        assertThat(result.get(testSubnetName).getDeploymentRestrictions())
                .containsExactlyElementsOf(DeploymentRestriction.ALL);
        assertThat(gatewayResult.get(testSubnetName).getDeploymentRestrictions())
                .containsExactlyElementsOf(DeploymentRestriction.ENDPOINT_ACCESS_GATEWAYS);

        //verify GCP related filters in place via argument captor
        ArgumentCaptor<PlatformResourceRequest> argumentCaptor = ArgumentCaptor.forClass(PlatformResourceRequest.class);
        verify(platformParameterService, times(2)).getCloudNetworks(argumentCaptor.capture());
        argumentCaptor.getAllValues()
                .forEach(capturedRequest -> {
                    Map<String, String> capturedFilters = capturedRequest.getFilters();
                    assertAll("Verify that GCP filters are in place",
                            () -> assertEquals(availabilityZones.stream().findFirst().get(), capturedFilters.get(GcpStackUtil.CUSTOM_AVAILABILITY_ZONE)),
                            () -> assertEquals(sharedProjectId, capturedFilters.get(GcpStackUtil.SHARED_PROJECT_ID)),
                            () -> assertEquals(String.valueOf(noPublicIp), capturedFilters.get(GcpStackUtil.NO_PUBLIC_IP)),
                            () -> assertEquals(Boolean.FALSE.toString(), capturedFilters.get(GcpStackUtil.NO_FIREWALL_RULES)),
                            () -> assertEquals(TEST_SUBNET_ID, capturedFilters.get(NetworkConstants.SUBNET_IDS)),
                            () -> assertEquals(networkId, capturedFilters.get(GcpStackUtil.NETWORK_ID))
                    );
                });
    }

    @Test
    @DisplayName("When retrieveCloudNetworks called with null Regions and getCloudNetworks call happens, it should return an empty Set.")
    void retrieveCloudNetworksNullRegions() {
        EnvironmentDto environmentDto = EnvironmentDto.builder().withRegions(null).withCredential(new Credential()).build();

        Set<String> result = underTest.retrieveCloudNetworks(environmentDto);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(platformParameterService, times(1)).getCloudNetworks(any());
    }

    @Test
    @DisplayName("When retrieveCloudNetworks called with empty Region and getCloudNetworks call happens, it should return an empty Set.")
    void retrieveCloudNetworksEmptyRegions() {
        EnvironmentDto environmentDto = EnvironmentDto.builder().withRegions(Collections.emptySet()).withCredential(new Credential()).build();

        Set<String> result = underTest.retrieveCloudNetworks(environmentDto);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(platformParameterService, times(1)).getCloudNetworks(any());
    }

    @Test
    @DisplayName("When retrieveCloudNetworks called with null Credential then getCloudNetworks call still should happen regardless its successfulness.")
    void retrieveCloudNetworksNullCredential() {
        EnvironmentDto environmentDto = EnvironmentDto.builder().withCredential(null).build();

        Set<String> result = underTest.retrieveCloudNetworks(environmentDto);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(platformParameterService, times(1)).getCloudNetworks(any());
    }

    @Test
    @DisplayName("When getCloudNetworks called inside retrieveCloudNetworks and its result does not contain a key which corresponds the given region, then"
            + " an empty Set should return.")
    void retrieveCloudNetworksNonMatchingGetCloudNetworksRegionResult() {
        EnvironmentDto environmentDto = EnvironmentDto.builder().withCredential(null).build();
        when(platformParameterService.getCloudNetworks(any())).thenReturn(new CloudNetworks(Collections.emptyMap()));

        Set<String> result = underTest.retrieveCloudNetworks(environmentDto);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(platformParameterService, times(1)).getCloudNetworks(any());
    }

    @Test
    @DisplayName("When getCloudNetworks called inside retrieveCloudNetworks and its result contain a key which corresponds the given region, then"
            + " a matching Set should return.")
    void retrieveCloudNetworksMatchingGetCloudNetworksRegionResult() {
        String regionName = "someRegion";
        Region region = new Region();
        region.setName(regionName);
        EnvironmentDto environmentDto = EnvironmentDto.builder().withCredential(null).withRegions(Set.of(region)).build();
        Set<CloudNetwork> networks = NetworkTestUtils.getCloudNetworks(1);
        CloudNetworks resultCloudNetworks = new CloudNetworks();
        resultCloudNetworks.setCloudNetworkResponses(Map.of(regionName, networks));
        when(platformParameterService.getCloudNetworks(any())).thenReturn(resultCloudNetworks);

        Set<String> result = underTest.retrieveCloudNetworks(environmentDto);

        assertNotNull(result);
        assertEquals(networks.size(), result.size());
        assertEquals(networks.stream().map(CloudNetwork::getName).toList().get(0), result.stream().findFirst().get());
        verify(platformParameterService, times(1)).getCloudNetworks(any());
    }

    private Map<String, Set<CloudNetwork>> cloudSubnetCreator() {
        Map<String, Set<CloudNetwork>> cloudNetwork = new LinkedHashMap<>(CloudNetworkServiceTest.AMOUNT_OF_SUBNETS);
        for (byte i = 0; i < CloudNetworkServiceTest.AMOUNT_OF_SUBNETS; i++) {
            cloudNetwork.put("response-" + i, NetworkTestUtils.getCloudNetworks(1));
        }
        return cloudNetwork;
    }

}
