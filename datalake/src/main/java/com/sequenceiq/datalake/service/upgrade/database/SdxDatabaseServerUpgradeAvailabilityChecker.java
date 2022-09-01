package com.sequenceiq.datalake.service.upgrade.database;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.database.MajorVersion;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.util.MajorVersionComparator;
import com.sequenceiq.datalake.entity.SdxCluster;

@Component
public class SdxDatabaseServerUpgradeAvailabilityChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxDatabaseServerUpgradeAvailabilityChecker.class);

    @Inject
    private DatabaseEngineVersionReaderService databaseEngineVersionReaderService;

    public boolean isUpgradeNeeded(SdxCluster cluster, TargetMajorVersion targetMajorVersion) {
        Optional<MajorVersion> currentVersionOptional = MajorVersion.get(cluster.getDatabaseEngineVersion());
        LOGGER.debug("Checking if upgrade is needed for sdx cluster. Registered database engine version: {}", currentVersionOptional);
        if (currentVersionOptional.isEmpty()) {
            currentVersionOptional =  databaseEngineVersionReaderService.getDatabaseServerMajorVersion(cluster);
            LOGGER.debug("Database server engine version was not present in SDX, tried to query it, result: {}", currentVersionOptional);
        }
        return currentVersionOptional
                .map(majorVersion -> isUpgradeNeeded(majorVersion, targetMajorVersion))
                .orElse(true);
    }

    private boolean isUpgradeNeeded(MajorVersion currentVersion, TargetMajorVersion targetMajorVersion) {
        MajorVersionComparator majorVersionComparator = new MajorVersionComparator();
        boolean upgradeNeeded = majorVersionComparator.compare(currentVersion, targetMajorVersion.convertToMajorVersion()) < 0;
        LOGGER.debug("Comparing current and target versions. Current version is {}, and target version is {}, isUpgradeNeeded: {}", currentVersion,
                targetMajorVersion, upgradeNeeded);
        return upgradeNeeded;
    }

}
