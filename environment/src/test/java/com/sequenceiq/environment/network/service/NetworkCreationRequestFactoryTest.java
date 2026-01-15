package com.sequenceiq.environment.network.service;

import static com.sequenceiq.cloudbreak.cloud.model.network.SubnetType.PUBLIC;
import static com.sequenceiq.environment.network.service.Cidrs.cidrs;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkResourcesCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkSubnetRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.PrivateDatabaseVariant;
import com.sequenceiq.cloudbreak.converter.ServiceEndpointCreationToEndpointTypeConverter;
import com.sequenceiq.common.api.type.ServiceEndpointCreation;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.environment.domain.EnvironmentTags;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.LocationDto;
import com.sequenceiq.environment.environment.service.EnvironmentTagProvider;
import com.sequenceiq.environment.environment.validation.network.azure.AzureExistingPrivateDnsZonesService;
import com.sequenceiq.environment.network.dao.domain.AzureNetwork;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.network.dto.AzureParams;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.parameter.dto.AzureParametersDto;
import com.sequenceiq.environment.parameter.dto.AzureResourceGroupDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto;

@ExtendWith(MockitoExtension.class)
class NetworkCreationRequestFactoryTest {

    private static final String REGION = "us-west-1";

    private static final String ENV_NAME = "testEnv";

    private static final String STACK_NAME = "testEnv-1";

    private static final String NETWORK_CIDR = "1.1.1.1/16";

    private static final long NETWORK_ID = 1L;

    private static final String CLOUD_PLATFORM = "AWS";

    private static final String LEGACY_RG = "LEGACY-RG";

    private static final String SINGLE_RG = "SINGLE-RG";

    private static final Set<NetworkSubnetRequest> SUBNET_CIDRS = Collections.singleton(new NetworkSubnetRequest("10.10.1.1/24", PUBLIC));

    private final DefaultSubnetCidrProvider defaultSubnetCidrProvider = Mockito.mock(DefaultSubnetCidrProvider.class);

    private final CredentialToCloudCredentialConverter credentialToCloudCredentialConverter = Mockito.mock(CredentialToCloudCredentialConverter.class);

    private final AzureExistingPrivateDnsZonesService azureExistingPrivateDnsZonesService = Mockito.mock(AzureExistingPrivateDnsZonesService.class);

    private final EnvironmentTagProvider environmentTagProvider = mock(EnvironmentTagProvider.class);

    private final ServiceEndpointCreationToEndpointTypeConverter serviceEndpointCreationToEndpointTypeConverter
            = mock(ServiceEndpointCreationToEndpointTypeConverter.class);

    private final NetworkCreationRequestFactory underTest = new NetworkCreationRequestFactory(Collections.emptyList(),
            credentialToCloudCredentialConverter, defaultSubnetCidrProvider, environmentTagProvider, serviceEndpointCreationToEndpointTypeConverter,
            azureExistingPrivateDnsZonesService);

    @Test
    void testCreateShouldCreateANetworkCreationRequestWhenAzureParamsAreNotPresent() {
        EnvironmentDto environmentDto = createEnvironmentDtoWithoutAzureParams().build();
        CloudCredential cloudCredential = new CloudCredential("1", "asd", "account");

        when(credentialToCloudCredentialConverter.convert(environmentDto.getCredential())).thenReturn(cloudCredential);
        when(defaultSubnetCidrProvider.provide(NETWORK_CIDR, false)).thenReturn(cidrs(SUBNET_CIDRS, new HashSet<>()));
        when(environmentTagProvider.getTags(environmentDto, environmentDto.getNetwork().getResourceCrn())).thenReturn(Map.of());

        NetworkCreationRequest actual = underTest.create(environmentDto);

        verify(credentialToCloudCredentialConverter).convert(environmentDto.getCredential());
        verify(defaultSubnetCidrProvider).provide(NETWORK_CIDR, false);
        assertEquals(ENV_NAME, actual.getEnvName());
        assertEquals(STACK_NAME, actual.getStackName());
        assertEquals(cloudCredential, actual.getCloudCredential());
        assertEquals(CLOUD_PLATFORM, actual.getVariant());
        assertEquals(REGION, actual.getRegion().value());
        assertEquals(NETWORK_CIDR, actual.getNetworkCidr());
        assertEquals(SUBNET_CIDRS, actual.getPublicSubnets());
        assertFalse(actual.isNoPublicIp());
    }

    @Test
    void testCreateShouldCreateANetworkCreationRequestWhenAzureParamsArePresent() {
        EnvironmentDto environmentDto = createEnvironmentDtoWithAzureParams(ServiceEndpointCreation.DISABLED).build();
        CloudCredential cloudCredential = new CloudCredential("1", "asd", "account");

        when(credentialToCloudCredentialConverter.convert(environmentDto.getCredential())).thenReturn(cloudCredential);
        when(defaultSubnetCidrProvider.provide(NETWORK_CIDR, false)).thenReturn(cidrs(SUBNET_CIDRS, new HashSet<>()));

        NetworkCreationRequest actual = underTest.create(environmentDto);

        verify(credentialToCloudCredentialConverter).convert(environmentDto.getCredential());
        verify(defaultSubnetCidrProvider).provide(NETWORK_CIDR, false);

        assertEquals(ENV_NAME, actual.getEnvName());
        assertEquals(STACK_NAME, actual.getStackName());
        assertEquals(cloudCredential, actual.getCloudCredential());
        assertEquals(CLOUD_PLATFORM, actual.getVariant());
        assertEquals(REGION, actual.getRegion().value());
        assertEquals(NETWORK_CIDR, actual.getNetworkCidr());
        assertEquals(SUBNET_CIDRS, actual.getPublicSubnets());
        assertTrue(actual.isNoPublicIp());
    }

