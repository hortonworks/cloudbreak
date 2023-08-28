package com.sequenceiq.environment.environment.validation.network.azure;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.azure.AzureCloudSubnetParametersService;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClientService;
import com.sequenceiq.cloudbreak.cloud.azure.validator.privatedns.AzureExistingPrivateDnsZoneValidatorService;
import com.sequenceiq.cloudbreak.cloud.azure.validator.privatedns.AzureNewPrivateDnsZoneValidatorService;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.parameter.dto.AzureParametersDto;
import com.sequenceiq.environment.parameter.dto.AzureResourceGroupDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto;
import com.sequenceiq.environment.parameter.dto.ResourceGroupUsagePattern;

@Component
public class AzurePrivateEndpointValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzurePrivateEndpointValidator.class);

    private final AzureCloudSubnetParametersService azureCloudSubnetParametersService;

    private final CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    private final AzureClientService azureClientService;

    private final AzureExistingPrivateDnsZoneValidatorService azureExistingPrivateDnsZoneValidatorService;

    private final AzureNewPrivateDnsZoneValidatorService azureNewPrivateDnsZoneValidatorService;

    private final AzureExistingPrivateDnsZonesService azureExistingPrivateDnsZonesService;

    public AzurePrivateEndpointValidator(AzureCloudSubnetParametersService azureCloudSubnetParametersService,
            CredentialToCloudCredentialConverter credentialToCloudCredentialConverter, AzureClientService azureClientService,
            AzureExistingPrivateDnsZoneValidatorService azureExistingPrivateDnsZoneValidatorService,
            AzureNewPrivateDnsZoneValidatorService azureNewPrivateDnsZoneValidatorService,
            AzureExistingPrivateDnsZonesService azureExistingPrivateDnsZonesService) {
        this.azureCloudSubnetParametersService = azureCloudSubnetParametersService;
        this.credentialToCloudCredentialConverter = credentialToCloudCredentialConverter;
        this.azureClientService = azureClientService;
        this.azureExistingPrivateDnsZoneValidatorService = azureExistingPrivateDnsZoneValidatorService;
        this.azureNewPrivateDnsZoneValidatorService = azureNewPrivateDnsZoneValidatorService;
        this.azureExistingPrivateDnsZonesService = azureExistingPrivateDnsZonesService;
    }

    public void checkNetworkPoliciesWhenExistingNetwork(
            NetworkDto networkDto, Map<String, CloudSubnet> cloudNetworks, ValidationResult.ValidationResultBuilder resultBuilder) {
        if (!networkDto.isPrivateEndpointEnabled(CloudPlatform.AZURE)) {
            LOGGER.debug("No private endpoint network policies validation requested");
            return;
        }

        if (RegistrationType.CREATE_NEW == networkDto.getRegistrationType()) {
            LOGGER.debug("Using new network -- bypassing private endpoint network policies validation");
            return;
        }

        boolean noSuitableSubnetPresent = cloudNetworks.values().stream().noneMatch(azureCloudSubnetParametersService::isPrivateEndpointNetworkPoliciesDisabled);
        if (noSuitableSubnetPresent) {
            String subnetsInVnet = cloudNetworks.values().stream().map(CloudSubnet::getName).collect(Collectors.joining(", "));
            String errorMessage = String.format("It is not possible to create private endpoints for existing network with id '%s' in resource group '%s': " +
                            "Azure requires at least one subnet with private endpoint network policies (eg. NSGs) disabled.  Please disable private endpoint " +
                            "network policies in at least one of the following subnets and retry: '%s'. Refer to Microsoft documentation at: " +
                            "https://docs.microsoft.com/en-us/azure/private-link/disable-private-endpoint-network-policy",
                    networkDto.getNetworkId(), networkDto.getAzure().getResourceGroupName(), subnetsInVnet);
            addValidationError(errorMessage, resultBuilder);
        }
    }

    public void checkMultipleResourceGroup(ValidationResult.ValidationResultBuilder resultBuilder, EnvironmentDto environmentDto,
            NetworkDto networkDto) {
        ResourceGroupUsagePattern resourceGroupUsagePattern = getResourceGroupUsagePattern(environmentDto);
        if (resourceGroupUsagePattern == ResourceGroupUsagePattern.USE_MULTIPLE && networkDto.isPrivateEndpointEnabled(CloudPlatform.AZURE)) {
            addValidationError("Private endpoint creation is not supported for multiple resource group deployment model, "
                    + "please use the single resource group deployment model to be able to use private endpoints on Azure!", resultBuilder);
        }
    }

    public void checkExistingManagedPrivateDnsZone(ValidationResult.ValidationResultBuilder resultBuilder, EnvironmentDto environmentDto,
            NetworkDto networkDto) {
        if (azureExistingPrivateDnsZonesService.hasNoExistingManagedZones(networkDto)) {
            LOGGER.debug("No existing private DNS zones are used, nothing to do.");
            return;
        }
        boolean hasFlexibleServerSubnets = CollectionUtils.isNotEmpty(networkDto.getAzure().getFlexibleServerSubnetIds());
        if (hasFlexibleServerSubnets) {
            // TODO: Create proper validation
            LOGGER.debug("Skipping validation of DNS Zone in case of Azure Flexible Server");
            return;
        }

        if (!networkDto.isPrivateEndpointEnabled(CloudPlatform.AZURE)) {
            addValidationError("A private DNS zone is provided, but private endpoint creation is turned off. Please either turn on private endpoint " +
                    "creation or do not specify the existing private DNS zone.", resultBuilder);
        } else {
            CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(environmentDto.getCredential());
            AzureClient azureClient = azureClientService.getClient(cloudCredential);
            azureExistingPrivateDnsZoneValidatorService.validate(azureClient, networkDto.getAzure().getResourceGroupName(), networkDto.getAzure().getNetworkId(),
                    azureExistingPrivateDnsZonesService.getExistingManagedZonesAsDescriptors(networkDto), resultBuilder);
        }
    }

    public void checkExistingRegisteredOnlyPrivateDnsZone(ValidationResult.ValidationResultBuilder resultBuilder, EnvironmentDto environmentDto,
            NetworkDto networkDto) {
        if (azureExistingPrivateDnsZonesService.hasNoExistingRegisteredOnlyZones(networkDto)) {
            LOGGER.debug("No existing private DNS zones are used, nothing to do.");
            return;
        }

        CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(environmentDto.getCredential());
        AzureClient azureClient = azureClientService.getClient(cloudCredential);
        azureExistingPrivateDnsZoneValidatorService.validate(azureClient, networkDto.getAzure().getResourceGroupName(), networkDto.getAzure().getNetworkId(),
                azureExistingPrivateDnsZonesService.getExistingRegisteredOnlyZonesAsDescriptors(networkDto), resultBuilder);

    }

    public void checkNewPrivateDnsZone(ValidationResult.ValidationResultBuilder resultBuilder, EnvironmentDto environmentDto, NetworkDto networkDto) {
        if (networkDto.isPrivateEndpointEnabled(CloudPlatform.AZURE) && ResourceGroupUsagePattern.USE_MULTIPLE != getResourceGroupUsagePattern(environmentDto)) {
            CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(environmentDto.getCredential());
            AzureClient azureClient = azureClientService.getClient(cloudCredential);
            Optional<String> resourceGroupName = getAzureResourceGroupDto(environmentDto)
                    .map(AzureResourceGroupDto::getName);
            resourceGroupName.ifPresent(rgName -> azureNewPrivateDnsZoneValidatorService.zonesNotConnectedToNetwork(azureClient,
                    networkDto.getAzure().getNetworkId(), rgName, azureExistingPrivateDnsZonesService.getServicesWithExistingManagedZones(networkDto),
                    resultBuilder));
        }
    }

    private ResourceGroupUsagePattern getResourceGroupUsagePattern(EnvironmentDto environmentDto) {
        return getAzureResourceGroupDto(environmentDto)
                .map(AzureResourceGroupDto::getResourceGroupUsagePattern)
                .orElse(ResourceGroupUsagePattern.USE_MULTIPLE);
    }

    private Optional<AzureResourceGroupDto> getAzureResourceGroupDto(EnvironmentDto environmentDto) {
        return Optional.ofNullable(environmentDto.getParameters())
                .map(ParametersDto::azureParametersDto)
                .map(AzureParametersDto::getAzureResourceGroupDto);
    }

    private void addValidationError(String message, ValidationResult.ValidationResultBuilder resultBuilder) {
        LOGGER.warn(message);
        resultBuilder.error(message);
    }

}