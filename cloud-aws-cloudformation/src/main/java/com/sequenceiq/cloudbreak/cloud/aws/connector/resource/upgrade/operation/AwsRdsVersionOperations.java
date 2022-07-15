package com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.common.database.MajorVersion;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.util.VersionComparator;

@Service
public class AwsRdsVersionOperations {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsRdsVersionOperations.class);

    public String getHighestUpgradeVersion(Set<String> allUpgradeTargetVersions, MajorVersion targetMajorVersion) {
        Set<DbEngineVersion> upgradeTargetsForMajorVersion = getUpgradeVersionsForTargetMajor(allUpgradeTargetVersions, targetMajorVersion);
        checkUpgradePresentForTargetMajorVersion(allUpgradeTargetVersions, upgradeTargetsForMajorVersion);
        String highestUpgradeTargetVersion = getHighestUpgradeTargetVersion(upgradeTargetsForMajorVersion);
        LOGGER.debug("The highest available upgrade target version for major version {} is: {}", upgradeTargetsForMajorVersion, highestUpgradeTargetVersion);
        return highestUpgradeTargetVersion;
    }

    private void checkUpgradePresentForTargetMajorVersion(Set<String> validUpgradeTargetVersions, Set<DbEngineVersion> upgradeTargetsForMajorVersion) {
        if (upgradeTargetsForMajorVersion.isEmpty()) {
            String message = String.format("There are no matching RDS upgrade versions to choose from. Available versions: %s", validUpgradeTargetVersions);
            LOGGER.warn(message);
            throw new CloudConnectorException(message);
        }
    }

    private String getHighestUpgradeTargetVersion(Set<DbEngineVersion> upgradeTargetsForMajorVersion) {
        VersionComparator versionComparator = new VersionComparator();
        List<DbEngineVersion> sortedUpgradeTargetVersion = upgradeTargetsForMajorVersion.stream()
                .sorted(versionComparator)
                .collect(Collectors.toList());
        return sortedUpgradeTargetVersion.get(sortedUpgradeTargetVersion.size() - 1).getVersion();
    }

    private Set<DbEngineVersion> getUpgradeVersionsForTargetMajor(Set<String> validUpgradeTargetVersions, MajorVersion targetMajorVersion) {
        String targetMajorVersionString = targetMajorVersion.getMajorVersion() + ".";
        Set<DbEngineVersion> upgradeTargetsForMajorVersion = validUpgradeTargetVersions.stream()
                .filter(version -> version.startsWith(targetMajorVersionString))
                .map(DbEngineVersion::new)
                .collect(Collectors.toSet());
        LOGGER.debug("DB engine versions applicable for the current major version {} are: {}", targetMajorVersion.getMajorVersion(),
                upgradeTargetsForMajorVersion);
        return upgradeTargetsForMajorVersion;
    }

    private void validateClusterHasASingleVersion(Set<String> dbVersions) {
        if (dbVersions.size() > 1) {
            String message = String.format("RDS is on multiple versions (), cannot proceed with its upgrade", dbVersions);
            LOGGER.warn(message);
            throw new CloudConnectorException(message);
        }
    }

    private void validateClusterHasVersion(Set<String> dbVersions) {
        if (dbVersions.isEmpty()) {
            String message = String.format("Current DB version could not be obtained");
            throw new CloudConnectorException(message);
        }
    }

    private static class DbEngineVersion implements Versioned {

        private final String version;

        DbEngineVersion(String version) {
            this.version = version;
        }

        @Override
        public String getVersion() {
            return version;
        }

        @Override
        public String toString() {
            return "DbEngineVersion{" +
                    "version='" + version + '\'' +
                    '}';
        }
    }

}
