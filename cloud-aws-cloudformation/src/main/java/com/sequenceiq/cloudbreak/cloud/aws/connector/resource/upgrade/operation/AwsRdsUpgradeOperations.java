package com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.amazonaws.services.rds.model.ModifyDBInstanceRequest;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonRdsClient;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.common.database.Version;

@Service
public class AwsRdsUpgradeOperations {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsRdsUpgradeOperations.class);

    @Inject
    private AwsRdsVersionOperations awsRdsVersionOperations;

    @Inject
    private AwsRdsUpgradeValidatorService awsRdsUpgradeValidatorService;

    @Inject
    private AwsRdsUpgradeWaitOperations awsRdsUpgradeWaitOperations;

    public DescribeDBInstancesResult describeRds(AmazonRdsClient rdsClient, String dbInstanceIdentifier) {
        DescribeDBInstancesRequest describeDBInstancesRequest = new DescribeDBInstancesRequest().withDBInstanceIdentifier(dbInstanceIdentifier);
        DescribeDBInstancesResult result = rdsClient.describeDBInstances(describeDBInstancesRequest);
        LOGGER.debug("Describing RDS with dbInstanceIdentifier {}, result: {}", dbInstanceIdentifier, result);
        return result;
    }

    public RdsEngineVersion getHighestUpgradeTargetVersion(AmazonRdsClient rdsClient, Version targetMajorVersion, RdsEngineVersion currentDbVersion) {
        Set<String> validUpgradeTargets = awsRdsVersionOperations.getAllUpgradeTargetVersions(rdsClient, currentDbVersion);
        Optional<RdsEngineVersion> upgradeTargetForMajorVersion = awsRdsVersionOperations.getHighestUpgradeVersionForTargetMajorVersion(validUpgradeTargets,
                targetMajorVersion);
        awsRdsUpgradeValidatorService.validateUpgradePresentForTargetMajorVersion(upgradeTargetForMajorVersion);
        LOGGER.debug("The highest available RDS upgrade target version for major version {} is: {}", targetMajorVersion, upgradeTargetForMajorVersion);
        return upgradeTargetForMajorVersion.get();
    }

    public void upgradeRds(AmazonRdsClient rdsClient, RdsEngineVersion targetVersion, String dbInstanceIdentifier, String dbParameterGroupName) {
        ModifyDBInstanceRequest modifyDBInstanceRequest = new ModifyDBInstanceRequest()
                .withDBInstanceIdentifier(dbInstanceIdentifier)
                .withEngineVersion(targetVersion.getVersion())
                .withAllowMajorVersionUpgrade(true)
                .withApplyImmediately(true);
        if (StringUtils.isNotEmpty(dbParameterGroupName)) {
            modifyDBInstanceRequest.withDBParameterGroupName(dbParameterGroupName);
        }

        LOGGER.debug("RDS modify request to upgrade engine version to {} for DB {}, request: {}", targetVersion, dbInstanceIdentifier, modifyDBInstanceRequest);
        try {
            rdsClient.modifyDBInstance(modifyDBInstanceRequest);
        } catch (Exception ex) {
            if (ex.getMessage().contains("Cannot modify engine version because another engine version upgrade is already in progress")) {
                LOGGER.info("The upgrade has already been started");
            } else {
                String message = String.format("Error when starting the upgrade of RDS: %s", ex);
                LOGGER.warn(message);
                throw new CloudConnectorException(message, ex);
            }
        }
    }

    public void waitForRdsUpgrade(AuthenticatedContext ac, AmazonRdsClient rdsClient, String dbInstanceIdentifier) {
        LOGGER.debug("Waiting until RDS enters upgrading state, dbInstanceIdentifier: {}", dbInstanceIdentifier);
        DescribeDBInstancesRequest describeDBInstancesRequest = new DescribeDBInstancesRequest().withDBInstanceIdentifier(dbInstanceIdentifier);
        awsRdsUpgradeWaitOperations.waitUntilUpgradeStarts(rdsClient, describeDBInstancesRequest);
        awsRdsUpgradeWaitOperations.waitUntilUpgradeFinishes(ac, rdsClient, describeDBInstancesRequest);
    }
}
