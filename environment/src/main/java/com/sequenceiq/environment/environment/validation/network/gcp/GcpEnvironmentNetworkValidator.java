package com.sequenceiq.environment.environment.validation.network.gcp;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.GCP;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentValidationDto;
import com.sequenceiq.environment.environment.validation.network.EnvironmentNetworkValidator;
import com.sequenceiq.environment.network.CloudNetworkService;
import com.sequenceiq.environment.network.dto.GcpParams;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.platformresource.PlatformParameterService;
import com.sequenceiq.environment.platformresource.PlatformResourceRequest;

@Component
public class GcpEnvironmentNetworkValidator implements EnvironmentNetworkValidator {

    private static final String MULTIPLE_AVAILABILITY_ZONES_PROVIDED_ERROR_MSG = "The multiple availability zones feature isn't available for GCP yet. "
            + "Please configure only one zone on field 'availabilityZones'";

    private static final String INVALID_ZONE_PATTERN = "The requested region '%s' doesn't contain the requested '%s' availability zone(s), "
            + "available zones: '%s'";

    private static final String INVALID_REGION_PATTERN = "The environment's requested region '%s' doesn't exist on GCP side.";

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpEnvironmentNetworkValidator.class);

    private final CloudNetworkService cloudNetworkService;

    private final PlatformParameterService platformParameterService;

    public GcpEnvironmentNetworkValidator(CloudNetworkService cloudNetworkService, PlatformParameterService platformParameterService) {
        this.cloudNetworkService = cloudNetworkService;
        this.platformParameterService = platformParameterService;
    }

    @Override
    public void validateDuringFlow(EnvironmentValidationDto environmentValidationDto, NetworkDto networkDto,
            ValidationResult.ValidationResultBuilder resultBuilder) {
        if (environmentValidationDto == null || environmentValidationDto.getEnvironmentDto() == null || networkDto == null) {
            LOGGER.warn("Neither EnvironmentDto nor NetworkDto could be null!");
            resultBuilder.error("Internal validation error");
            return;
        }

        EnvironmentDto environmentDto = environmentValidationDto.getEnvironmentDto();
        checkSubnetsProvidedWhenExistingNetwork(resultBuilder, "subnet IDs",
                networkDto.getSubnetIds(), networkDto.getGcp(),
                cloudNetworkService.retrieveSubnetMetadata(environmentDto, networkDto));
        if (CollectionUtils.isNotEmpty(networkDto.getEndpointGatewaySubnetIds())) {
            LOGGER.debug("Checking EndpointGatewaySubnetIds {}", networkDto.getEndpointGatewaySubnetIds());
            checkSubnetsProvidedWhenExistingNetwork(resultBuilder, "endpoint gateway subnet IDs",
                    networkDto.getEndpointGatewaySubnetIds(), networkDto.getGcp(),
                    cloudNetworkService.retrieveEndpointGatewaySubnetMetadata(environmentDto, networkDto));
        }
        checkAvailabilityZones(resultBuilder, environmentDto, networkDto);
    }

    @Override
    public void validateDuringRequest(NetworkDto networkDto, ValidationResult.ValidationResultBuilder resultBuilder) {
        if (networkDto == null) {
            return;
        }

        if (StringUtils.isEmpty(networkDto.getNetworkCidr()) && StringUtils.isEmpty(networkDto.getNetworkId())) {
            String message = "Either the GCP network id or cidr needs to be defined!";
            LOGGER.info(message);
            resultBuilder.error(message);
        }

        GcpParams gcpParams = networkDto.getGcp();
        if (gcpParams != null) {
            checkSubnetsProvidedWhenExistingNetwork(resultBuilder, gcpParams, networkDto.getSubnetMetas());
            checkExistingNetworkParamsProvidedWhenSubnetsPresent(networkDto, resultBuilder);
            checkNetworkIdIsSpecifiedWhenSubnetIdsArePresent(resultBuilder, gcpParams, networkDto);
            checkAvailabilityZones(resultBuilder, gcpParams);
        } else if (StringUtils.isEmpty(networkDto.getNetworkCidr())) {
            resultBuilder.error(missingParamsErrorMsg(GCP));
        }
    }

    private void checkSubnetsProvidedWhenExistingNetwork(ValidationResult.ValidationResultBuilder resultBuilder, String context,
            Set<String> subnetIds, GcpParams gcpParams, Map<String, CloudSubnet> subnetMetas) {
        if (StringUtils.isNotEmpty(gcpParams.getNetworkId())) {
            if (CollectionUtils.isEmpty(subnetIds)) {
                String message = String.format("If networkId (%s) is given then %s must exist on GCP as well.",
                        gcpParams.getNetworkId(), context);
                LOGGER.info(message);
                resultBuilder.error(message);
            } else if (subnetMetas.size() != subnetIds.size()) {
                String message = String.format("If networkId (%s) is given then %s must be specified and must exist on GCP as well. " +
                                "Given %s: [%s], exisiting ones: [%s]", gcpParams.getNetworkId(), context, context,
                        subnetIds.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(", ")),
                        subnetMetas.keySet().stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(", ")));
                LOGGER.info(message);
                resultBuilder.error(message);
            }
        }
    }

    private void checkExistingNetworkParamsProvidedWhenSubnetsPresent(NetworkDto networkDto, ValidationResult.ValidationResultBuilder resultBuilder) {
        if ((CollectionUtils.isNotEmpty(networkDto.getSubnetIds()) || CollectionUtils.isNotEmpty(networkDto.getEndpointGatewaySubnetIds()))
                && StringUtils.isEmpty(networkDto.getGcp().getNetworkId())) {
            String message =
                    String.format("If %s subnet ids were provided then network id have to be specified, too.", GCP.name());
            LOGGER.info(message);
            resultBuilder.error(message);
        }
    }

    private void checkNetworkIdIsSpecifiedWhenSubnetIdsArePresent(ValidationResult.ValidationResultBuilder resultBuilder,
            GcpParams gcpParams, NetworkDto networkDto) {
        if (StringUtils.isEmpty(gcpParams.getNetworkId()) && CollectionUtils.isNotEmpty(networkDto.getSubnetIds())) {
            resultBuilder.error("If subnetIds are given, then networkId must be specified too.");
        }
    }

    private void checkSubnetsProvidedWhenExistingNetwork(ValidationResult.ValidationResultBuilder resultBuilder,
        GcpParams gcpParams, Map<String, CloudSubnet> subnetMetas) {
        if (StringUtils.isNotEmpty(gcpParams.getNetworkId())
                && MapUtils.isEmpty(subnetMetas)) {
            String message = String.format("If networkId (%s) is given then subnet ids must be specified as well.",
                    gcpParams.getNetworkId());
            LOGGER.info(message);
            resultBuilder.error(message);
        }
    }

    private void checkAvailabilityZones(ValidationResult.ValidationResultBuilder resultBuilder, GcpParams gcpParams) {
        Set<String> availabilityZones = gcpParams.getAvailabilityZones();
        if (CollectionUtils.isNotEmpty(availabilityZones) && availabilityZones.size() > 1) {
            LOGGER.info(MULTIPLE_AVAILABILITY_ZONES_PROVIDED_ERROR_MSG);
            resultBuilder.error(MULTIPLE_AVAILABILITY_ZONES_PROVIDED_ERROR_MSG);
        }
    }

    private void checkAvailabilityZones(ValidationResult.ValidationResultBuilder resultBuilder, EnvironmentDto environmentDto, NetworkDto networkDto) {
        PlatformResourceRequest platformResourceRequest = new PlatformResourceRequest();
        platformResourceRequest.setCredential(environmentDto.getCredential());
        platformResourceRequest.setCloudPlatform(environmentDto.getCloudPlatform());
        CloudRegions regionsByCredential = platformParameterService.getRegionsByCredential(platformResourceRequest, true);
        Map<Region, List<AvailabilityZone>> cloudRegions = regionsByCredential.getCloudRegions();
        Region requestedRegion = Region.region(environmentDto.getLocation().getName());

        if (networkDto.getGcp() != null && CollectionUtils.isNotEmpty(networkDto.getGcp().getAvailabilityZones()) && MapUtils.isNotEmpty(cloudRegions)) {
            Set<AvailabilityZone> requestedZones = networkDto.getGcp().getAvailabilityZones()
                    .stream()
                    .map(AvailabilityZone::availabilityZone)
                    .collect(Collectors.toSet());
            if (cloudRegions.containsKey(requestedRegion)) {
                List<AvailabilityZone> zonesInRegion = cloudRegions.get(requestedRegion);
                Set<String> invalidZones = requestedZones.stream()
                        .filter(Predicate.not(zonesInRegion::contains))
                        .map(AvailabilityZone::value)
                        .collect(Collectors.toSet());
                if (CollectionUtils.isNotEmpty(zonesInRegion) && !invalidZones.isEmpty()) {
                    String msg = String.format(INVALID_ZONE_PATTERN,
                            requestedRegion.getRegionName(),
                            String.join(",", invalidZones),
                            zonesInRegion.stream().map(AvailabilityZone::value).collect(Collectors.joining(",")));
                    LOGGER.info(msg);
                    resultBuilder.error(msg);
                }
            } else {
                String errorMessage = String.format(INVALID_REGION_PATTERN, requestedRegion.getRegionName());
                LOGGER.warn(errorMessage);
                resultBuilder.error(errorMessage);
            }

        }
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return GCP;
    }
}
