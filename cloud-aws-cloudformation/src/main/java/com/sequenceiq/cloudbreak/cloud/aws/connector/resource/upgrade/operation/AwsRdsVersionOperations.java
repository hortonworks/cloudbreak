package com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation;

import static com.sequenceiq.cloudbreak.cloud.model.DatabaseEngine.POSTGRESQL;
import static com.sequenceiq.cloudbreak.common.database.MajorVersion.VERSION_13;
import static com.sequenceiq.cloudbreak.common.database.MajorVersion.VERSION_FAMILY_9;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.services.rds.model.AmazonRDSException;
import com.amazonaws.services.rds.model.DescribeDBEngineVersionsRequest;
import com.amazonaws.services.rds.model.DescribeDBEngineVersionsResult;
import com.amazonaws.services.rds.model.UpgradeTarget;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonRdsClient;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseEngine;
import com.sequenceiq.cloudbreak.common.database.Version;
import com.sequenceiq.cloudbreak.util.VersionComparator;

@Service
public class AwsRdsVersionOperations {

    private static final Pattern ENGINE_VERSION_PATTERN = Pattern.compile("^(\\d+)(?:\\.\\d+)?$");

    private static final int GROUP_MAJOR_VERSION = 1;

    private static final String POSTGRES = "postgres";

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsRdsVersionOperations.class);

    public String getDBParameterGroupFamily(DatabaseEngine engine, String engineVersion) {
        LOGGER.debug("Getting the DB parameter group family for engine {} and version {}", engine, engineVersion);
        if (POSTGRESQL == engine) {
            return getPostgresFamilyVersion(engineVersion);
        } else {
            String message = String.format("Unsupported RDS engine: %s", engine);
            LOGGER.warn(message);
            throw new IllegalStateException(message);
        }
    }

    private String getPostgresFamilyVersion(String engineVersion) {
        String familyVersion = null;
        if (engineVersion != null) {
            Matcher engineVersionMatcher = ENGINE_VERSION_PATTERN.matcher(engineVersion);
            if (engineVersionMatcher.matches()) {
                String engineMajorVersion = engineVersionMatcher.group(GROUP_MAJOR_VERSION);
                int engineMajorVersionNumber = Integer.parseInt(engineMajorVersion);
                if (VERSION_FAMILY_9.getMajorVersionFamily() <= engineMajorVersionNumber && VERSION_13.getMajorVersionFamily() >= engineMajorVersionNumber) {
                    // Family version matches the engine version for 9.5 and 9.6, and simply equals the major version otherwise
                    familyVersion = VERSION_FAMILY_9.getMajorVersionFamily() == engineMajorVersionNumber ? engineVersion : engineMajorVersion;
                } else {
                    throwEngineVersionError(String.format("Unsupported RDS POSTGRESQL engine version %s, it is expected to be between %s and %s",
                            engineVersion, VERSION_FAMILY_9, VERSION_13));
                }
            } else {
                throwEngineVersionError(String.format("Unsupported RDS POSTGRESQL engine version %s", engineVersion));
            }
        } else {
            throwEngineVersionError("RDS POSTGRESQL engine version is null, not able to determine postgres family");
        }
        String postgresFamilyVersion = POSTGRES + familyVersion;
        LOGGER.debug("Get the postgres family version returns {}", postgresFamilyVersion);
        return postgresFamilyVersion;
    }

    private void throwEngineVersionError(String message) {
        LOGGER.warn(message);
        throw new IllegalStateException(message);
    }

    public RdsEngineVersion getHighestUpgradeTargetVersion(Set<RdsEngineVersion> upgradeTargetsForMajorVersion) {
        List<RdsEngineVersion> sortedUpgradeTargetVersion = upgradeTargetsForMajorVersion.stream()
                .sorted(new VersionComparator())
                .collect(Collectors.toList());
        return sortedUpgradeTargetVersion.get(sortedUpgradeTargetVersion.size() - 1);
    }

    public Set<String> getAllUpgradeTargetVersions(AmazonRdsClient rdsClient, RdsEngineVersion dbVersion) {
        DescribeDBEngineVersionsRequest describeDBEngineVersionsRequest = new DescribeDBEngineVersionsRequest();
        describeDBEngineVersionsRequest.setEngine("postgres");
        describeDBEngineVersionsRequest.setEngineVersion(dbVersion.getVersion());
        try {
            DescribeDBEngineVersionsResult result = rdsClient.describeDBEngineVersions(describeDBEngineVersionsRequest);
            LOGGER.debug("Filtering DB upgrade targets for current version {}, results are: {}", dbVersion, result);
            Set<String> validUpgradeTargets = result.getDBEngineVersions().stream()
                    .flatMap(ver -> ver.getValidUpgradeTarget().stream())
                    .map(UpgradeTarget::getEngineVersion)
                    .collect(Collectors.toSet());
            LOGGER.debug("The following valid AWS RDS upgrade targets were found: {}", validUpgradeTargets);
            return validUpgradeTargets;
        } catch (AmazonRDSException e) {
            String message = getErrorMessage(e.getErrorMessage());
            throw new CloudConnectorException(message, e);
        } catch (Exception e) {
            String message = getErrorMessage(e.getMessage());
            throw new CloudConnectorException(message, e);
        }
    }

    private static String getErrorMessage(String exceptionMessage) {
        String message = String.format("Exception occurred when querying valid upgrade targets: %s", exceptionMessage);
        LOGGER.warn(message);
        return message;
    }

    public Optional<RdsEngineVersion> getHighestUpgradeVersionForTargetMajorVersion(Set<String> validUpgradeTargetVersions, Version targetMajorVersion) {
        String targetMajorVersionString = targetMajorVersion.getMajorVersion() + ".";
        List<RdsEngineVersion> upgradeTargetsForMajorVersion = validUpgradeTargetVersions.stream()
                .filter(version -> version.startsWith(targetMajorVersionString))
                .map(RdsEngineVersion::new)
                .sorted(new VersionComparator())
                .collect(Collectors.toList());
        LOGGER.debug("DB engine versions applicable for the current major version {} are: {}", targetMajorVersion.getMajorVersion(),
                upgradeTargetsForMajorVersion);
        return upgradeTargetsForMajorVersion.isEmpty()
                ? Optional.empty()
                : Optional.of(upgradeTargetsForMajorVersion.get(upgradeTargetsForMajorVersion.size() - 1));
    }

}
