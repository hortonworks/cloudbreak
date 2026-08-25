package com.sequenceiq.cloudbreak.service.database;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.database.DatabaseInstanceTypeValidationInput.InstanceTypeSpecs;
import com.sequenceiq.common.model.Architecture;

@Component
public class DatabaseInstanceTypeCapabilityValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseInstanceTypeCapabilityValidator.class);

    private static final int MAX_TYPES_IN_ERROR_MESSAGE = 20;

    public void validate(DatabaseInstanceTypeValidationInput input) {
        if (input.availableTypes() == null || input.availableTypes().isEmpty()) {
            LOGGER.warn("No available instance type data for region '{}'. Skipping database instance type validation.", input.regionName());
            return;
        }
        validateRegionAvailability(input);
        validateMinimumSpecs(input);
        validateArchitecture(input);
    }

    private void validateRegionAvailability(DatabaseInstanceTypeValidationInput input) {
        if (!input.availableTypes().containsKey(input.requestedInstanceType())) {
            String availableList = input.availableTypes().keySet().stream()
                    .sorted()
                    .limit(MAX_TYPES_IN_ERROR_MESSAGE)
                    .collect(Collectors.joining(", "));
            String suffix = input.availableTypes().size() > MAX_TYPES_IN_ERROR_MESSAGE
                    ? " (showing first " + MAX_TYPES_IN_ERROR_MESSAGE + " of " + input.availableTypes().size() + ")"
                    : "";
            throw new BadRequestException(String.format(
                    "Database instance type '%s' is not available in region '%s'. Available types: [%s]%s",
                    input.requestedInstanceType(), input.regionName(), availableList, suffix));
        }
    }

    private void validateMinimumSpecs(DatabaseInstanceTypeValidationInput input) {
        if (input.defaultInstanceType() == null) {
            LOGGER.debug("No default instance type for region '{}', skipping minimum spec validation.", input.regionName());
            return;
        }
        InstanceTypeSpecs defaultSpecs = input.availableTypes().get(input.defaultInstanceType());
        InstanceTypeSpecs requestedSpecs = input.availableTypes().get(input.requestedInstanceType());
        if (defaultSpecs == null || requestedSpecs == null) {
            LOGGER.debug("Spec data not available for default '{}' or requested '{}', skipping minimum spec validation.",
                    input.defaultInstanceType(), input.requestedInstanceType());
            return;
        }
        if (defaultSpecs.cpu() != null && requestedSpecs.cpu() != null && requestedSpecs.cpu() < defaultSpecs.cpu()) {
            throw new BadRequestException(String.format(
                    "Database instance type '%s' has %d vCPU which is less than the minimum required %d vCPU (from default type '%s').",
                    input.requestedInstanceType(), requestedSpecs.cpu(), defaultSpecs.cpu(), input.defaultInstanceType()));
        }
        if (defaultSpecs.memoryInGb() != null && requestedSpecs.memoryInGb() != null
                && requestedSpecs.memoryInGb() < defaultSpecs.memoryInGb()) {
            throw new BadRequestException(String.format(
                    "Database instance type '%s' has %.1f GB memory which is less than the minimum required %.1f GB (from default type '%s').",
                    input.requestedInstanceType(), requestedSpecs.memoryInGb(), defaultSpecs.memoryInGb(), input.defaultInstanceType()));
        }
    }

    private void validateArchitecture(DatabaseInstanceTypeValidationInput input) {
        Architecture desired = input.desiredArchitecture();
        if (desired == null || Architecture.UNKNOWN.equals(desired)) {
            return;
        }
        InstanceTypeSpecs requestedSpecs = input.availableTypes().get(input.requestedInstanceType());
        if (requestedSpecs == null || requestedSpecs.architecture() == null || Architecture.UNKNOWN.equals(requestedSpecs.architecture())) {
            LOGGER.debug("Architecture data not available for '{}', skipping architecture validation.", input.requestedInstanceType());
            return;
        }
        if (!desired.equals(requestedSpecs.architecture())) {
            throw new BadRequestException(String.format(
                    "Database instance type '%s' has architecture '%s' which does not match the requested cluster architecture '%s'.",
                    input.requestedInstanceType(), requestedSpecs.architecture().getName(), desired.getName()));
        }
    }
}
