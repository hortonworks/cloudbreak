package com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation;

import static com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.AwsRdsState.AVAILABLE;
import static com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.AwsRdsState.UPGRADING;
import static com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.RdsState.OTHER;
import static com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.RdsState.UNKNOWN;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonRdsClient;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.AwsRdsStatusLookupService;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.common.database.Version;
import com.sequenceiq.cloudbreak.util.MajorVersionComparator;

import software.amazon.awssdk.services.rds.model.DBParameterGroupStatus;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesResponse;

@Service
public class AwsRdsUpgradeValidatorProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsRdsUpgradeValidatorProvider.class);

    @Inject
    private AwsRdsStatusLookupService awsRdsStatusLookupService;

    @Inject
    private AwsRdsVersionOperations awsRdsVersionOperations;

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

    public void validateRdsIsAvailableOrUpgrading(RdsInfo rdsInfo) {
        if (OTHER == rdsInfo.getRdsState() || UNKNOWN == rdsInfo.getRdsState()) {
            String message = String.format("RDS upgrade is not possible as one or more instances are not in a correct state: %s",
                    getNotApplicableStates(rdsInfo.getDbArnToInstanceStatuses()));
            LOGGER.warn(message);
            throw new CloudConnectorException(message);
        }
    }

    public boolean isRdsMajorVersionSmallerThanTarget(RdsInfo rdsInfo, Version targetMajorVersion) {
        MajorVersionComparator majorVersionComparator = new MajorVersionComparator();
        RdsEngineVersion currentVersion = rdsInfo.getRdsEngineVersion();
        RdsEngineVersion targetVersion = new RdsEngineVersion(targetMajorVersion.getMajorVersion());
        boolean upgradeNeeded = majorVersionComparator.compare(currentVersion, targetVersion) < 0;
        LOGGER.debug("Comparing current DB version to target major version if an upgrade is needed. Current version: {}. target version: {}. upgarde needed: {}",
                currentVersion, targetVersion, upgradeNeeded);
        return upgradeNeeded;
    }

    public void validateCustomPropertiesAdded(AuthenticatedContext authenticatedContext, DatabaseStack stack) {
        if (stack.getDatabaseServer().isUseSslEnforcement()) {
            LOGGER.info("Custom parameter group check is skipped as CB use its own custom group for ssl enforcement");
        } else {
            DescribeDbInstancesResponse describeDbInstancesResponse = awsRdsStatusLookupService.getDescribeDBInstancesResult(authenticatedContext, stack);
            if (describeDbInstancesResponse != null && CollectionUtils.isNotEmpty(describeDbInstancesResponse.dbInstances())) {
                String nonDefaultParamGroupNames = getNonDefaultParamGroupNames(describeDbInstancesResponse);
                if (StringUtils.isNotBlank(nonDefaultParamGroupNames)) {
                    String message = String.format("The following custom parameter groups are attached to the RDS instance [%s]: %s. " +
                                    "As we could not guarantee parameter compatibility between RDS versions, " +
                                    "please remove them before RDS upgrade and they can be recreated based on the previous ones afterwards manually.",
                            stack.getDatabaseServer().getServerId(), nonDefaultParamGroupNames);
                    LOGGER.warn(message);
                    throw new CloudConnectorException(message);
                }
            } else {
                LOGGER.warn("AWS describeRDSInstances() returns empty result for {}, validation will be skipped", stack.getDatabaseServer().getServerId());
            }
        }
    }

    private String getNonDefaultParamGroupNames(DescribeDbInstancesResponse describeDbInstancesResponse) {
        return describeDbInstancesResponse.dbInstances().stream()
                .filter(instance -> Objects.nonNull(instance.dbParameterGroups()))
                .flatMap(instance -> instance.dbParameterGroups().stream())
                .map(DBParameterGroupStatus::dbParameterGroupName)
                .filter(groupName -> !groupName.startsWith("default."))
                .collect(Collectors.joining(", "));
    }

    private Set<String> getNotApplicableStates(Map<String, String> dbArnToInstanceStatuses) {
        return dbArnToInstanceStatuses.entrySet().stream()
                .filter(entry -> !AVAILABLE.getState().equals(entry.getValue())
                        && !UPGRADING.getState().equals(entry.getValue()))
                .map(entry -> String.format("arn: %s => status: %s", entry.getKey(), entry.getValue()))
                .collect(Collectors.toSet());
    }

    public RdsEngineVersion getHighestUpgradeTargetVersion(AmazonRdsClient rdsClient, Version targetMajorVersion, RdsEngineVersion currentDbVersion) {
        Set<String> validUpgradeTargets = awsRdsVersionOperations.getAllUpgradeTargetVersions(rdsClient, currentDbVersion);
        Optional<RdsEngineVersion> upgradeTargetForMajorVersion = awsRdsVersionOperations.getHighestUpgradeVersionForTargetMajorVersion(validUpgradeTargets,
                targetMajorVersion);
        validateUpgradePresentForTargetMajorVersion(upgradeTargetForMajorVersion);
        LOGGER.debug("The highest available RDS upgrade target version for major version {} is: {}", targetMajorVersion, upgradeTargetForMajorVersion);
        return upgradeTargetForMajorVersion.get();
    }
}
