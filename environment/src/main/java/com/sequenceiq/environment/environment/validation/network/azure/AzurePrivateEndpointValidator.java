package com.sequenceiq.environment.environment.validation.network.azure;

import static com.sequenceiq.common.api.type.ServiceEndpointCreation.ENABLED_PRIVATE_ENDPOINT;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.azure.AzureCloudSubnetParametersService;
import com.sequenceiq.cloudbreak.cloud.azure.AzureNetworkLinkService;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClientService;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.util.NullUtil;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.common.api.type.ServiceEndpointCreation;
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

    private final AzureNetworkLinkService azureNetworkLinkService;

    private final CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    private final AzureClientService azureClientService;

    public AzurePrivateEndpointValidator(AzureCloudSubnetParametersService azureCloudSubnetParametersService, AzureNetworkLinkService azureNetworkLinkService,
            CredentialToCloudCredentialConverter credentialToCloudCredentialConverter, AzureClientService azureClientService) {
        this.azureCloudSubnetParametersService = azureCloudSubnetParametersService;
        this.azureNetworkLinkService = azureNetworkLinkService;
        this.credentialToCloudCredentialConverter = credentialToCloudCredentialConverter;
        this.azureClientService = azureClientService;
    }

    public void checkPrivateEndpointNetworkPoliciesWhenExistingNetwork(
            NetworkDto networkDto, Map<String, CloudSubnet> cloudNetworks, ValidationResult.ValidationResultBuilder resultBuilder) {
        if (!ENABLED_PRIVATE_ENDPOINT.equals(networkDto.getServiceEndpointCreation())) {
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
            LOGGER.warn(errorMessage);
            resultBuilder.error(errorMessage);
        }
    }

    public void checkPrivateEndpointsWhenMultipleResourceGroup(ValidationResult.ValidationResultBuilder resultBuilder, EnvironmentDto environmentDto,
            ServiceEndpointCreation serviceEndpointCreation) {
        ResourceGroupUsagePattern resourceGroupUsagePattern = getResourceGroupUsagePattern(environmentDto);
        if (resourceGroupUsagePattern == ResourceGroupUsagePattern.USE_MULTIPLE
                && serviceEndpointCreation == ServiceEndpointCreation.ENABLED_PRIVATE_ENDPOINT) {
            resultBuilder.error("Private endpoint creation is not supported for multiple resource group deployment model, "
                    + "please use single single resource groups to be able to use private endpoints in Azure!");
        }
    }

    public void checkExistingPrivateDnsZoneWhenNotPrivateEndpoint(ValidationResult.ValidationResultBuilder resultBuilder, NetworkDto networkDto) {
        if (StringUtils.isNotEmpty(networkDto.getAzure().getPrivateDnsZoneId())
                && networkDto.getServiceEndpointCreation() != ServiceEndpointCreation.ENABLED_PRIVATE_ENDPOINT) {
            resultBuilder.error("A private DNS zone is provided, but private endpoint creation is turned off. Please either turn on private endpoint creation"
                    + " or do not specify the existing private DNS zone.");
        }
    }

    public void checkPrivateEndpointForExistingNetworkLink(ValidationResult.ValidationResultBuilder resultBuilder, EnvironmentDto environmentDto,
            NetworkDto networkDto) {
        if (networkDto.getServiceEndpointCreation() == ServiceEndpointCreation.ENABLED_PRIVATE_ENDPOINT &&
                ResourceGroupUsagePattern.USE_MULTIPLE != getResourceGroupUsagePattern(environmentDto) &&
                StringUtils.isEmpty(networkDto.getAzure().getPrivateDnsZoneId())) {
            CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(environmentDto.getCredential());
            AzureClient azureClient = azureClientService.getClient(cloudCredential);
            Optional<String> resourceGroupName = getAzureResourceGroupDto(environmentDto)
                    .map(AzureResourceGroupDto::getName);
            resourceGroupName.ifPresent(rgName -> NullUtil.doIfNotNull(
                    azureNetworkLinkService.validateExistingNetworkLink(azureClient, networkDto.getAzure().getNetworkId(), rgName), resultBuilder::merge));
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

}