package com.sequenceiq.environment.environment.validation.validators;

import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;

@Component
public class EnvironmentRegionValidator {

    public ValidationResultBuilder validateRegions(Set<String> requestedRegions, CloudRegions cloudRegions, String cloudPlatform) {
        ValidationResultBuilder resultBuilder = ValidationResult.builder();
        if (cloudRegions.areRegionsSupported()) {
            validateRegionsWhereSupported(requestedRegions, cloudRegions.getRegionNames(), resultBuilder, cloudPlatform);
        } else if (!requestedRegions.isEmpty()) {
            resultBuilder.error(String.format("Regions are not supporeted on cloudprovider: [%s].", cloudPlatform));
        }
        return resultBuilder;
    }

    public ValidationResultBuilder validateLocation(String requestedLocation, Set<String> requestedRegions,
        CloudRegions cloudRegions, String cloudPlatform) {
        ValidationResultBuilder resultBuilder = ValidationResult.builder();
        Set<String> requestedRegionNames = new HashSet<>(requestedRegions);
        if (!requestedRegionNames.contains(requestedLocation) && cloudRegions.areRegionsSupported()) {
            if (!cloudPlatform.equalsIgnoreCase(CloudConstants.OPENSTACK) && !cloudPlatform.equalsIgnoreCase(CloudConstants.MOCK)) {
                resultBuilder.error(String.format("Location [%s] is not one of the regions: [%s].", requestedLocation,
                        String.join(", ", requestedRegionNames)));
            }
        }
        return resultBuilder;
    }

    private void validateRegionsWhereSupported(Set<String> requestedRegions, Set<String> supportedRegions, ValidationResultBuilder resultBuilder,
            String cloudPlatform) {
        Set<String> requestedRegionNames = new HashSet<>(requestedRegions);
        if (requestedRegionNames.isEmpty()) {
            resultBuilder.error(String.format("Regions are mandatory on cloud provider: [%s]", cloudPlatform));
        } else {
            Set<String> existingRegionNames = new HashSet<>(supportedRegions);
            requestedRegionNames.removeAll(existingRegionNames);
            if (!requestedRegionNames.isEmpty()) {
                resultBuilder.error(String.format("The following regions does not exist in your cloud provider: [%s]. "
                                + "Existing regions are: [%s]",
                        String.join(", ", requestedRegionNames),
                        String.join(", ", existingRegionNames)
                ));
            }
        }
    }
}
