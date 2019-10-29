package com.sequenceiq.environment.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
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
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.LocationDto;
import com.sequenceiq.environment.network.dao.domain.AwsNetwork;
import com.sequenceiq.environment.network.dao.domain.AzureNetwork;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.network.dto.AzureParams;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.network.service.NetworkCreationRequestFactory;
import com.sequenceiq.environment.network.v1.converter.AwsEnvironmentNetworkConverter;
import com.sequenceiq.environment.network.v1.converter.EnvironmentNetworkConverter;

@ExtendWith(MockitoExtension.class)
class EnvironmentNetworkServiceTest {

    private static final String CLOUD_PLATFORM = "AZURE";

    private static final String STACK_NAME = "stackName";

    private static final String USER_NAME = "name";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:accountId:user:" + USER_NAME;

    @Mock
    private CloudConnector<Object> cloudConnector;

    @Mock
    private NetworkConnector networkConnector;

    private final CloudPlatformConnectors cloudPlatformConnectors = Mockito.mock(CloudPlatformConnectors.class);

    private final NetworkCreationRequestFactory networkCreationRequestFactory = Mockito.mock(NetworkCreationRequestFactory.class);

    private Map<CloudPlatform, EnvironmentNetworkConverter> environmentNetworkConverterMap = Mockito.mock(Map.class);

    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter = Mockito.mock(CredentialToCloudCredentialConverter.class);

    @InjectMocks
    private EnvironmentNetworkService underTest = new EnvironmentNetworkService(
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
        AwsEnvironmentNetworkConverter networkConverter = Mockito.mock(AwsEnvironmentNetworkConverter.class);

        when(cloudConnector.networkConnector()).thenReturn(networkConnector);
        when(networkCreationRequestFactory.create(environmentDto)).thenReturn(networkCreationRequest);
        when(networkConnector.createNetworkWithSubnets(networkCreationRequest, USER_NAME)).thenReturn(createdCloudNetwork);
        when(environmentNetworkConverterMap.get(CloudPlatform.valueOf(CLOUD_PLATFORM))).thenReturn(networkConverter);
        when(networkConverter.setProviderSpecificNetwork(baseNetwork, createdCloudNetwork)).thenReturn(baseNetwork);

        BaseNetwork actual = underTest.createNetwork(environmentDto, baseNetwork);

        verify(cloudConnector).networkConnector();
        verify(cloudPlatformConnectors).get(any(CloudPlatformVariant.class));
        verify(networkCreationRequestFactory).create(environmentDto);
        verify(networkConnector).createNetworkWithSubnets(networkCreationRequest, USER_NAME);
        verify(environmentNetworkConverterMap).get(CloudPlatform.valueOf(CLOUD_PLATFORM));
        verify(networkConverter).setProviderSpecificNetwork(baseNetwork, createdCloudNetwork);
        assertEquals(baseNetwork, actual);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testCreateNetworkIfUnableToObtainNetworkConnectorThenBadRequestExceptionComes() {
        EnvironmentDto environmentDto = EnvironmentDto.builder().withCloudPlatform(CLOUD_PLATFORM).build();
        CloudConnector<Object> cloudConnector = mock(CloudConnector.class);

        when(cloudPlatformConnectors.get(any(CloudPlatformVariant.class))).thenReturn(cloudConnector);
        when(cloudConnector.networkConnector()).thenReturn(null);

        Assertions.assertThrows(BadRequestException.class, () -> underTest.createNetwork(environmentDto, new AzureNetwork()));

        verify(cloudPlatformConnectors, times(1)).get(any());
    }

    @Test
    void testDeleteNetworkShouldDeleteTheNetwork() {
        CloudCredential cloudCredential = new CloudCredential("1", "asd");
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
    void testDeleteNetworkShouldDeleteTheNetworkWithResourceGroup() {
        CloudCredential cloudCredential = new CloudCredential("1", "credName");
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
    }

    @Test
    void testGetNetworkCidr() {
        Credential credential = mock(Credential.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        Network network = mock(Network.class);

        String networkCidr = "10.0.0.0/16";

        when(credentialToCloudCredentialConverter.convert(credential)).thenReturn(cloudCredential);
        when(cloudConnector.networkConnector()).thenReturn(networkConnector);
        when(networkConnector.getNetworkCidr(network, cloudCredential)).thenReturn(networkCidr);

        String result = underTest.getNetworkCidr(network, "AWS", credential);

        assertEquals(networkCidr, result);
    }

    @Test
    void testGetNetworkCidrWhenNetworkNull() {
        Credential credential = mock(Credential.class);

        String result = underTest.getNetworkCidr(null, "AWS", credential);

        assertNull(result);
    }

    @Test
    void testGetNetworkCidrWhenNetworkConnectorNull() {
        Credential credential = mock(Credential.class);
        Network network = mock(Network.class);

        when(cloudConnector.networkConnector()).thenReturn(null);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> underTest.getNetworkCidr(network, "AWS", credential));
        assertEquals("No network connector for cloud platform: AWS", exception.getMessage());
    }

    private EnvironmentDto createEnvironmentDto(String resourceGroup) {
        return EnvironmentDto.builder()
                .withCloudPlatform(CLOUD_PLATFORM)
                .withCredential(new Credential())
                .withLocationDto(LocationDto.builder().withName("us-west-1").build())
                .withNetwork(NetworkDto.builder()
                        .withName("net-1")
                        .withAzure(AzureParams.AzureParamsBuilder.anAzureParams()
                                .withResourceGroupName(resourceGroup)
                                .build())
                        .build())
                .build();
    }
}
