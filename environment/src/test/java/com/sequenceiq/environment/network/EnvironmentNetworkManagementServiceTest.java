package com.sequenceiq.environment.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.NetworkConnector;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedCloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkDeletionRequest;
import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.LocationDto;
import com.sequenceiq.environment.network.dao.domain.AwsNetwork;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.network.dto.AzureParams;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.network.service.NetworkCreationRequestFactory;
import com.sequenceiq.environment.network.v1.converter.AwsEnvironmentNetworkConverter;
import com.sequenceiq.environment.network.v1.converter.EnvironmentNetworkConverter;

@RunWith(MockitoJUnitRunner.class)
public class EnvironmentNetworkManagementServiceTest {

    private static final String CLOUD_PLATFORM = "AWS";

    @InjectMocks
    private EnvironmentNetworkManagementService underTest;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private NetworkCreationRequestFactory networkCreationRequestFactory;

    @Mock
    private Map<CloudPlatform, EnvironmentNetworkConverter> environmentNetworkConverterMap;

    @Mock
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Mock
    private CloudConnector<Object> cloudConnector;

    @Mock
    private NetworkConnector networkConnector;

    @Before
    public void before() {
        when(cloudPlatformConnectors.get(any(CloudPlatformVariant.class))).thenReturn(cloudConnector);
        when(cloudConnector.networkConnector()).thenReturn(networkConnector);
    }

    @Test
    public void testCreateNetworkShouldReturnWithANewNetwork() {
        EnvironmentDto environmentDto = EnvironmentDto.builder().withCloudPlatform(CLOUD_PLATFORM).build();
        BaseNetwork baseNetwork = new AwsNetwork();
        NetworkCreationRequest networkCreationRequest = new NetworkCreationRequest.Builder().build();
        CreatedCloudNetwork createdCloudNetwork = new CreatedCloudNetwork();
        AwsEnvironmentNetworkConverter networkConverter = Mockito.mock(AwsEnvironmentNetworkConverter.class);

        when(networkCreationRequestFactory.create(environmentDto)).thenReturn(networkCreationRequest);
        when(networkConnector.createNetworkWithSubnets(networkCreationRequest)).thenReturn(createdCloudNetwork);
        when(environmentNetworkConverterMap.get(CloudPlatform.valueOf(CLOUD_PLATFORM))).thenReturn(networkConverter);
        when(networkConverter.setProviderSpecificNetwork(baseNetwork, createdCloudNetwork)).thenReturn(baseNetwork);

        BaseNetwork actual = underTest.createNetwork(environmentDto, baseNetwork);

        verify(cloudConnector).networkConnector();
        verify(cloudPlatformConnectors).get(any(CloudPlatformVariant.class));
        verify(networkCreationRequestFactory).create(environmentDto);
        verify(networkConnector).createNetworkWithSubnets(networkCreationRequest);
        verify(environmentNetworkConverterMap).get(CloudPlatform.valueOf(CLOUD_PLATFORM));
        verify(networkConverter).setProviderSpecificNetwork(baseNetwork, createdCloudNetwork);
        assertEquals(baseNetwork, actual);
    }

    @Test
    public void testDeleteNetworkShouldDeleteTheNetwork() {
        CloudCredential cloudCredential = new CloudCredential("1", "asd");
        EnvironmentDto environmentDto = createEnvironmentDto(null);

        when(credentialToCloudCredentialConverter.convert(environmentDto.getCredential())).thenReturn(cloudCredential);

        underTest.deleteNetwork(environmentDto);

        ArgumentCaptor<NetworkDeletionRequest> argumentCaptor = ArgumentCaptor.forClass(NetworkDeletionRequest.class);

        verify(networkConnector).deleteNetworkWithSubnets(argumentCaptor.capture());

        assertEquals(environmentDto.getNetwork().getNetworkName(), argumentCaptor.getValue().getStackName());
        assertEquals(cloudCredential, argumentCaptor.getValue().getCloudCredential());
        assertEquals(environmentDto.getLocation().getName(), argumentCaptor.getValue().getRegion());
        assertNull(argumentCaptor.getValue().getResourceGroup());
    }

    @Test
    public void testDeleteNetworkShouldDeleteTheNetworkWithResourceGroup() {
        CloudCredential cloudCredential = new CloudCredential("1", "asd");
        EnvironmentDto environmentDto = createEnvironmentDto("resourceGroup");

        when(credentialToCloudCredentialConverter.convert(environmentDto.getCredential())).thenReturn(cloudCredential);

        underTest.deleteNetwork(environmentDto);

        ArgumentCaptor<NetworkDeletionRequest> argumentCaptor = ArgumentCaptor.forClass(NetworkDeletionRequest.class);

        verify(networkConnector).deleteNetworkWithSubnets(argumentCaptor.capture());

        assertEquals(environmentDto.getNetwork().getNetworkName(), argumentCaptor.getValue().getStackName());
        assertEquals(cloudCredential, argumentCaptor.getValue().getCloudCredential());
        assertEquals(environmentDto.getLocation().getName(), argumentCaptor.getValue().getRegion());
        assertEquals(environmentDto.getNetwork().getAzure().getResourceGroupName(), argumentCaptor.getValue().getResourceGroup());
    }

    private EnvironmentDto createEnvironmentDto(String resourceGroup) {
        return EnvironmentDto.builder()
                .withCloudPlatform(CLOUD_PLATFORM)
                .withCredential(new Credential())
                .withLocationDto(LocationDto.LocationDtoBuilder.aLocationDto().withName("us-west").build())
                .withNetwork(NetworkDto.Builder.aNetworkDto()
                        .withName("net-1")
                        .withAzure(AzureParams.AzureParamsBuilder.anAzureParams()
                                .withResourceGroupName(resourceGroup)
                                .build())
                        .build())
                .build();
    }
}