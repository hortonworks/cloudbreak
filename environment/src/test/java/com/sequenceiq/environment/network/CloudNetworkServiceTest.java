package com.sequenceiq.environment.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.cloud.model.CloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetworks;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.Region;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.validation.network.NetworkTestUtils;
import com.sequenceiq.environment.network.dto.AwsParams;
import com.sequenceiq.environment.network.dto.AzureParams;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.platformresource.PlatformParameterService;
import com.sequenceiq.environment.platformresource.PlatformResourceRequest;

class CloudNetworkServiceTest {

    private static final byte AMOUNT_OF_SUBNETS = 2;

    private static final Set<String> DEFAULT_TEST_SUBNET_ID_SET = Set.of("test-subnet-id");

    private static final String DEFAULT_TEST_VPC_ID = "test-vpc-id";

    private static final String AWS_CLOUD_PLATFORM = "AWS";

    private static final String AZURE_CLOUD_PLATFORM = "AZURE";

    private static final String DEFAULT_TEST_REGION_NAME = "somewhere-over-the-rainbo";

    @Mock
    private PlatformParameterService platformParameterService;

    @Mock
    private CloudNetworks cloudNetworks;

    @Mock
    private EnvironmentDto testEnvironmentDto;

    @Mock
    private NetworkDto testNetworkDto;

    @Mock
    private Environment testEnvironment;

    private CloudNetworkService underTest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        underTest = new CloudNetworkService(platformParameterService);

        Region testRegion = new Region();
        testRegion.setName(DEFAULT_TEST_REGION_NAME);
        testRegion.setDisplayName(DEFAULT_TEST_REGION_NAME);

