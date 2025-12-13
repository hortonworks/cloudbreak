package com.sequenceiq.environment.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.NetworkConnector;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedCloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkDeletionRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkResourcesCreationRequest;
import com.sequenceiq.cloudbreak.cloud.network.NetworkCidr;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.LocationDto;
import com.sequenceiq.environment.network.dao.domain.AwsNetwork;
import com.sequenceiq.environment.network.dao.domain.AzureNetwork;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.environment.network.dto.AzureParams;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.network.service.NetworkCreationRequestFactory;
import com.sequenceiq.environment.network.v1.converter.AwsEnvironmentNetworkConverter;
import com.sequenceiq.environment.network.v1.converter.AzureEnvironmentNetworkConverter;
import com.sequenceiq.environment.network.v1.converter.EnvironmentNetworkConverter;
import com.sequenceiq.environment.parameter.dto.AzureParametersDto;
import com.sequenceiq.environment.parameter.dto.AzureResourceGroupDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto;
import com.sequenceiq.environment.parameter.dto.ResourceGroupUsagePattern;

@ExtendWith(MockitoExtension.class)
class EnvironmentNetworkServiceTest {

    private static final String CLOUD_PLATFORM = "AZURE";

    private static final String STACK_NAME = "stackName";

    private static final String NETWORK_ID = "networkId";

    private static final String USER_NAME = "name";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:accountId:user:" + USER_NAME;

    @Mock
    private CloudConnector cloudConnector;

    @Mock
    private NetworkConnector networkConnector;

    private final CloudPlatformConnectors cloudPlatformConnectors = mock(CloudPlatformConnectors.class);

    private final NetworkCreationRequestFactory networkCreationRequestFactory = mock(NetworkCreationRequestFactory.class);

    private final Map<CloudPlatform, EnvironmentNetworkConverter> environmentNetworkConverterMap = mock(Map.class);

    private final CredentialToCloudCredentialConverter credentialToCloudCredentialConverter = mock(CredentialToCloudCredentialConverter.class);

    @InjectMocks
    private final EnvironmentNetworkService underTest = new EnvironmentNetworkService(
            cloudPlatformConnectors,
            networkCreationRequestFactory,
            environmentNetworkConverterMap,
            credentialToCloudCredentialConverter
    );

    @BeforeEach
    void before() {
        when(cloudPlatformConnectors.get(any(CloudPlatformVariant.class))).thenReturn(cloudConnector);
    }

    @Test
    void testCreateNetworkShouldReturnWithANewNetwork() {
        EnvironmentDto environmentDto = EnvironmentDto.builder().withCloudPlatform(CLOUD_PLATFORM).withCreator(USER_CRN).build();
        BaseNetwork baseNetwork = new AwsNetwork();
        NetworkCreationRequest networkCreationRequest = new NetworkCreationRequest.Builder().build();
        CreatedCloudNetwork createdCloudNetwork = new CreatedCloudNetwork();
        AwsEnvironmentNetworkConverter networkConverter = mock(AwsEnvironmentNetworkConverter.class);

        when(cloudConnector.networkConnector()).thenReturn(networkConnector);
        when(networkCreationRequestFactory.create(environmentDto)).thenReturn(networkCreationRequest);
        when(networkConnector.createNetworkWithSubnets(networkCreationRequest)).thenReturn(createdCloudNetwork);
        when(environmentNetworkConverterMap.get(CloudPlatform.valueOf(CLOUD_PLATFORM))).thenReturn(networkConverter);
        when(networkConverter.setCreatedCloudNetwork(baseNetwork, createdCloudNetwork)).thenReturn(baseNetwork);

        BaseNetwork actual = underTest.createCloudNetwork(environmentDto, baseNetwork);

        verify(cloudConnector).networkConnector();
        verify(cloudPlatformConnectors).get(any(CloudPlatformVariant.class));
        verify(networkCreationRequestFactory).create(environmentDto);
        verify(networkConnector).createNetworkWithSubnets(networkCreationRequest);
        verify(environmentNetworkConverterMap).get(CloudPlatform.valueOf(CLOUD_PLATFORM));
        verify(networkConverter).setCreatedCloudNetwork(baseNetwork, createdCloudNetwork);
        assertEquals(baseNetwork, actual);
    }

