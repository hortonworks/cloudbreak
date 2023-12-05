package com.sequenceiq.environment.environment.validation.network.azure;


import static com.sequenceiq.cloudbreak.cloud.model.network.PrivateDatabaseVariant.NONE;
import static com.sequenceiq.cloudbreak.cloud.model.network.PrivateDatabaseVariant.POSTGRES_WITH_EXISTING_DNS_ZONE;
import static com.sequenceiq.cloudbreak.cloud.model.network.PrivateDatabaseVariant.POSTGRES_WITH_NEW_DNS_ZONE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.azure.AzureCloudSubnetParametersService;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClientService;
import com.sequenceiq.cloudbreak.cloud.azure.validator.privatedns.AzureExistingPrivateDnsZoneValidatorService;
import com.sequenceiq.cloudbreak.cloud.azure.validator.privatedns.AzureNewPrivateDnsZoneValidatorService;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.util.NullUtil;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.common.api.type.ServiceEndpointCreation;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.validation.ValidationType;
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

    private static final String EXISTING_DATABASE_PRIVATE_DNS_ZONE_ID = "existingDatabasePrivateDnsZoneId";

    private static final String EXISTING_AKS_PRIVATE_DNS_ZONE_ID = "existingAksPrivateDnsZoneId";

    private static final String FLEXIBLE_SERVER_SUBNET_ID = "flexibleServerSubnetId";

    @Mock
    private AzureCloudSubnetParametersService azureCloudSubnetParametersService;

    @Mock
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Mock
    private AzureClientService azureClientService;

    @Mock
    private AzureExistingPrivateDnsZoneValidatorService azureExistingPrivateDnsZoneValidatorService;

    @Mock
    private AzureNewPrivateDnsZoneValidatorService azureNewPrivateDnsZoneValidatorService;

    @Mock
    private AzureExistingPrivateDnsZonesService azureExistingPrivateDnsZonesService;

    private AzurePrivateEndpointValidator underTest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        underTest = new AzurePrivateEndpointValidator(
                azureCloudSubnetParametersService,
                credentialToCloudCredentialConverter,
                azureClientService,
                azureExistingPrivateDnsZoneValidatorService,
                azureNewPrivateDnsZoneValidatorService,
                azureExistingPrivateDnsZonesService
        );
    }

    @Test
    void testCheckNetworkPoliciesWhenExistingNetworkWhenPrivateEndpointNetworkPoliciesEnabled() {
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();
        AzureParams azureParams = getAzureParams();
        NetworkDto networkDto = NetworkTestUtils.getNetworkDtoBuilder(azureParams, null, null, azureParams.getNetworkId(), null, 1, 0, RegistrationType.EXISTING)
                .withServiceEndpointCreation(ServiceEndpointCreation.ENABLED_PRIVATE_ENDPOINT)
                .build();
        when(azureCloudSubnetParametersService.isPrivateEndpointNetworkPoliciesDisabled(any())).thenCallRealMethod();

        underTest.checkNetworkPoliciesWhenExistingNetwork(networkDto, getCloudSubnets(true), validationResultBuilder);

        assertTrue(validationResultBuilder.build().hasError());
        NetworkTestUtils.checkErrorsPresent(validationResultBuilder, List.of(
                "It is not possible to create private endpoints for existing network with id 'networkId' in resource group 'networkResourceGroupName': " +
                        "Azure requires at least one subnet with private endpoint network policies (eg. NSGs) disabled.  Please disable private endpoint " +
                        "network policies in at least one of the following subnets and retry: 'subnet-one'. Refer to Microsoft documentation at: " +
                        "https://docs.microsoft.com/en-us/azure/private-link/disable-private-endpoint-network-policy"));
    }

    @Test
    void testCheckNetworkPoliciesWhenExistingNetworkWhenPrivateEndpointNetworkPoliciesDisabled() {
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();
        NetworkDto networkDto = getNetworkDto(getAzureParams());
        when(azureCloudSubnetParametersService.isPrivateEndpointNetworkPoliciesDisabled(any())).thenCallRealMethod();

        underTest.checkNetworkPoliciesWhenExistingNetwork(networkDto, getCloudSubnets(false), validationResultBuilder);

        assertFalse(validationResultBuilder.build().hasError());
    }

    @Test
    void testCheckNewPrivateDnsZoneWhenConnectedZoneFromAnotherRG() {
        ValidationResult.ValidationResultBuilder envValidationResultBuilder = new ValidationResult.ValidationResultBuilder();
        String message = "myCustomErrorMessage";

        EnvironmentDto environmentDto = getEnvironmentDto(MY_SINGLE_RG, ResourceGroupUsagePattern.USE_SINGLE);
        AzureParams azureParams = getAzureParams();
        NetworkDto networkDto = getNetworkDto(azureParams);
        when(azureNewPrivateDnsZoneValidatorService.zonesNotConnectedToNetwork(any(), any(), eq(MY_SINGLE_RG), any(),
                eq(POSTGRES_WITH_NEW_DNS_ZONE), any())).
                thenReturn(envValidationResultBuilder.error(message));

        underTest.checkNewPrivateDnsZone(envValidationResultBuilder, environmentDto, networkDto);

        assertTrue(envValidationResultBuilder.build().hasError());
        NetworkTestUtils.checkErrorsPresent(envValidationResultBuilder, List.of(message));
    }

    @Test
    void testCheckNewPrivateDnsZoneWhenNotExists() {
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();
        EnvironmentDto environmentDto = getEnvironmentDto(MY_SINGLE_RG, ResourceGroupUsagePattern.USE_SINGLE);
        AzureParams azureParams = getAzureParams();
        when(azureNewPrivateDnsZoneValidatorService.zonesNotConnectedToNetwork(any(), any(), eq(MY_SINGLE_RG), any(), eq(POSTGRES_WITH_NEW_DNS_ZONE), any())).
                thenReturn(null);

        underTest.checkNewPrivateDnsZone(validationResultBuilder, environmentDto, getNetworkDto(azureParams));

        verify(credentialToCloudCredentialConverter).convert(any());
        verify(azureClientService).getClient(any());
        verify(azureNewPrivateDnsZoneValidatorService).zonesNotConnectedToNetwork(any(), any(), eq(MY_SINGLE_RG), any(), eq(POSTGRES_WITH_NEW_DNS_ZONE), any());
        assertFalse(validationResultBuilder.build().hasError());
    }

    @Test
    void testCheckNewPrivateDnsZoneWhenServiceEndpoints() {
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();
        EnvironmentDto environmentDto = getEnvironmentDto(MY_SINGLE_RG, ResourceGroupUsagePattern.USE_SINGLE);
        AzureParams azureParams = getAzureParams();

        underTest.checkNewPrivateDnsZone(validationResultBuilder, environmentDto,
                getNetworkDto(azureParams, ServiceEndpointCreation.ENABLED));

        verify(credentialToCloudCredentialConverter, never()).convert(any());
        verify(azureClientService, never()).getClient(any());
        verify(azureNewPrivateDnsZoneValidatorService, never()).zonesNotConnectedToNetwork(any(), any(), any(), any(), eq(NONE), any());
        assertFalse(validationResultBuilder.build().hasError());
    }

    @Test
    void testCheckNewPrivateDnsZoneWhenMultipleRg() {
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();
        EnvironmentDto environmentDto = getEnvironmentDto(MY_SINGLE_RG, ResourceGroupUsagePattern.USE_MULTIPLE);
        AzureParams azureParams = getAzureParams();

        underTest.checkNewPrivateDnsZone(validationResultBuilder, environmentDto, getNetworkDto(azureParams));

        verify(credentialToCloudCredentialConverter, never()).convert(any());
        verify(azureClientService, never()).getClient(any());
        verify(azureNewPrivateDnsZoneValidatorService, never()).zonesNotConnectedToNetwork(any(), any(), any(), any(), eq(POSTGRES_WITH_NEW_DNS_ZONE), any());
        assertFalse(validationResultBuilder.build().hasError());
    }

    @Test
    void testCheckNewPrivateDnsZoneWhenExistingDnsZone() {
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();
        EnvironmentDto environmentDto = getEnvironmentDto(MY_SINGLE_RG, ResourceGroupUsagePattern.USE_SINGLE);
        AzureParams azureParams = getAzureParams(EXISTING_DATABASE_PRIVATE_DNS_ZONE_ID, null);

        underTest.checkNewPrivateDnsZone(validationResultBuilder, environmentDto, getNetworkDto(azureParams));

        verify(credentialToCloudCredentialConverter).convert(any());
        verify(azureClientService).getClient(any());
        verify(azureNewPrivateDnsZoneValidatorService).zonesNotConnectedToNetwork(any(), eq(NETWORK_ID), eq(MY_SINGLE_RG),
                any(), eq(POSTGRES_WITH_EXISTING_DNS_ZONE), eq(validationResultBuilder));
        assertFalse(validationResultBuilder.build().hasError());
    }

    @Test
    void testCheckMultipleResourceGroupWhenMultipleRG() {
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();
        EnvironmentDto environmentDto = getEnvironmentDto(null, ResourceGroupUsagePattern.USE_MULTIPLE);

        underTest.checkMultipleResourceGroup(validationResultBuilder, environmentDto, getNetworkDto(getAzureParams()));

        assertTrue(validationResultBuilder.build().hasError());
        NetworkTestUtils.checkErrorsPresent(validationResultBuilder, List.of(
                "Private endpoint creation is not supported for multiple resource group deployment model, please use the single " +
                        "resource group deployment model to be able to use private endpoints on Azure!"));
    }

    @Test
    void testCheckExistingManagedPrivateDnsZoneWhenNoExistingDnsZonesProvided() {
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();
        EnvironmentDto environmentDto = getEnvironmentDto(null, ResourceGroupUsagePattern.USE_SINGLE);
        NetworkDto networkDto = getNetworkDto(getAzureParams(), ServiceEndpointCreation.ENABLED_PRIVATE_ENDPOINT);
        when(azureExistingPrivateDnsZonesService.hasNoExistingManagedZones(networkDto)).thenReturn(true);

        underTest.checkExistingManagedPrivateDnsZone(validationResultBuilder, environmentDto, networkDto);

        verify(credentialToCloudCredentialConverter, never()).convert(any());
        verify(azureClientService, never()).getClient(any());
        verify(azureExistingPrivateDnsZoneValidatorService, never()).validate(any(), any(), any(), any(), any());
    }

    @Test
    void testCheckExistingManagedPrivateDnsZoneWhenNotPrivateEndpoint() {
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();
        AzureParams azureParams = getAzureParams(EXISTING_DATABASE_PRIVATE_DNS_ZONE_ID, null);
        NetworkDto networkDto = getNetworkDto(azureParams, ServiceEndpointCreation.DISABLED);
        when(azureExistingPrivateDnsZonesService.hasNoExistingManagedZones(networkDto)).thenReturn(false);

        underTest.checkExistingManagedPrivateDnsZone(validationResultBuilder, new EnvironmentDto(), networkDto);

        assertTrue(validationResultBuilder.build().hasError());
        NetworkTestUtils.checkErrorsPresent(validationResultBuilder, List.of(
                "A private DNS zone is provided, but private endpoint creation is turned off and no Flexible server delegated subnet is specified. "
                        + "Please specify exactly one of them or do not specify the existing private DNS zone."));
    }

    @Test
    void testCheckExistingManagedPrivateDnsZoneWhenPrivateEndpoint() {
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();
        AzureParams azureParams = getAzureParams(EXISTING_DATABASE_PRIVATE_DNS_ZONE_ID, null);
        NetworkDto networkDto = getNetworkDto(azureParams);
        when(azureExistingPrivateDnsZonesService.hasNoExistingManagedZones(networkDto)).thenReturn(false);

        underTest.checkExistingManagedPrivateDnsZone(validationResultBuilder, new EnvironmentDto(), networkDto);

        assertFalse(validationResultBuilder.build().hasError());
        verify(credentialToCloudCredentialConverter).convert(any());
        verify(azureClientService).getClient(any());
        verify(azureExistingPrivateDnsZoneValidatorService).validate(any(), any(), any(), any(), eq(validationResultBuilder));
    }

    @Test
    void testCheckExistingManagedPrivateDnsZoneWhenBothPrivateEndpointAndFlexibleServerSubnet() {
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();
        AzureParams azureParams = getAzureParams(EXISTING_DATABASE_PRIVATE_DNS_ZONE_ID, null, FLEXIBLE_SERVER_SUBNET_ID);
        NetworkDto networkDto = getNetworkDto(azureParams);
        when(azureExistingPrivateDnsZonesService.hasNoExistingManagedZones(networkDto)).thenReturn(false);

        underTest.checkExistingManagedPrivateDnsZone(validationResultBuilder, new EnvironmentDto(), networkDto);

        assertFalse(validationResultBuilder.build().hasError());
        verify(credentialToCloudCredentialConverter).convert(any());
        verify(azureClientService).getClient(any());
        verify(azureExistingPrivateDnsZoneValidatorService).validate(any(), any(), any(), any(), eq(validationResultBuilder));
    }

    @Test
    void testCheckExistingRegisteredOnlyPrivateDnsZoneWhenNoRegisteredOnlyZones() {
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();
        AzureParams azureParams = getAzureParams(null, null);
        NetworkDto networkDto = getNetworkDto(azureParams);
        when(azureExistingPrivateDnsZonesService.hasNoExistingRegisteredOnlyZones(networkDto)).thenReturn(true);

        underTest.checkExistingRegisteredOnlyPrivateDnsZone(validationResultBuilder, new EnvironmentDto(), networkDto);

        assertFalse(validationResultBuilder.build().hasError());
        verify(credentialToCloudCredentialConverter, never()).convert(any());
        verify(azureClientService, never()).getClient(any());
        verify(azureExistingPrivateDnsZoneValidatorService, never()).validate(any(), any(), any(), any(), any());
    }

    @Test
    void testCheckExistingRegisteredOnlyPrivateDnsZoneWhenExistingRegisteredDnsZoneProvided() {
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();
        AzureParams azureParams = getAzureParams(null, EXISTING_AKS_PRIVATE_DNS_ZONE_ID);
        NetworkDto networkDto = getNetworkDto(azureParams);
        when(azureExistingPrivateDnsZonesService.hasNoExistingRegisteredOnlyZones(networkDto)).thenReturn(false);

        underTest.checkExistingRegisteredOnlyPrivateDnsZone(validationResultBuilder, new EnvironmentDto(), networkDto);

        assertFalse(validationResultBuilder.build().hasError());
        verify(credentialToCloudCredentialConverter).convert(any());
        verify(azureClientService).getClient(any());
        verify(azureExistingPrivateDnsZoneValidatorService).validate(any(), any(), any(), any(), eq(validationResultBuilder));
    }

    @Test
    void testCheckExistingDnsZoneDeletion() {
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();
        underTest.checkExistingDnsZoneDeletion(ValidationType.ENVIRONMENT_EDIT, "orig", "new", validationResultBuilder);
        assertFalse(validationResultBuilder.build().hasError());
    }

    @Test
    void testCheckExistingDnsZoneDeletionInvalid() {
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();
        underTest.checkExistingDnsZoneDeletion(ValidationType.ENVIRONMENT_EDIT, "orig", "", validationResultBuilder);
        ValidationResult validationResult = validationResultBuilder.build();
        assertTrue(validationResult.hasError());
        assertTrue(validationResult.getErrors().stream().anyMatch(error -> error.startsWith("Deletion of existing dns zone id is not a valid operation")));
    }

    private AzureParams getAzureParams() {
        return getAzureParams(null, null);
    }

    private AzureParams getAzureParams(String databasePrivateDnsZoneId, String aksPrivateDnsZoneId) {
        return getAzureParams(databasePrivateDnsZoneId, aksPrivateDnsZoneId, null);
    }

    private AzureParams getAzureParams(String databasePrivateDnsZoneId, String aksPrivateDnsZoneId, String flexibleSubnetId) {
        return AzureParams.builder()
                .withNetworkId(NETWORK_ID)
                .withResourceGroupName("networkResourceGroupName")
                .withDatabasePrivateDnsZoneId(databasePrivateDnsZoneId)
                .withAksPrivateDnsZoneId(aksPrivateDnsZoneId)
                .withFlexibleServerSubnetIds(NullUtil.getIfNotNullOtherwise(flexibleSubnetId, Set::of, null))
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
                        .withAzureParametersDto(
                                AzureParametersDto.builder()
                                        .withAzureResourceGroupDto(AzureResourceGroupDto.builder()
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
