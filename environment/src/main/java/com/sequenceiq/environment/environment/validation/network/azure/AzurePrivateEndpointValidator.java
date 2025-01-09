package com.sequenceiq.environment.environment.validation.network.azure;

import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.azure.AzureCloudSubnetParametersService;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClientService;
import com.sequenceiq.cloudbreak.cloud.azure.validator.privatedns.AzureExistingPrivateDnsZoneValidatorService;
import com.sequenceiq.cloudbreak.cloud.azure.validator.privatedns.AzureNewPrivateDnsZoneValidatorService;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.validation.ValidationType;
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

    public void checkExistingDnsZoneDeletion(ValidationType validationType, String originalZoneId, String newZoneId, ValidationResultBuilder resultBuilder) {
        if (validationType == ValidationType.ENVIRONMENT_EDIT && StringUtils.isNotEmpty(originalZoneId) && StringUtils.isEmpty(newZoneId)) {
            String message = "Deletion of existing dns zone id is not a valid operation";
            addValidationError(message, resultBuilder);
        }
    }

    public void checkMultipleResourceGroup(ValidationResultBuilder resultBuilder, EnvironmentDto environmentDto,
            NetworkDto networkDto) {
        ResourceGroupUsagePattern resourceGroupUsagePattern = getResourceGroupUsagePattern(environmentDto);
        if (resourceGroupUsagePattern == ResourceGroupUsagePattern.USE_MULTIPLE && networkDto.isPrivateEndpointEnabled(CloudPlatform.AZURE)) {
            addValidationError("Private endpoint creation is not supported for multiple resource group deployment model, "
                    + "please use the single resource group deployment model to be able to use private endpoints on Azure!", resultBuilder);
        }
    }

    public void checkExistingManagedPrivateDnsZone(ValidationResultBuilder resultBuilder, EnvironmentDto environmentDto,
            NetworkDto networkDto) {
        if (azureExistingPrivateDnsZonesService.hasNoExistingManagedZones(networkDto)) {
            LOGGER.debug("Existing private DNS zones are not being used, nothing to do.");
            return;
        }
        boolean hasFlexibleServerSubnets = CollectionUtils.isNotEmpty(networkDto.getAzure().getFlexibleServerSubnetIds());
        boolean hasPrivateEndpointEnabled = networkDto.isPrivateEndpointEnabled(CloudPlatform.AZURE);

        if (!hasFlexibleServerSubnets && !hasPrivateEndpointEnabled) {
            addValidationError("A private DNS zone is provided, but private endpoint creation is turned off and no Flexible server delegated subnet is "
                    + "specified. Please specify exactly one of them or do not specify the existing private DNS zone.", resultBuilder);
        } else {
            CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(environmentDto.getCredential());
            AzureClient azureClient = azureClientService.getClient(cloudCredential);
            azureExistingPrivateDnsZoneValidatorService.validate(azureClient, networkDto.getAzure().getResourceGroupName(), networkDto.getAzure().getNetworkId(),
                    azureExistingPrivateDnsZonesService.getExistingManagedZonesAsDescriptors(networkDto), resultBuilder);
        }
    }

    public void checkExistingRegisteredOnlyPrivateDnsZone(ValidationResultBuilder resultBuilder, EnvironmentDto environmentDto,
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

    public void checkNewPrivateDnsZone(ValidationResultBuilder resultBuilder, EnvironmentDto environmentDto, NetworkDto networkDto) {
        if (networkDto.isPrivateEndpointEnabled(CloudPlatform.AZURE) && ResourceGroupUsagePattern.USE_MULTIPLE != getResourceGroupUsagePattern(environmentDto)) {
            CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(environmentDto.getCredential());
            AzureClient azureClient = azureClientService.getClient(cloudCredential);
            Optional<String> resourceGroupName = getAzureResourceGroupDto(environmentDto)
                    .map(AzureResourceGroupDto::getName);
            resourceGroupName.ifPresent(rgName -> azureNewPrivateDnsZoneValidatorService.zonesNotConnectedToNetwork(azureClient,
                    networkDto.getAzure().getNetworkId(), rgName, azureExistingPrivateDnsZonesService.getServicesWithExistingManagedZones(networkDto),
                    networkDto.getPrivateDatabaseVariant(), resultBuilder));
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

    private void addValidationError(String message, ValidationResultBuilder resultBuilder) {
        LOGGER.warn(message);
        resultBuilder.error(message);
    }

}