        when(cloudNetworks.getCloudNetworkResponses()).thenReturn(cloudSubnetCreator(AMOUNT_OF_SUBNETS));
        when(platformParameterService.getCloudNetworks(any(PlatformResourceRequest.class))).thenReturn(cloudNetworks);
        when(testEnvironmentDto.getRegions()).thenReturn(Set.of(testRegion));
        when(testEnvironment.getRegionSet()).thenReturn(Set.of(testRegion));
    }

    @Test
    @DisplayName("when retrieveSubnetMetadata has called with EnvironmentDto and with a null NetworkDto then empty map should return")
    void testRetrieveSubnetMetadataByEnvironmentDtoWhenNetworkIsNullThenEmptyMapReturns() {
        Map<String, CloudSubnet> result = underTest.retrieveSubnetMetadata(testEnvironmentDto, null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("when retrieveSubnetMetadata has called with EnvironmentDto and empty subnetIds, then empty map should return")
    void testRetrieveSubnetMetadataByEnvironmentDtoWhenNetworkIsNotNullButSubnetIdSetIsEmptyThenEmptyMapReturns() {
        when(testNetworkDto.getSubnetIds()).thenReturn(Collections.emptySet());
        Map<String, CloudSubnet> result = underTest.retrieveSubnetMetadata(testEnvironmentDto, testNetworkDto);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("when retrieveSubnetMetadata has called with EnvironmentDto and the platform is AWS then we should fetch the cloud networks from the provider")
    void testRetrieveSubnetMetadataByEnvironmentDtoWhenPlatformIsAwsThenWeShouldFetchTheCloudNetworksFromProvider() {
        AwsParams awsParams = NetworkTestUtils.getAwsParams(DEFAULT_TEST_VPC_ID);

        CloudSubnet cloudSubnet = new CloudSubnet(DEFAULT_TEST_SUBNET_ID_SET.iterator().next(), "someSubnet");
        CloudNetwork cloudNetwork = new CloudNetwork("someCloudNetwork", DEFAULT_TEST_SUBNET_ID_SET.iterator().next(), Set.of(cloudSubnet),
                Collections.emptyMap());
        Map<String, Set<CloudNetwork>> cloudNetworksFromProvider = new LinkedHashMap<>();
        cloudNetworksFromProvider.put(DEFAULT_TEST_REGION_NAME, Set.of(cloudNetwork));

        when(testNetworkDto.getSubnetIds()).thenReturn(DEFAULT_TEST_SUBNET_ID_SET);
        when(testNetworkDto.getAws()).thenReturn(awsParams);
        when(testEnvironmentDto.getCloudPlatform()).thenReturn(AWS_CLOUD_PLATFORM);
        when(cloudNetworks.getCloudNetworkResponses()).thenReturn(cloudNetworksFromProvider);

        Map<String, CloudSubnet> result = underTest.retrieveSubnetMetadata(testEnvironmentDto, testNetworkDto);

        byte expectedAmountOfResultCloudSubnet = 1;
        assertNotNull(result);
        assertEquals(expectedAmountOfResultCloudSubnet, result.size(), "The amount of result CloudSubnet(s) must be: " + expectedAmountOfResultCloudSubnet);
        assertEquals(DEFAULT_TEST_SUBNET_ID_SET.iterator().next(), result.get(result.keySet().iterator().next()).getId());

        verify(platformParameterService, times(1)).getCloudNetworks(any());
    }

    @Test
    @DisplayName("when retrieveSubnetMetadata has called with EnvironmentDto and the platform is AZURE, then we should fetch the cloud networks from the " +
            "provider")
    void testRetrieveSubnetMetadataByEnvironmentDtoWhenPlatformIsAzureThenWeShouldFetchTheCloudNetworksFromProvider() {
        AzureParams azureParams = NetworkTestUtils.getAzureParams();

        CloudSubnet cloudSubnet = new CloudSubnet(DEFAULT_TEST_SUBNET_ID_SET.iterator().next(), "someSubnet");
        CloudNetwork cloudNetwork = new CloudNetwork("someCloudNetwork", DEFAULT_TEST_SUBNET_ID_SET.iterator().next(), Set.of(cloudSubnet),
                Collections.emptyMap());
        Map<String, Set<CloudNetwork>> cloudNetworksFromProvider = new LinkedHashMap<>();
        cloudNetworksFromProvider.put(DEFAULT_TEST_REGION_NAME, Set.of(cloudNetwork));

        when(testNetworkDto.getSubnetIds()).thenReturn(DEFAULT_TEST_SUBNET_ID_SET);
        when(testNetworkDto.getAzure()).thenReturn(azureParams);
        when(testEnvironmentDto.getCloudPlatform()).thenReturn(AZURE_CLOUD_PLATFORM);
        when(cloudNetworks.getCloudNetworkResponses()).thenReturn(cloudNetworksFromProvider);

        Map<String, CloudSubnet> result = underTest.retrieveSubnetMetadata(testEnvironmentDto, testNetworkDto);

        byte expectedAmountOfResultCloudSubnet = 1;

        assertNotNull(result);
        assertEquals(expectedAmountOfResultCloudSubnet, result.size(), "The amount of result CloudSubnet(s) must be: " + expectedAmountOfResultCloudSubnet);
        assertEquals(DEFAULT_TEST_SUBNET_ID_SET.iterator().next(), result.get(result.keySet().iterator().next()).getId());

        verify(platformParameterService, times(1)).getCloudNetworks(any());
    }

    @Test
    @DisplayName("when retrieveSubnetMetadata has called with EnvironmentDto and the platform is neither AWS or AZURE then the result will be combined " +
            "from the network ids and no need for fetching anything from the provider")
    void testRetrieveSubnetMetadataByEnvironmentDtoWhenPlatformIsNeitherAzureOrAwsThenNoNeedForFetchingAnythingFromProvider() {
        when(testNetworkDto.getSubnetIds()).thenReturn(DEFAULT_TEST_SUBNET_ID_SET);

        Map<String, CloudSubnet> result = underTest.retrieveSubnetMetadata(testEnvironmentDto, testNetworkDto);

        byte expectedAmountOfResultCloudSubnet = 1;

        assertNotNull(result);
        assertEquals(expectedAmountOfResultCloudSubnet, result.size(), "The amount of result CloudSubnet(s) must be: " + expectedAmountOfResultCloudSubnet);
        assertEquals(DEFAULT_TEST_SUBNET_ID_SET.iterator().next(), result.get(result.keySet().iterator().next()).getId());

        verify(platformParameterService, times(0)).getCloudNetworks(any());
    }

    @Test
    @DisplayName("when retrieveSubnetMetadata has called with Environment and empty subnetIds, then empty map should return")
    void testRetrieveSubnetMetadataByEnvironmentWhenNetworkIsNotNullButSubnetIdSetIsEmptyThenEmptyMapReturns() {
        when(testNetworkDto.getSubnetIds()).thenReturn(Collections.emptySet());
        Map<String, CloudSubnet> result = underTest.retrieveSubnetMetadata(testEnvironment, testNetworkDto);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("when retrieveSubnetMetadata has called with Environment and with a null NetworkDto then empty map should return")
    void retrieveSubnetMetadataByEnvironmentWhenNetworkIsNullThenEmptyMapReturns() {
        Map<String, CloudSubnet> result = underTest.retrieveSubnetMetadata(testEnvironment, null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("when retrieveSubnetMetadata has called with Environment and the platform is AWS then we should fetch the cloud networks from the provider")
    void testRetrieveSubnetMetadataByEnvironmentWhenPlatformIsAwsThenWeShouldFetchTheCloudNetworksFromProvider() {
        AwsParams awsParams = NetworkTestUtils.getAwsParams(DEFAULT_TEST_VPC_ID);

        CloudSubnet cloudSubnet = new CloudSubnet(DEFAULT_TEST_SUBNET_ID_SET.iterator().next(), "someSubnet");
        CloudNetwork cloudNetwork = new CloudNetwork("someCloudNetwork", DEFAULT_TEST_SUBNET_ID_SET.iterator().next(), Set.of(cloudSubnet),
                Collections.emptyMap());
        Map<String, Set<CloudNetwork>> cloudNetworksFromProvider = new LinkedHashMap<>();
        cloudNetworksFromProvider.put(DEFAULT_TEST_REGION_NAME, Set.of(cloudNetwork));

        when(testNetworkDto.getSubnetIds()).thenReturn(DEFAULT_TEST_SUBNET_ID_SET);
        when(testNetworkDto.getAws()).thenReturn(awsParams);
        when(testEnvironment.getCloudPlatform()).thenReturn(AWS_CLOUD_PLATFORM);
        when(cloudNetworks.getCloudNetworkResponses()).thenReturn(cloudNetworksFromProvider);

        Map<String, CloudSubnet> result = underTest.retrieveSubnetMetadata(testEnvironment, testNetworkDto);

        byte expectedAmountOfResultCloudSubnet = 1;
        assertNotNull(result);
        assertEquals(expectedAmountOfResultCloudSubnet, result.size(), "The amount of result CloudSubnet(s) must be: " + expectedAmountOfResultCloudSubnet);
        assertEquals(DEFAULT_TEST_SUBNET_ID_SET.iterator().next(), result.get(result.keySet().iterator().next()).getId());

        verify(platformParameterService, times(1)).getCloudNetworks(any());
    }

    @Test
    @DisplayName("when retrieveSubnetMetadata has called with Environment and the platform is AZURE, then we should fetch the cloud networks from the " +
            "provider")
    void testRetrieveSubnetMetadataByEnvironmentWhenPlatformIsAzureThenWeShouldFetchTheCloudNetworksFromProvider() {
        AzureParams azureParams = NetworkTestUtils.getAzureParams();

        CloudSubnet cloudSubnet = new CloudSubnet(DEFAULT_TEST_SUBNET_ID_SET.iterator().next(), "someSubnet");
        CloudNetwork cloudNetwork = new CloudNetwork("someCloudNetwork", DEFAULT_TEST_SUBNET_ID_SET.iterator().next(), Set.of(cloudSubnet),
                Collections.emptyMap());
        Map<String, Set<CloudNetwork>> cloudNetworksFromProvider = new LinkedHashMap<>();
        cloudNetworksFromProvider.put(DEFAULT_TEST_REGION_NAME, Set.of(cloudNetwork));

        when(testNetworkDto.getSubnetIds()).thenReturn(DEFAULT_TEST_SUBNET_ID_SET);
        when(testNetworkDto.getAzure()).thenReturn(azureParams);
        when(testEnvironment.getCloudPlatform()).thenReturn(AZURE_CLOUD_PLATFORM);
        when(cloudNetworks.getCloudNetworkResponses()).thenReturn(cloudNetworksFromProvider);

        Map<String, CloudSubnet> result = underTest.retrieveSubnetMetadata(testEnvironment, testNetworkDto);

        byte expectedAmountOfResultCloudSubnet = 1;
        assertNotNull(result);
        assertEquals(expectedAmountOfResultCloudSubnet, result.size(), "The amount of result CloudSubnet(s) must be: " + expectedAmountOfResultCloudSubnet);
        assertEquals(DEFAULT_TEST_SUBNET_ID_SET.iterator().next(), result.get(result.keySet().iterator().next()).getId());

        verify(platformParameterService, times(1)).getCloudNetworks(any());
    }

    @Test
    @DisplayName("when retrieveSubnetMetadata has called with Environment and the platform is neither AWS or AZURE then the result will be combined " +
            "from the network ids and no need for fetching anything from the provider")
    void testRetrieveSubnetMetadataByEnvironmentWhenPlatformIsNeitherAzureOrAwsThenNoNeedForFetchingAnythingFromProvider() {
        when(testNetworkDto.getSubnetIds()).thenReturn(DEFAULT_TEST_SUBNET_ID_SET);

        Map<String, CloudSubnet> result = underTest.retrieveSubnetMetadata(testEnvironment, testNetworkDto);

        byte expectedAmountOfResultCloudSubnet = 1;

        assertNotNull(result);
        assertEquals(expectedAmountOfResultCloudSubnet, result.size(), "The amount of result CloudSubnet(s) must be: " + expectedAmountOfResultCloudSubnet);
        assertEquals(DEFAULT_TEST_SUBNET_ID_SET.iterator().next(), result.get(result.keySet().iterator().next()).getId());

        verify(platformParameterService, times(0)).getCloudNetworks(any());
    }

    private Map<String, Set<CloudNetwork>> cloudSubnetCreator(byte amountToCreate) {
        Map<String, Set<CloudNetwork>> cloudNetwork = new LinkedHashMap<>(amountToCreate);
        for (byte i = 0; i < amountToCreate; i++) {
            cloudNetwork.put("response-" + i, NetworkTestUtils.getCloudNetworks(1));
        }
        return cloudNetwork;
    }

}