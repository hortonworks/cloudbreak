package com.sequenceiq.redbeams.service.validation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
import com.sequenceiq.cloudbreak.service.database.DatabaseInstanceTypeCapabilityValidator;
import com.sequenceiq.cloudbreak.service.database.DatabaseInstanceTypeValidationInput;
import com.sequenceiq.cloudbreak.service.database.DatabaseInstanceTypeValidationInput.InstanceTypeSpecs;
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

    @Inject
    private DatabaseInstanceTypeCapabilityValidator capabilityValidator;

    public Optional<String> validate(String instanceType, List<String> fallbackInstanceTypes, String environmentCrn, String cloudPlatform, String regionName) {
        if (fallbackInstanceTypes != null && !fallbackInstanceTypes.isEmpty()) {
            validatePrimaryInstanceTypePresent(instanceType);
            validateNoBlankEntries(fallbackInstanceTypes);
            validateNoDuplicates(fallbackInstanceTypes);
            validatePrimaryNotRepeatedInFallbacks(instanceType, fallbackInstanceTypes);
        }
        if (instanceType == null || instanceType.isBlank()) {
            return Optional.empty();
        }
        return validateWithCapabilities(instanceType, fallbackInstanceTypes, environmentCrn, cloudPlatform, regionName);
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

    private Optional<String> validateWithCapabilities(String instanceType, List<String> fallbackInstanceTypes,
            String environmentCrn, String cloudPlatform, String regionName) {
        try {
            PlatformDatabaseCapabilities capabilities = fetchCapabilities(environmentCrn, cloudPlatform, regionName);
            Region region = Region.region(regionName);
            Set<DatabaseVmType> regionTypes = capabilities.getRegionAvailableInstanceTypes().get(region);
            if (regionTypes == null || regionTypes.isEmpty()) {
                String message = String.format("Could not validate database instance type availability for region '%s' on platform '%s'. "
                        + "Proceeding without validation.", regionName, cloudPlatform);
                LOGGER.warn(message);
                return Optional.of(message);
            }

            Map<String, InstanceTypeSpecs> availableTypesMap = buildAvailableTypesMap(regionTypes);
            String defaultType = capabilities.getRegionDefaultInstanceTypeMap().get(region);

            DatabaseInstanceTypeValidationInput primaryInput = new DatabaseInstanceTypeValidationInput(
                    regionName, instanceType, defaultType, null, availableTypesMap);
            capabilityValidator.validate(primaryInput);

            if (fallbackInstanceTypes != null) {
                for (String fallback : fallbackInstanceTypes) {
                    DatabaseInstanceTypeValidationInput fallbackInput = new DatabaseInstanceTypeValidationInput(
                            regionName, fallback, defaultType, null, availableTypesMap);
                    capabilityValidator.validate(fallbackInput);
                }
            }
            return Optional.empty();
        } catch (BadRequestException e) {
            throw e;
        } catch (UnsupportedOperationException e) {
            LOGGER.debug("Database capabilities not supported for platform '{}', skipping validation", cloudPlatform);
            return Optional.empty();
        } catch (Exception e) {
            String message = String.format("Could not validate database instance type availability for region '%s' on platform '%s'. "
                    + "Proceeding without validation.", regionName, cloudPlatform);
            LOGGER.warn(message, e);
            return Optional.of(message);
        }
    }

    private PlatformDatabaseCapabilities fetchCapabilities(String environmentCrn, String cloudPlatform, String regionName) {
        CloudPlatformVariant platformVariant = new CloudPlatformVariant(Platform.platform(cloudPlatform), Variant.variant(cloudPlatform));
        CloudConnector connector = cloudPlatformConnectors.get(platformVariant);
        Credential credential = credentialService.getCredentialByEnvCrn(environmentCrn);
        ExtendedCloudCredential cloudCredential = extendedCredentialConverter.convert(credential, cloudPlatform);
        Region region = Region.region(regionName);
        return connector.platformResources().databaseCapabilities(cloudCredential, region, Map.of());
    }

    private Map<String, InstanceTypeSpecs> buildAvailableTypesMap(Set<DatabaseVmType> regionTypes) {
        Map<String, InstanceTypeSpecs> map = new HashMap<>();
        for (DatabaseVmType vmType : regionTypes) {
            map.put(vmType.getValue(), new InstanceTypeSpecs(
                    vmType.getMetaData().getCPU(),
                    vmType.getMetaData().getMemoryInGb(),
                    vmType.getMetaData().getArchitecture()));
        }
        return map;
    }
}