    @Test
    void testCreateShouldCreateANetworkCreationRequestWhenResourceGroupNameIsPresent() {
        EnvironmentDto environmentDto = createAzureParametersDto(ServiceEndpointCreation.DISABLED).build();
        CloudCredential cloudCredential = new CloudCredential("1", "asd", "account");

        when(credentialToCloudCredentialConverter.convert(environmentDto.getCredential())).thenReturn(cloudCredential);
        when(defaultSubnetCidrProvider.provide(NETWORK_CIDR, false)).thenReturn(cidrs(SUBNET_CIDRS, new HashSet<>()));

        NetworkCreationRequest actual = underTest.create(environmentDto);

        assertEquals(SINGLE_RG, actual.getResourceGroup());
    }

    @ParameterizedTest
    @EnumSource(ServiceEndpointCreation.class)
    void testCreateProviderSpecificNetworkResourcesShouldCreateAProviderSpecificNetworkResourcesCreationRequestWhenResourceGroupNameIsPresent(
            ServiceEndpointCreation serviceEndpointCreation) {
        EnvironmentDto environmentDto = createAzureParametersDto(serviceEndpointCreation).build();
        CloudCredential cloudCredential = new CloudCredential("1", "asd", "account");
        BaseNetwork baseNetwork = getNetwork();

        when(credentialToCloudCredentialConverter.convert(environmentDto.getCredential())).thenReturn(cloudCredential);
        when(defaultSubnetCidrProvider.provide(NETWORK_CIDR, false)).thenReturn(cidrs(SUBNET_CIDRS, new HashSet<>()));

        NetworkResourcesCreationRequest request = underTest.createProviderSpecificNetworkResources(environmentDto, baseNetwork);

        assertEquals(NETWORK_ID, Long.parseLong(request.getNetworkId()));
        assertEquals(LEGACY_RG, request.getNetworkResourceGroup());
        assertEquals(cloudCredential, request.getCloudCredential());
        assertEquals(REGION, request.getRegion().getRegionName());
        assertEquals(SINGLE_RG, request.getResourceGroup());
        switch (serviceEndpointCreation) {
            case ENABLED, DISABLED ->
                    assertEquals(PrivateDatabaseVariant.NONE, request.getPrivateEndpointVariant());
            case ENABLED_PRIVATE_ENDPOINT ->
                    assertEquals(PrivateDatabaseVariant.FLEXIBLE_POSTGRES_WITH_PE_AND_NEW_DNS_ZONE, request.getPrivateEndpointVariant());
            // Needed for checkstyle
            default -> { }
        }
    }

    private EnvironmentDto.EnvironmentDtoBuilder createEnvironmentDtoWithoutAzureParams() {
        return EnvironmentDto.builder()
                .withName(ENV_NAME)
                .withTags(new EnvironmentTags(new HashMap<>(), new HashMap<>()))
                .withCreator("creator")
                .withCredential(new Credential())
                .withCloudPlatform(CLOUD_PLATFORM)
                .withLocationDto(LocationDto.builder().withName(REGION).build())
                .withNetwork(NetworkDto.builder().withId(NETWORK_ID).withNetworkCidr(NETWORK_CIDR).build());
    }

    private EnvironmentDto.EnvironmentDtoBuilder createEnvironmentDtoWithAzureParams(ServiceEndpointCreation serviceEndpointCreation) {
        EnvironmentDto.EnvironmentDtoBuilder builder = createEnvironmentDtoWithoutAzureParams();
        builder.withNetwork(NetworkDto.builder()
                .withId(NETWORK_ID)
                .withServiceEndpointCreation(serviceEndpointCreation)
                .withNetworkCidr(NETWORK_CIDR)
                .withAzure(AzureParams.builder()
                        .withNoPublicIp(true)
                        .withNetworkId(String.valueOf(NETWORK_ID))
                        .withResourceGroupName(LEGACY_RG)
                        .build())
                .build());
        return builder;
    }

    private EnvironmentDto.EnvironmentDtoBuilder createAzureParametersDto(ServiceEndpointCreation serviceEndpointCreation) {
        return createEnvironmentDtoWithAzureParams(serviceEndpointCreation)
                .withParameters(ParametersDto.builder()
                        .withAzureParametersDto(
                                AzureParametersDto.builder()
                                        .withAzureResourceGroupDto(AzureResourceGroupDto.builder()
                                                .withName(SINGLE_RG)
                                                .build())
                                        .build())
                        .build());
    }

    private BaseNetwork getNetwork() {
        AzureNetwork azureNetwork = new AzureNetwork();
        azureNetwork.setNetworkId(String.valueOf(NETWORK_ID));
        azureNetwork.setResourceGroupName(LEGACY_RG);
        return azureNetwork;
    }
}