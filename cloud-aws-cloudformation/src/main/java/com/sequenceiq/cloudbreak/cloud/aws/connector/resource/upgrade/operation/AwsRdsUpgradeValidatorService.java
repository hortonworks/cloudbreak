package com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation;

import static com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.AwsRdsState.AVAILABLE;
import static com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.AwsRdsState.UPGRADING;
import static com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.RdsState.OTHER;
import static com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.RdsState.UNKNOWN;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.common.database.Version;
import com.sequenceiq.cloudbreak.util.MajorVersionComparator;

@Service
public class AwsRdsUpgradeValidatorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsRdsUpgradeValidatorService.class);

    public void validateRdsCanBeUpgraded(RdsInfo rdsInfo, Version targetMajorVersion) {
        LOGGER.debug("Rds state is: {} and db instance statuses are: {} ", rdsInfo.getRdsState(), rdsInfo.getDbArnToInstanceStatuses());

        validateRdsIsAvailableOrUpgrading(rdsInfo);
        validateRdsMajorVersionIsSmallerThanTarget(rdsInfo, targetMajorVersion);
    }

    public void validateUpgradePresentForTargetMajorVersion(Optional<RdsEngineVersion> upgradeTargetForMajorVersion) {
        if (upgradeTargetForMajorVersion.isEmpty()) {
            String message = "There are no matching RDS upgrade versions to choose from.";
            LOGGER.warn(message);
            throw new CloudConnectorException(message);
        }
    }

    public void validateClusterHasASingleVersion(Set<String> dbVersions) {
        if (dbVersions.isEmpty()) {
            String message = "Current DB version could not be obtained";
            LOGGER.warn(message);
            throw new CloudConnectorException(message);
        }

        if (dbVersions.size() > 1) {
            String message = String.format("RDS is on multiple versions (%s), cannot proceed with its upgrade", dbVersions);
            LOGGER.warn(message);
            throw new CloudConnectorException(message);
        }
    }

    private void validateRdsIsAvailableOrUpgrading(RdsInfo rdsInfo) {
        if (OTHER == rdsInfo.getRdsState() || UNKNOWN == rdsInfo.getRdsState()) {
            String message = String.format("RDS upgrade is not possible as one or more instances are not in a correct state: %s",
                    getNotApplicableStates(rdsInfo.getDbArnToInstanceStatuses()));
            LOGGER.warn(message);
            throw new CloudConnectorException(message);
        }
    }

    private void validateRdsMajorVersionIsSmallerThanTarget(RdsInfo rdsInfo, Version targetMajorVersion) {
        if (new MajorVersionComparator().compare(rdsInfo.getRdsEngineVersion(), new RdsEngineVersion(targetMajorVersion.getMajorVersion())) >= 0) {
            String message = String.format("Cannot start upgrade as the RDS major version %s is not smaller than the target version %s",
                    rdsInfo.getRdsEngineVersion(), targetMajorVersion);
            LOGGER.debug(message);
            throw new CloudConnectorException(message);
        }
    }

    private Set<String> getNotApplicableStates(Map<String, String> dbArnToInstanceStatuses) {
        return dbArnToInstanceStatuses.entrySet().stream()
                .filter(entry -> !AVAILABLE.getState().equals(entry.getValue())
                        && !UPGRADING.getState().equals(entry.getValue()))
                .map(entry -> String.format("arn: %s => status: %s", entry.getKey(), entry.getValue()))
                .collect(Collectors.toSet());
    }

}