    @Test
    void testCreateProviderSpecificNetworkResources() {
        EnvironmentDto environmentDto = EnvironmentDto.builder().withCloudPlatform(CLOUD_PLATFORM).withCreator(USER_CRN).build();
        NetworkResourcesCreationRequest networkResourcesCreationRequest
                = new NetworkResourcesCreationRequest.Builder().build();
        AzureEnvironmentNetworkConverter networkConverter = mock(AzureEnvironmentNetworkConverter.class);
        BaseNetwork baseNetwork = getNetwork();

        when(cloudConnector.networkConnector()).thenReturn(networkConnector);
        when(networkCreationRequestFactory.createProviderSpecificNetworkResources(environmentDto, baseNetwork))
                .thenReturn(networkResourcesCreationRequest);
        when(environmentNetworkConverterMap.get(CloudPlatform.valueOf(CLOUD_PLATFORM))).thenReturn(networkConverter);

        underTest.createProviderSpecificNetworkResources(environmentDto, baseNetwork);

        verify(cloudConnector).networkConnector();
        verify(cloudPlatformConnectors).get(any(CloudPlatformVariant.class));
        verify(networkCreationRequestFactory).createProviderSpecificNetworkResources(environmentDto, baseNetwork);
        verify(networkConnector).createProviderSpecificNetworkResources(networkResourcesCreationRequest);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testCreateNetworkIfUnableToObtainNetworkConnectorThenNetworkConnectorNotFoundExceptionComes() {
        EnvironmentDto environmentDto = EnvironmentDto.builder().withCloudPlatform(CLOUD_PLATFORM).build();
        CloudConnector cloudConnector = mock(CloudConnector.class);

        when(cloudPlatformConnectors.get(any(CloudPlatformVariant.class))).thenReturn(cloudConnector);
        when(cloudConnector.networkConnector()).thenReturn(null);

        assertThrows(NetworkConnectorNotFoundException.class, () -> underTest.createCloudNetwork(environmentDto, new AzureNetwork()));

        verify(cloudPlatformConnectors, times(1)).get(any());
    }

    @Test
    void testDeleteNetworkShouldDeleteTheNetwork() {
        CloudCredential cloudCredential = new CloudCredential("1", "asd", "account");
        EnvironmentDto environmentDto = createEnvironmentDto(null);

        when(cloudConnector.networkConnector()).thenReturn(networkConnector);
        when(credentialToCloudCredentialConverter.convert(environmentDto.getCredential())).thenReturn(cloudCredential);
        when(networkCreationRequestFactory.getStackName(any())).thenReturn(STACK_NAME);

        underTest.deleteNetwork(environmentDto);

        ArgumentCaptor<NetworkDeletionRequest> argumentCaptor = ArgumentCaptor.forClass(NetworkDeletionRequest.class);

        verify(networkConnector).deleteNetworkWithSubnets(argumentCaptor.capture());

        assertEquals(STACK_NAME, argumentCaptor.getValue().getStackName());
        assertEquals(cloudCredential, argumentCaptor.getValue().getCloudCredential());
        assertEquals(environmentDto.getLocation().getName(), argumentCaptor.getValue().getRegion());
        assertNull(argumentCaptor.getValue().getResourceGroup());
    }

    @Test
    void testDeleteNetworkShouldDeleteTheNetworkWithResourceGroupWhenUsagePatternMultiple() {
        CloudCredential cloudCredential = new CloudCredential("1", "credName", "account");
        EnvironmentDto environmentDto = createEnvironmentDto("resourceGroup");

        when(cloudConnector.networkConnector()).thenReturn(networkConnector);
        when(credentialToCloudCredentialConverter.convert(environmentDto.getCredential())).thenReturn(cloudCredential);
        when(networkCreationRequestFactory.getStackName(any())).thenReturn(STACK_NAME);

        underTest.deleteNetwork(environmentDto);

        ArgumentCaptor<NetworkDeletionRequest> argumentCaptor = ArgumentCaptor.forClass(NetworkDeletionRequest.class);

        verify(networkConnector).deleteNetworkWithSubnets(argumentCaptor.capture());

        assertEquals(STACK_NAME, argumentCaptor.getValue().getStackName());
        assertEquals(cloudCredential, argumentCaptor.getValue().getCloudCredential());
        assertEquals(environmentDto.getLocation().getName(), argumentCaptor.getValue().getRegion());
        assertEquals(environmentDto.getNetwork().getAzure().getResourceGroupName(), argumentCaptor.getValue().getResourceGroup());
        assertFalse(argumentCaptor.getValue().isSingleResourceGroup());
    }

    @ParameterizedTest
    @EnumSource(value = ResourceGroupUsagePattern.class, names = {"USE_SINGLE", "USE_SINGLE_WITH_DEDICATED_STORAGE_ACCOUNT"})
    void testDeleteNetworkShouldNotDeleteResourceGroupWhenUsagePatternSingle(ResourceGroupUsagePattern resourceGroupUsagePattern) {
        CloudCredential cloudCredential = new CloudCredential("1", "credName", "account");
        EnvironmentDto environmentDto = createEnvironmentDto("resourceGroup", "mySingleRg", resourceGroupUsagePattern);

        when(cloudConnector.networkConnector()).thenReturn(networkConnector);
        when(credentialToCloudCredentialConverter.convert(environmentDto.getCredential())).thenReturn(cloudCredential);
        when(networkCreationRequestFactory.getStackName(any())).thenReturn(STACK_NAME);

        underTest.deleteNetwork(environmentDto);

        ArgumentCaptor<NetworkDeletionRequest> argumentCaptor = ArgumentCaptor.forClass(NetworkDeletionRequest.class);

        verify(networkConnector).deleteNetworkWithSubnets(argumentCaptor.capture());

        assertEquals(STACK_NAME, argumentCaptor.getValue().getStackName());
        assertEquals(cloudCredential, argumentCaptor.getValue().getCloudCredential());
        assertEquals(environmentDto.getLocation().getName(), argumentCaptor.getValue().getRegion());
        assertEquals(environmentDto.getParameters().getAzureParametersDto().getAzureResourceGroupDto().getName(), argumentCaptor.getValue().getResourceGroup());
        assertTrue(argumentCaptor.getValue().isSingleResourceGroup());
    }

    @Test
    void testDeleteNetworkShouldNotThrowExceptionWhenNoNetworkConnectorForPlatform() {
        EnvironmentDto environmentDto = createEnvironmentDto(null);
        when(cloudConnector.networkConnector()).thenReturn(null);

        underTest.deleteNetwork(environmentDto);

        verify(networkConnector, times(0)).deleteNetworkWithSubnets(any());
    }

    @Test
    void testGetNetworkCidr() {
        Credential credential = mock(Credential.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        Network network = mock(Network.class);

        String networkCidr = "10.0.0.0/16";

        when(credentialToCloudCredentialConverter.convert(credential)).thenReturn(cloudCredential);
        when(cloudConnector.networkConnector()).thenReturn(networkConnector);
        when(networkConnector.getNetworkCidr(network, cloudCredential)).thenReturn(new NetworkCidr(networkCidr));

        NetworkCidr result = underTest.getNetworkCidr(network, "AWS", credential);

        assertEquals(networkCidr, result.getCidr());
    }

    @Test
    void testGetNetworkCidrWhenNetworkNull() {
        Credential credential = mock(Credential.class);

        NetworkCidr result = underTest.getNetworkCidr(null, "AWS", credential);

        assertNull(result);
    }

    @Test
    void testGetNetworkCidrWhenNetworkConnectorNull() {
        Credential credential = mock(Credential.class);
        Network network = mock(Network.class);

        when(cloudConnector.networkConnector()).thenReturn(null);

        NetworkConnectorNotFoundException exception = assertThrows(NetworkConnectorNotFoundException.class,
                () -> underTest.getNetworkCidr(network, "AWS", credential));
        assertEquals("No network connector for cloud platform: AWS", exception.getMessage());
    }

    private EnvironmentDto createEnvironmentDto(String resourceGroup) {
        return createEnvironmentDto(resourceGroup, null, ResourceGroupUsagePattern.USE_MULTIPLE);
    }

    private EnvironmentDto createEnvironmentDto(String resourceGroup, String singleRgName, ResourceGroupUsagePattern resourceGroupUsagePattern) {
        return EnvironmentDto.builder()
                .withCloudPlatform(CLOUD_PLATFORM)
                .withCredential(new Credential())
                .withLocationDto(LocationDto.builder().withName("us-west-1").build())
                .withNetwork(NetworkDto.builder()
                        .withName("net-1")
                        .withRegistrationType(RegistrationType.EXISTING)
                        .withAzure(AzureParams.builder()
                                .withResourceGroupName(resourceGroup)
                                .build())
                        .build())
                .withParameters(
                        ParametersDto.builder().withAzureParametersDto(
                                AzureParametersDto.builder().withAzureResourceGroupDto(
                                        AzureResourceGroupDto.builder()
                                                .withResourceGroupUsagePattern(resourceGroupUsagePattern)
                                                .withName(singleRgName)
                                                .build()
                                ).build()
                        ).build()
                )
                .build();
    }

    private BaseNetwork getNetwork() {
        AzureNetwork azureNetwork = new AzureNetwork();
        azureNetwork.setNetworkId(NETWORK_ID);
        return azureNetwork;
    }
}
