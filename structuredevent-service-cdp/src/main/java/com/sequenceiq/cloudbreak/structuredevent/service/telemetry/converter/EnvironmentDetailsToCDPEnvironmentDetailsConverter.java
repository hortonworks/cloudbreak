package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.EnvironmentDetails;
import com.sequenceiq.environment.environment.domain.Region;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.parameter.dto.AzureParametersDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto;

@Component
public class EnvironmentDetailsToCDPEnvironmentDetailsConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentDetailsToCDPEnvironmentDetailsConverter.class);

    private static final int DEFAULT_INTEGER_VALUE = -1;

    @Inject
    private EnvironmentDetailsToCDPNetworkDetailsConverter networkDetailsConverter;

    public UsageProto.CDPEnvironmentDetails convert(EnvironmentDetails srcEnvironmentDetails) {
        UsageProto.CDPEnvironmentDetails.Builder cdpEnvironmentDetails = UsageProto.CDPEnvironmentDetails.newBuilder();
        cdpEnvironmentDetails.setNumberOfAvailabilityZones(DEFAULT_INTEGER_VALUE);

        if (srcEnvironmentDetails != null) {
            if (srcEnvironmentDetails.getRegions() != null) {
                cdpEnvironmentDetails.setRegion(srcEnvironmentDetails.getRegions().stream()
                        .map(Region::getName)
                        .filter(Objects::nonNull)
                        .map(region -> region.toLowerCase().replace(" ", ""))
                        .sorted()
                        .distinct()
                        .collect(Collectors.joining(",")));
            }

            if (srcEnvironmentDetails.getCloudPlatform() != null) {
                cdpEnvironmentDetails.setEnvironmentType(UsageProto.CDPEnvironmentsEnvironmentType
                        .Value.valueOf(srcEnvironmentDetails.getCloudPlatform()));
            }

            NetworkDto network = srcEnvironmentDetails.getNetwork();
            if (network != null && network.getSubnetMetas() != null) {
                if (network.getSubnetMetas().isEmpty()) {
                    cdpEnvironmentDetails.setNumberOfAvailabilityZones(0);
                } else {
                    List<String> availabilityZones = network.getSubnetMetas().values()
                            .stream()
                            .map(CloudSubnet::getAvailabilityZone)
                            .filter(Objects::nonNull)
                            .sorted()
                            .distinct()
                            .collect(Collectors.toUnmodifiableList());
                    if (!availabilityZones.isEmpty()) {
                        cdpEnvironmentDetails.setNumberOfAvailabilityZones(availabilityZones.size());
                        cdpEnvironmentDetails.setAvailabilityZones(String.join(",", availabilityZones));
                    }
                }
            }
            cdpEnvironmentDetails.setNetworkDetails(networkDetailsConverter.convert(srcEnvironmentDetails));

            cdpEnvironmentDetails.setAwsDetails(convertAwsDetails(srcEnvironmentDetails.getParameters()));
            cdpEnvironmentDetails.setAzureDetails(convertAzureDetails(srcEnvironmentDetails.getParameters()));

            Map<String, String> userTags = srcEnvironmentDetails.getUserDefinedTags();
            if (userTags != null && !userTags.isEmpty()) {
                cdpEnvironmentDetails.setUserTags(JsonUtil.writeValueAsStringSilentSafe(userTags));
            }
        }

        UsageProto.CDPEnvironmentDetails ret = cdpEnvironmentDetails.build();
        LOGGER.debug("Converted CDPEnvironmentDetails event: {}", ret);
        return ret;
    }

    private UsageProto.CDPEnvironmentAwsDetails convertAwsDetails(ParametersDto parametersDto) {
        UsageProto.CDPEnvironmentAwsDetails.Builder builder = UsageProto.CDPEnvironmentAwsDetails.newBuilder();
        return builder.build();
    }

    private UsageProto.CDPEnvironmentAzureDetails convertAzureDetails(ParametersDto parametersDto) {
        UsageProto.CDPEnvironmentAzureDetails.Builder builder = UsageProto.CDPEnvironmentAzureDetails.newBuilder();
        if (parametersDto != null) {
            AzureParametersDto azureParametersDto = parametersDto.getAzureParametersDto();
            if (azureParametersDto != null) {
                builder.setSingleResourceGroup(
                        azureParametersDto.getAzureResourceGroupDto().getResourceGroupUsagePattern().isSingleResourceGroup());
            }
        }
        return builder.build();
    }
}
