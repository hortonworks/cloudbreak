package com.sequenceiq.redbeams.service.validation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseVmType;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformDatabaseCapabilities;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.redbeams.converter.cloud.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.redbeams.dto.Credential;
import com.sequenceiq.redbeams.service.CredentialService;

@Component
public class DatabaseInstanceTypeValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseInstanceTypeValidator.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private CredentialService credentialService;

    @Inject
    private CredentialToExtendedCloudCredentialConverter extendedCredentialConverter;

    public Optional<String> validate(String instanceType, List<String> fallbackInstanceTypes, String environmentCrn, String cloudPlatform, String regionName) {
        if (fallbackInstanceTypes == null || fallbackInstanceTypes.isEmpty()) {
            return Optional.empty();
        }
        validatePrimaryInstanceTypePresent(instanceType);
        validateNoBlankEntries(fallbackInstanceTypes);
        validateNoDuplicates(fallbackInstanceTypes);
        validatePrimaryNotRepeatedInFallbacks(instanceType, fallbackInstanceTypes);
        return validateRegionAvailability(instanceType, fallbackInstanceTypes, environmentCrn, cloudPlatform, regionName);
    }

    private void validatePrimaryInstanceTypePresent(String instanceType) {
        if (instanceType == null || instanceType.isBlank()) {
            throw new BadRequestException("Primary instance type must be specified when fallback instance types are provided.");
        }
    }

    private void validateNoBlankEntries(List<String> fallbackInstanceTypes) {
        for (int i = 0; i < fallbackInstanceTypes.size(); i++) {
            String entry = fallbackInstanceTypes.get(i);
            if (entry == null || entry.isBlank()) {
                throw new BadRequestException(String.format("Fallback instance type at position %d is blank.", i + 1));
            }
        }
    }

    private void validateNoDuplicates(List<String> fallbackInstanceTypes) {
        Set<String> seen = new HashSet<>();
        for (String entry : fallbackInstanceTypes) {
            if (!seen.add(entry)) {
                throw new BadRequestException(String.format("Duplicate fallback instance type: '%s'.", entry));
            }
        }
    }

    private void validatePrimaryNotRepeatedInFallbacks(String instanceType, List<String> fallbackInstanceTypes) {
        if (fallbackInstanceTypes.contains(instanceType)) {
            throw new BadRequestException(String.format(
                    "Primary instance type '%s' must not be repeated in the fallback instance types list.", instanceType));
        }
    }

    private Optional<String> validateRegionAvailability(String instanceType, List<String> fallbackInstanceTypes,
            String environmentCrn, String cloudPlatform, String regionName) {
        Optional<String> warning = Optional.empty();
        Set<String> availableTypes = getAvailableInstanceTypes(environmentCrn, cloudPlatform, regionName);
        if (availableTypes.isEmpty()) {
            String message = String.format("Could not validate database instance type availability for region '%s' on platform '%s'. "
                    + "Proceeding without validation.", regionName, cloudPlatform);
            LOGGER.warn(message);
            return Optional.of(message);
        }
        List<String> allTypes = new ArrayList<>();
        allTypes.add(instanceType);
        allTypes.addAll(fallbackInstanceTypes);

        List<String> unavailableTypes = allTypes.stream()
                .filter(type -> !availableTypes.contains(type))
                .collect(Collectors.toList());

        if (!unavailableTypes.isEmpty()) {
            throw new BadRequestException(String.format(
                    "The following database instance types are not available in region '%s': %s. Available types: %s",
                    regionName, unavailableTypes, availableTypes.stream().sorted().collect(Collectors.joining(", "))));
        }
        return warning;
    }

    private Set<String> getAvailableInstanceTypes(String environmentCrn, String cloudPlatform, String regionName) {
        try {
            CloudPlatformVariant platformVariant = new CloudPlatformVariant(Platform.platform(cloudPlatform), Variant.variant(cloudPlatform));
            CloudConnector connector = cloudPlatformConnectors.get(platformVariant);
            Credential credential = credentialService.getCredentialByEnvCrn(environmentCrn);
            ExtendedCloudCredential cloudCredential = extendedCredentialConverter.convert(credential, cloudPlatform);
            Region region = Region.region(regionName);

            PlatformDatabaseCapabilities capabilities = connector.platformResources().databaseCapabilities(cloudCredential, region, Map.of());
            Set<DatabaseVmType> regionTypes = capabilities.getRegionAvailableInstanceTypes().get(region);
            if (regionTypes == null || regionTypes.isEmpty()) {
                return Set.of();
            }
            return regionTypes.stream()
                    .map(DatabaseVmType::getValue)
                    .collect(Collectors.toSet());
        } catch (UnsupportedOperationException e) {
            LOGGER.debug("Database capabilities not supported for platform '{}', skipping availability validation", cloudPlatform);
            return Set.of();
        } catch (Exception e) {
            LOGGER.warn("Failed to fetch database capabilities for region '{}' on platform '{}', skipping availability validation",
                    regionName, cloudPlatform, e);
            return Set.of();
        }
    }
}
