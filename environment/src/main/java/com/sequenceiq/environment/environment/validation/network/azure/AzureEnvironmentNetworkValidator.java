package com.sequenceiq.environment.environment.validation.network.azure;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;
import static com.sequenceiq.environment.environment.validation.ValidationType.ENVIRONMENT_CREATION;
import static com.sequenceiq.environment.environment.validation.ValidationType.ENVIRONMENT_EDIT;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.azure.AzureCloudSubnetParametersService;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.common.api.type.ServiceEndpointCreation;
import com.sequenceiq.environment.environment.domain.Region;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentValidationDto;
import com.sequenceiq.environment.environment.validation.ValidationType;
import com.sequenceiq.environment.environment.validation.network.EnvironmentNetworkValidator;
import com.sequenceiq.environment.network.CloudNetworkService;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.environment.network.dto.AzureParams;
import com.sequenceiq.environment.network.dto.NetworkDto;

@Component
public class AzureEnvironmentNetworkValidator implements EnvironmentNetworkValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureEnvironmentNetworkValidator.class);

    private final CloudNetworkService cloudNetworkService;

    private final AzurePrivateEndpointValidator azurePrivateEndpointValidator;

    private final AzureCloudSubnetParametersService azureCloudSubnetParametersService;

    @Value("${cb.multiaz.azure.availabilityZones}")
    private Set<String> azureAvailabilityZones;

    public AzureEnvironmentNetworkValidator(CloudNetworkService cloudNetworkService,
            AzurePrivateEndpointValidator azurePrivateEndpointValidator, AzureCloudSubnetParametersService azureCloudSubnetParametersService) {
        this.cloudNetworkService = cloudNetworkService;
        this.azurePrivateEndpointValidator = azurePrivateEndpointValidator;
        this.azureCloudSubnetParametersService = azureCloudSubnetParametersService;
    }

    @Override
    public void validateDuringFlow(EnvironmentValidationDto environmentValidationDto, NetworkDto networkDto, ValidationResultBuilder resultBuilder) {
        if (environmentValidationDto == null || environmentValidationDto.getEnvironmentDto() == null || networkDto == null) {
            LOGGER.warn("Neither EnvironmentDto nor NetworkDto could be null!");
            resultBuilder.error("Internal validation error");
            return;
        }
        EnvironmentDto environmentDto = environmentValidationDto.getEnvironmentDto();
        Map<String, CloudSubnet> cloudNetworks = cloudNetworkService.retrieveSubnetMetadata(environmentDto, networkDto);
        Region region = environmentDto.getRegions()
                .stream().findFirst()
                .orElseThrow();
        checkSubnetsProvidedWhenExistingNetwork(resultBuilder, "subnet IDs", networkDto.getSubnetIds(), networkDto.getAzure(), cloudNetworks, region);
        checkFlexibleServerSubnetIds(environmentValidationDto, networkDto, resultBuilder);
        if (CollectionUtils.isNotEmpty(networkDto.getEndpointGatewaySubnetIds())) {
            LOGGER.debug("Checking EndpointGatewaySubnetIds {}", networkDto.getEndpointGatewaySubnetIds());
            Map<String, CloudSubnet> endpointGatewayNetworks = cloudNetworkService.retrieveEndpointGatewaySubnetMetadata(environmentDto, networkDto);
            checkSubnetsProvidedWhenExistingNetwork(resultBuilder, "endpoint gateway subnet IDs",
                    networkDto.getEndpointGatewaySubnetIds(), networkDto.getAzure(), endpointGatewayNetworks, region);
        }
        checkPrivateDnsZoneId(environmentValidationDto, networkDto, resultBuilder, environmentDto, cloudNetworks);
    }

    @Override
    public void validateDuringRequest(EnvironmentValidationDto environmentValidationDto, NetworkDto networkDto, ValidationResultBuilder resultBuilder) {
        if (networkDto == null) {
            return;
        }
        checkNetworkIdIsPresent(networkDto, resultBuilder);
        AzureParams azureParams = networkDto.getAzure();
        if (azureParams != null) {
            checkSubnetsProvidedWhenExistingNetwork(resultBuilder, azureParams, networkDto.getSubnetMetas());
            checkExistingNetworkParamsProvidedWhenSubnetsPresent(networkDto, resultBuilder);
            checkResourceGroupNameWhenExistingNetwork(resultBuilder, azureParams);
            checkNetworkIdWhenExistingNetwork(resultBuilder, azureParams);
            checkNetworkIdIsSpecifiedWhenSubnetIdsArePresent(resultBuilder, azureParams, networkDto);
            validateAvailabilityZones(environmentValidationDto, resultBuilder, azureParams);
        } else if (StringUtils.isEmpty(networkDto.getNetworkCidr())) {
            resultBuilder.error(missingParamsErrorMsg(AZURE));
        }
    }

    @Override
    public void validateDuringRequest(NetworkDto networkDto, ValidationResultBuilder resultBuilder) {
        validateDuringRequest(null, networkDto, resultBuilder);
    }

    private void checkNetworkIdIsPresent(NetworkDto networkDto, ValidationResultBuilder resultBuilder) {
        if (networkDto.getAzure() == null || StringUtils.isEmpty(networkDto.getAzure().getNetworkId())) {
            String message = "Azure existing networkId needs to be defined, environment creation with new network is not supported.";
            LOGGER.info(message);
            resultBuilder.error(message);
        }
    }

    private void checkSubnetsProvidedWhenExistingNetwork(ValidationResultBuilder resultBuilder, String context,
            Set<String> subnetIds, AzureParams azureParams, Map<String, CloudSubnet> subnetMetas, Region region) {
        if (StringUtils.isNotEmpty(azureParams.getNetworkId()) && StringUtils.isNotEmpty(azureParams.getResourceGroupName())) {
            if (CollectionUtils.isEmpty(subnetIds)) {
                String message = String.format("If networkId (%s) and resourceGroupName (%s) are specified then %s must be specified as well.",
                        azureParams.getNetworkId(), azureParams.getResourceGroupName(), context);
                LOGGER.info(message);
                resultBuilder.error(message);
            } else if (subnetMetas.size() != subnetIds.size()) {
                String message = String.format("If networkId (%s) and resourceGroupName (%s) are specified then %s must be specified and should exist " +
                                "on Azure as well. Given %s: [%s], existing ones: [%s], in region: [%s]", azureParams.getNetworkId(),
                        azureParams.getResourceGroupName(), context, context,
                        subnetIds.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(", ")),
                        subnetMetas.keySet().stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(", ")), region.getDisplayName());
                LOGGER.info(message);
                resultBuilder.error(message);
            }
        }
    }

    private void validateNetworkId(EnvironmentValidationDto environmentValidationDto, NetworkDto networkDto, ValidationResultBuilder resultBuilder) {
        LOGGER.debug("About to validate Azure network using {}: {} and {}: {}", EnvironmentValidationDto.class.getSimpleName(), environmentValidationDto,
                NetworkDto.class.getSimpleName(), networkDto);
        NetworkDto network = networkDto != null ? networkDto : environmentValidationDto.getEnvironmentDto().getNetwork();
        if (network == null) {
            resultBuilder.error("Unable to validate network ID due to the lack of network parameters");
            return;
        }
        if (RegistrationType.EXISTING.equals(network.getRegistrationType())) {
            boolean foundOnProvider = cloudNetworkService.retrieveCloudNetworks(environmentValidationDto.getEnvironmentDto())
                    .stream()
                    .anyMatch(networkId -> networkId.equalsIgnoreCase(network.getNetworkId()));
            if (!foundOnProvider) {
                resultBuilder.error("Unable to find network on Azure with the following name: " + network.getNetworkId() +
                        ". Please double check the name/ID.");
            }
        }
    }

    private void checkResourceGroupNameWhenExistingNetwork(ValidationResultBuilder resultBuilder, AzureParams azureParams) {
        if (StringUtils.isNotEmpty(azureParams.getNetworkId()) && StringUtils.isEmpty(azureParams.getResourceGroupName())) {
            resultBuilder.error("If networkId is specified, then resourceGroupName must be specified too.");
        }
    }

    private void checkNetworkIdWhenExistingNetwork(ValidationResultBuilder resultBuilder, AzureParams azureParams) {
        if (StringUtils.isEmpty(azureParams.getNetworkId()) && StringUtils.isNotEmpty(azureParams.getResourceGroupName())) {
            resultBuilder.error("If resourceGroupName is specified, then networkId must be specified too.");
        }
    }

    private void checkExistingNetworkParamsProvidedWhenSubnetsPresent(NetworkDto networkDto, ValidationResultBuilder resultBuilder) {
        if ((CollectionUtils.isNotEmpty(networkDto.getSubnetIds()) || CollectionUtils.isNotEmpty(networkDto.getEndpointGatewaySubnetIds()))
                && StringUtils.isEmpty(networkDto.getAzure().getNetworkId())
                && StringUtils.isEmpty(networkDto.getAzure().getResourceGroupName())) {
            String message =
                    String.format("If %s subnet IDs were provided then network id and resource group name have to be specified, too.", AZURE.name());
            LOGGER.info(message);
            resultBuilder.error(message);
        }
    }

    private void checkNetworkIdIsSpecifiedWhenSubnetIdsArePresent(ValidationResultBuilder resultBuilder,
            AzureParams azureParams, NetworkDto networkDto) {
        if (StringUtils.isEmpty(azureParams.getNetworkId()) && CollectionUtils.isNotEmpty(networkDto.getSubnetIds())) {
            resultBuilder.error("If subnetIds are specified, then networkId must be specified too.");
        }
    }

    private void checkSubnetsProvidedWhenExistingNetwork(ValidationResultBuilder resultBuilder,
            AzureParams azureParams, Map<String, CloudSubnet> subnetMetas) {
        if (StringUtils.isNotEmpty(azureParams.getNetworkId()) && StringUtils.isNotEmpty(azureParams.getResourceGroupName())
                && MapUtils.isEmpty(subnetMetas)) {
            String message = String.format("If networkId (%s) and resourceGroupName (%s) are specified then subnet ids must be specified as well.",
                    azureParams.getNetworkId(), azureParams.getResourceGroupName());
            LOGGER.info(message);
            resultBuilder.error(message);
        }
    }

    private void validateAvailabilityZones(EnvironmentValidationDto environmentValidationDto, ValidationResultBuilder resultBuilder, AzureParams azureParams) {
        if (CollectionUtils.isNotEmpty(azureParams.getAvailabilityZones())) {
            LOGGER.debug("Availability zones are {}", azureParams.getAvailabilityZones());
            boolean allZonesValid = checkInvalidAvailabilityZones(azureParams, resultBuilder);
            if (allZonesValid) {
                Set<String> existingAvailabilityZones = getAvailabilityZones(environmentValidationDto);
                if (CollectionUtils.isNotEmpty(existingAvailabilityZones)) {
                    if (!CollectionUtils.containsAll(azureParams.getAvailabilityZones(), existingAvailabilityZones)) {
                        String message = String.format("Provided Availability Zones for environment do not contain the existing Availability Zones. " +
                                        "Provided Availability Zones : %s. Existing Availability Zones : %s", azureParams.getAvailabilityZones()
                                        .stream().sorted().collect(Collectors.joining(",")),
                                existingAvailabilityZones.stream().sorted().collect(Collectors.joining(",")));
                        LOGGER.info(message);
                        resultBuilder.error(message);
                    }
                }
            }
        }
    }

    private Set<String> getAvailabilityZones(EnvironmentValidationDto environmentValidationDto) {
        Set<String> availabilityZones = null;
        if (environmentValidationDto != null && environmentValidationDto.getEnvironmentDto() != null
                && environmentValidationDto.getEnvironmentDto().getNetwork() != null
                && environmentValidationDto.getEnvironmentDto().getNetwork().getAzure() != null) {
            availabilityZones = environmentValidationDto.getEnvironmentDto().getNetwork().getAzure().getAvailabilityZones();
        }
        return availabilityZones;
    }

    private boolean checkInvalidAvailabilityZones(AzureParams azureParams, ValidationResultBuilder resultBuilder) {
        Set<String> invalidAvailabilityZones = azureParams.getAvailabilityZones().stream().filter(az -> !azureAvailabilityZones.contains(az))
                .collect(Collectors.toSet());
        if (CollectionUtils.isNotEmpty(invalidAvailabilityZones)) {
            String message = String.format("Availability zones %s are not valid. Valid availability zones are %s.",
                    invalidAvailabilityZones.stream().sorted().collect(Collectors.joining(",")),
                    azureAvailabilityZones.stream().sorted().collect(Collectors.joining(",")));
            LOGGER.info(message);
            resultBuilder.error(message);
            return false;
        } else {
            return true;
        }
    }

    private void checkFlexibleServerSubnetIds(EnvironmentValidationDto environmentValidationDto, NetworkDto networkDto, ValidationResultBuilder resultBuilder) {
        EnvironmentDto environmentDto = environmentValidationDto.getEnvironmentDto();
        Set<String> originalFlexibleSubnets = Optional.ofNullable(environmentDto.getNetwork())
                .map(NetworkDto::getAzure)
                .map(AzureParams::getFlexibleServerSubnetIds)
                .orElse(Set.of());
        Set<String> newFlexibleSubnets = Optional.ofNullable(networkDto.getAzure())
                .map(AzureParams::getFlexibleServerSubnetIds)
                .orElse(Set.of());
        if (environmentValidationDto.getValidationType() == ENVIRONMENT_EDIT
                && originalFlexibleSubnets.equals(newFlexibleSubnets)) {
            LOGGER.info("Flexible server subnet validation is not needed during environment edit as subnet ids has not changed.");
        } else if (environmentValidationDto.getValidationType() == ENVIRONMENT_EDIT
                && !originalFlexibleSubnets.isEmpty()
                && newFlexibleSubnets.isEmpty()
                && networkDto.getServiceEndpointCreation() != ServiceEndpointCreation.ENABLED_PRIVATE_ENDPOINT) {
            String message = "Deletion of all Flexible server delegated subnets is not supported";
            LOGGER.warn(message);
            resultBuilder.error(message);
        } else if (!newFlexibleSubnets.isEmpty()) {
            checkPrivateEndpointSetting(networkDto, resultBuilder);
            Set<String> flexibleServerSubnetIds = convertFlexibleServerSubnetIds(newFlexibleSubnets);
            Map<String, CloudSubnet> flexibleSubnets = cloudNetworkService.getSubnetMetadata(environmentDto, networkDto, flexibleServerSubnetIds);
            if (flexibleSubnets.size() != flexibleServerSubnetIds.size()) {
                String message = String.format("The following flexible server delegated subnets are not found on the provider side: %s",
                        newFlexibleSubnets.stream()
                                .filter(subnetId -> !flexibleSubnets.containsKey(convertFlexibleServerSubnetId(subnetId)))
                                .collect(Collectors.joining(",")));
                LOGGER.warn(message);
                resultBuilder.error(message);
            } else {
                String invalidSubnets = flexibleSubnets.entrySet().stream()
                        .filter(entry -> !azureCloudSubnetParametersService.isFlexibleServerDelegatedSubnet(entry.getValue()))
                        .map(Entry::getKey)
                        .collect(Collectors.joining(","));
                if (StringUtils.isNotEmpty(invalidSubnets)) {
                    String message = String.format("The following subnets are not delegated to flexible servers: %s", invalidSubnets);
                    LOGGER.warn(message);
                    resultBuilder.error(message);
                }
            }
        }
    }

    private void checkPrivateEndpointSetting(NetworkDto networkDto, ValidationResultBuilder resultBuilder) {
        if (networkDto.isPrivateEndpointEnabled(AZURE)) {
            String message = "Both Private Endpoint and Flexible Server delegated subnet(s) are specified in the request. " +
                    "As they are mutually exclusive, please specify only one of them and retry.";
            LOGGER.warn(message);
            resultBuilder.error(message);
        }
    }

    private Set<String> convertFlexibleServerSubnetIds(Set<String> flexibleServerSubnetIds) {
        return flexibleServerSubnetIds.stream()
                .map(this::convertFlexibleServerSubnetId)
                .collect(Collectors.toSet());
    }

    private String convertFlexibleServerSubnetId(String subnetId) {
        if (subnetId.contains("/")) {
            return StringUtils.substringAfterLast(subnetId, "/");
        } else {
            return subnetId;
        }
    }

    private void checkPrivateDnsZoneId(EnvironmentValidationDto environmentValidationDto, NetworkDto networkDto, ValidationResultBuilder resultBuilder,
            EnvironmentDto environmentDto, Map<String, CloudSubnet> cloudNetworks) {
        ValidationType validationType = environmentValidationDto.getValidationType();
        String originalDnsZone = Optional.ofNullable(environmentValidationDto.getEnvironmentDto())
                .map(EnvironmentDto::getNetwork)
                .map(NetworkDto::getAzure)
                .map(AzureParams::getDatabasePrivateDnsZoneId)
                .orElse(null);
        String newDnsZone = Optional.ofNullable(networkDto.getAzure())
                .map(AzureParams::getDatabasePrivateDnsZoneId)
                .orElse(null);
        if (validationType == ENVIRONMENT_CREATION ||
                (validationType == ENVIRONMENT_EDIT && !StringUtils.equals(originalDnsZone, newDnsZone))) {
            LOGGER.debug("Dns zone validation: validation type: {}, originalDnsZone: {}, newDnsZone {}", validationType, originalDnsZone, newDnsZone);
            azurePrivateEndpointValidator.checkExistingDnsZoneDeletion(validationType, originalDnsZone, newDnsZone, resultBuilder);
            validateNetworkId(environmentValidationDto, networkDto, resultBuilder);
            azurePrivateEndpointValidator.checkMultipleResourceGroup(resultBuilder, environmentDto, networkDto);
            azurePrivateEndpointValidator.checkExistingManagedPrivateDnsZone(resultBuilder, environmentDto, networkDto);
            azurePrivateEndpointValidator.checkNewPrivateDnsZone(resultBuilder, environmentDto, networkDto);
            azurePrivateEndpointValidator.checkExistingRegisteredOnlyPrivateDnsZone(resultBuilder, environmentDto, networkDto);
            LOGGER.debug("Dns zone validation finished");
        } else {
            LOGGER.debug("Skipping Private Dns Zone related validations as it is not changed during environment edit.");
        }
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return AZURE;
    }

}