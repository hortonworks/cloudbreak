package com.sequenceiq.datalake.service.upgrade.database;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.StackDatabaseServerResponse;
import com.sequenceiq.cloudbreak.common.database.MajorVersion;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.util.MajorVersionComparator;

@Component
public class SdxDatabaseServerUpgradeAvailabilityChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxDatabaseServerUpgradeAvailabilityChecker.class);

    public boolean isUpgradeNeeded(StackDatabaseServerResponse stackDatabaseServerResponse, TargetMajorVersion targetMajorVersion, boolean forced) {
        Optional<MajorVersion> currentVersionOptional = Optional.ofNullable(stackDatabaseServerResponse.getMajorVersion());
        LOGGER.debug("Checking if database upgrade is needed for sdx cluster. Registered database engine version: {}", currentVersionOptional);
        return currentVersionOptional
                .map(majorVersion -> isUpgradeNeeded(majorVersion, targetMajorVersion, forced))
                .orElse(true);
    }

    private boolean isUpgradeNeeded(MajorVersion currentVersion, TargetMajorVersion targetMajorVersion, boolean forced) {
        MajorVersionComparator majorVersionComparator = new MajorVersionComparator();
        int compareResult = majorVersionComparator.compare(currentVersion, targetMajorVersion.convertToMajorVersion());
        boolean upgradeNeeded = compareResult < 0 || (forced && compareResult == 0);
        LOGGER.debug("Comparing current and target versions. Current version is {}, and target version is {}, forced is {}, isUpgradeNeeded: {}",
                currentVersion, targetMajorVersion, forced, upgradeNeeded);
        return upgradeNeeded;
    }
}
