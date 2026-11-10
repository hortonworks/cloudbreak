package com.sequenceiq.redbeams.service.validation;

import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.database.DatabaseServerInstanceTypeState;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;

/**
 * Pre-flight validation of changing the instance type of a database server, run against the live values read from the cloud provider.
 * Requesting the instance type the database server already runs on is valid, the flow then only updates the metadata.
 */
@Component
public class ChangeInstanceTypeValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChangeInstanceTypeValidator.class);

    @Inject
    private DatabaseInstanceTypeValidator databaseInstanceTypeValidator;

    public void validate(DatabaseStack databaseStack, DatabaseServerInstanceTypeState liveState, String targetInstanceType, String environmentCrn,
            String cloudPlatform, String regionName) {
        String serverId = databaseStack.getDatabaseServer().getServerId();
        LOGGER.debug("Validating the change of the instance type of database server '{}' to '{}', live state: {}", serverId, targetInstanceType, liveState);
        if (StringUtils.isBlank(liveState.instanceType())) {
            throw new BadRequestException(String.format("The cloud provider did not report the current instance type of database server '%s', "
                    + "so it can not be changed to '%s'.", serverId, targetInstanceType));
        }
        if (liveState.modificationInProgress() && !StringUtils.equalsIgnoreCase(targetInstanceType, liveState.instanceType())) {
            throw new BadRequestException(String.format("A modification is already in progress on database server '%s', so its instance type can not be "
                    + "changed to '%s' now, retry once it finishes.", serverId, targetInstanceType));
        }
        validateAgainstRegionCapabilities(targetInstanceType, environmentCrn, cloudPlatform, regionName);
    }

    private void validateAgainstRegionCapabilities(String targetInstanceType, String environmentCrn, String cloudPlatform, String regionName) {
        Optional<String> warning = databaseInstanceTypeValidator.validate(targetInstanceType, null, environmentCrn, cloudPlatform, regionName);
        warning.ifPresent(message -> LOGGER.warn("Validating the target instance type '{}' against the capabilities of region '{}' on platform '{}' "
                + "produced a warning: {}", targetInstanceType, regionName, cloudPlatform, message));
    }
}
