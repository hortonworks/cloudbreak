package com.sequenceiq.environment.environment.validation.network.azure;


import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.azure.AzureCloudSubnetParametersService;
import com.sequenceiq.cloudbreak.cloud.azure.AzureNetworkLinkService;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClientService;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.common.api.type.ServiceEndpointCreation;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.validation.network.NetworkTestUtils;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.environment.network.dto.AzureParams;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.parameter.dto.AzureParametersDto;
import com.sequenceiq.environment.parameter.dto.AzureResourceGroupDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto;
import com.sequenceiq.environment.parameter.dto.ResourceGroupUsagePattern;

@ExtendWith(MockitoExtension.class)
public class AzurePrivateEndpointValidatorTest {

    private static final String MY_SINGLE_RG = "mySingleRg";

    private static final String NETWORK_ID = "networkId";

    private static final String EXISTING_PRIVATE_DNS_ZONE_ID = "existingPrivateDnsZoneId";

    @Mock
    private AzureCloudSubnetParametersService azureCloudSubnetParametersService;

    @Mock
    private AzureNetworkLinkService azureNetworkLinkService;

    @Mock
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Mock
    private AzureClientService azureClientService;

    private AzurePrivateEndpointValidator underTest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        underTest = new AzurePrivateEndpointValidator(
                azureCloudSubnetParametersService,
                azureNetworkLinkService,
                credentialToCloudCredentialConverter,
                azureClientService
        );
    }

    @Test
    void testCheckPrivateEndpointNetworkPoliciesWhenExistingNetworkAndPrivateEndpointNetworkPoliciesEnabled() {
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();
        AzureParams azureParams = getAzureParams();
        NetworkDto networkDto = NetworkTestUtils.getNetworkDtoBuilder(azureParams, null, null, azureParams.getNetworkId(), null, 1, RegistrationType.EXISTING)
                .withServiceEndpointCreation(ServiceEndpointCreation.ENABLED_PRIVATE_ENDPOINT)
                .build();
        when(azureCloudSubnetParametersService.isPrivateEndpointNetworkPoliciesDisabled(any())).thenCallRealMethod();

        underTest.checkPrivateEndpointNetworkPoliciesWhenExistingNetwork(networkDto, getCloudSubnets(true), validationResultBuilder);

        assertTrue(validationResultBuilder.build().hasError());
        NetworkTestUtils.checkErrorsPresent(validationResultBuilder, List.of(
                "It is not possible to create private endpoints for existing network with id 'networkId' in resource group 'networkResourceGroupName': " +
                        "Azure requires at least one subnet with private endpoint network policies (eg. NSGs) disabled.  Please disable private endpoint " +
                        "network policies in at least one of the following subnets and retry: 'subnet-one'. Refer to Microsoft documentation at: " +
                        "https://docs.microsoft.com/en-us/azure/private-link/disable-private-endpoint-network-policy"));
    }

    @Test
    void testCheckPrivateEndpointNetworkPoliciesWhenExistingNetworkAndPrivateEndpointNetworkPoliciesDisabled() {
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();
        NetworkDto networkDto = getNetworkDto(getAzureParams());
        when(azureCloudSubnetParametersService.isPrivateEndpointNetworkPoliciesDisabled(any())).thenCallRealMethod();

        underTest.checkPrivateEndpointNetworkPoliciesWhenExistingNetwork(networkDto, getCloudSubnets(false), validationResultBuilder);

        assertFalse(validationResultBuilder.build().hasError());
    }

    @Test
    void testCheckPrivateEndpointForExistingNetworkLinkWhenLinkFromAnotherRG() {
        ValidationResult.ValidationResultBuilder envValidationResultBuilder = new ValidationResult.ValidationResultBuilder();
        ValidationResult.ValidationResultBuilder azureValidationResultBuilder = new ValidationResult.ValidationResultBuilder();
        String message = "Network link for the network aNetworkLink already exists for Private DNS Zone "
                + "privatelink.postgres.database.azure.com in resource group mySingleRg. Please ensure that there is no existing network link and try again!";

        EnvironmentDto environmentDto = getEnvironmentDto(MY_SINGLE_RG, ResourceGroupUsagePattern.USE_SINGLE);
        AzureParams azureParams = getAzureParams();
        NetworkDto networkDto = getNetworkDto(azureParams);
        when(azureNetworkLinkService.validateExistingNetworkLink(any(), any(), eq(MY_SINGLE_RG))).
                thenReturn(azureValidationResultBuilder.error(message).build());

        underTest.checkPrivateEndpointForExistingNetworkLink(envValidationResultBuilder, environmentDto, networkDto);

        assertTrue(envValidationResultBuilder.build().hasError());
        NetworkTestUtils.checkErrorsPresent(envValidationResultBuilder, List.of(message));
    }

    @Test
    void testCheckPrivateEndpointForExistingNetworkLinkWhenNotExists() {
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();
        EnvironmentDto environmentDto = getEnvironmentDto(MY_SINGLE_RG, ResourceGroupUsagePattern.USE_SINGLE);
        AzureParams azureParams = getAzureParams();
        when(azureNetworkLinkService.validateExistingNetworkLink(any(), any(), eq(MY_SINGLE_RG))).thenReturn(null);

        underTest.checkPrivateEndpointForExistingNetworkLink(validationResultBuilder, environmentDto, getNetworkDto(azureParams));

        verify(credentialToCloudCredentialConverter).convert(any());
        verify(azureClientService).getClient(any());
        verify(azureNetworkLinkService).validateExistingNetworkLink(any(), any(), any());
        assertFalse(validationResultBuilder.build().hasError());
    }

    @Test
    void testCheckPrivateEndpointForExistingNetworkLinkWhenServiceEndpoints() {
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();
        EnvironmentDto environmentDto = getEnvironmentDto(MY_SINGLE_RG, ResourceGroupUsagePattern.USE_SINGLE);
        AzureParams azureParams = getAzureParams();

        underTest.checkPrivateEndpointForExistingNetworkLink(validationResultBuilder, environmentDto,
                getNetworkDto(azureParams, ServiceEndpointCreation.ENABLED));

        verify(credentialToCloudCredentialConverter, never()).convert(any());
        verify(azureClientService, never()).getClient(any());
        verify(azureNetworkLinkService, never()).validateExistingNetworkLink(any(), any(), any());
        assertFalse(validationResultBuilder.build().hasError());
    }

    @Test
    void testCheckPrivateEndpointForExistingNetworkLinkWhenMultipleRg() {
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();
        EnvironmentDto environmentDto = getEnvironmentDto(MY_SINGLE_RG, ResourceGroupUsagePattern.USE_MULTIPLE);
        AzureParams azureParams = getAzureParams();

        underTest.checkPrivateEndpointForExistingNetworkLink(validationResultBuilder, environmentDto, getNetworkDto(azureParams));

        verify(credentialToCloudCredentialConverter, never()).convert(any());
        verify(azureClientService, never()).getClient(any());
        verify(azureNetworkLinkService, never()).validateExistingNetworkLink(any(), any(), any());
        assertFalse(validationResultBuilder.build().hasError());
    }

    @Test
    void testCheckPrivateEndpointForExistingNetworkLinkWhenExistingDnsZone() {
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();
        EnvironmentDto environmentDto = getEnvironmentDto(MY_SINGLE_RG, ResourceGroupUsagePattern.USE_SINGLE);
        AzureParams azureParams = getAzureParams(EXISTING_PRIVATE_DNS_ZONE_ID);

        underTest.checkPrivateEndpointForExistingNetworkLink(validationResultBuilder, environmentDto, getNetworkDto(azureParams));

        verify(credentialToCloudCredentialConverter, never()).convert(any());
        verify(azureClientService, never()).getClient(any());
        verify(azureNetworkLinkService, never()).validateExistingNetworkLink(any(), any(), any());
        assertFalse(validationResultBuilder.build().hasError());
    }

    @Test
    void testCheckPrivateEndpointsWhenMultipleResourceGroupWhenMultipleRG() {
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();
        EnvironmentDto environmentDto = getEnvironmentDto(null, ResourceGroupUsagePattern.USE_MULTIPLE);

        underTest.checkPrivateEndpointsWhenMultipleResourceGroup(validationResultBuilder, environmentDto, ServiceEndpointCreation.ENABLED_PRIVATE_ENDPOINT);

        assertTrue(validationResultBuilder.build().hasError());
        NetworkTestUtils.checkErrorsPresent(validationResultBuilder, List.of(
                "Private endpoint creation is not supported for multiple resource group deployment model, please use single single " +
                        "resource groups to be able to use private endpoints in Azure!"));
    }

    @Test
    void testCheckExistingPrivateDnsZoneWhenNotPrivateEndpoint() {
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();
        AzureParams azureParams = getAzureParams(EXISTING_PRIVATE_DNS_ZONE_ID);
        NetworkDto networkDto = getNetworkDto(azureParams, ServiceEndpointCreation.DISABLED);

        underTest.checkExistingPrivateDnsZoneWhenNotPrivateEndpoint(validationResultBuilder, networkDto);

        assertTrue(validationResultBuilder.build().hasError());
        NetworkTestUtils.checkErrorsPresent(validationResultBuilder, List.of(
                "A private DNS zone is provided, but private endpoint creation is turned off. Please either turn on private endpoint creation" +
                        " or do not specify the existing private DNS zone."));
    }

    private AzureParams getAzureParams() {
        return getAzureParams(null);
    }

    private AzureParams getAzureParams(String privateDnsZoneId) {
        return AzureParams.builder()
                .withNetworkId(AzurePrivateEndpointValidatorTest.NETWORK_ID)
                .withResourceGroupName("networkResourceGroupName")
                .withPrivateDnsZoneId(privateDnsZoneId)
                .build();
    }

    private NetworkDto getNetworkDto(AzureParams azureParams) {
        return getNetworkDto(azureParams, ServiceEndpointCreation.ENABLED_PRIVATE_ENDPOINT);
    }

    private NetworkDto getNetworkDto(AzureParams azureParams, ServiceEndpointCreation serviceEndpointCreation) {
        return NetworkDto.builder()
                .withId(1L)
                .withName("networkName")
                .withResourceCrn("aResourceCRN")
                .withAzure(azureParams)
                .withSubnetMetas(Map.of())
                .withNetworkId("networkId")
                .withServiceEndpointCreation(serviceEndpointCreation)
                .build();
    }

    private EnvironmentDto getEnvironmentDto(String name, ResourceGroupUsagePattern resourceGroupUsagePattern) {
        return EnvironmentDto.builder()
                .withParameters(ParametersDto.builder()
                        .withAzureParameters(
                                AzureParametersDto.builder()
                                        .withResourceGroup(AzureResourceGroupDto.builder()
                                                .withName(name)
                                                .withResourceGroupUsagePattern(resourceGroupUsagePattern)
                                                .build())
                                        .build()
                        ).build())
                .build();
    }

    private Map<String, CloudSubnet> getCloudSubnets(boolean privateEndpointNetworkPoliciesEnabled) {
        CloudSubnet cloudSubnetOne = new CloudSubnet();
        cloudSubnetOne.putParameter("privateEndpointNetworkPolicies", privateEndpointNetworkPoliciesEnabled ? "enabled" : "disabled");
        cloudSubnetOne.setName("subnet-one");
        return Map.of("subnet-one", cloudSubnetOne);
    }

}
