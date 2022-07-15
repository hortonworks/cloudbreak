package com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation;

import static com.sequenceiq.cloudbreak.cloud.aws.scheduler.WaiterRunner.run;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DescribeDBEngineVersionsRequest;
import com.amazonaws.services.rds.model.DescribeDBEngineVersionsResult;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.amazonaws.services.rds.model.ModifyDBInstanceRequest;
import com.amazonaws.services.rds.model.UpgradeTarget;
import com.amazonaws.waiters.Waiter;
import com.dyngr.exception.PollerException;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonRdsClient;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.CustomAmazonWaiterProvider;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.StackCancellationCheck;
import com.sequenceiq.cloudbreak.cloud.aws.util.poller.upgrade.UpgradeStartPoller;
import com.sequenceiq.cloudbreak.cloud.aws.util.poller.upgrade.UpgradeStartWaitTask;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;

@Service
public class AwsRdsUpgradeOperations {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsRdsUpgradeOperations.class);

    @Inject
    private CustomAmazonWaiterProvider customAmazonWaiterProvider;

    @Inject
    private UpgradeStartPoller upgradeStartPoller;

    public String getCurrentDbEngineVersion(AmazonRdsClient rdsClient, String dbInstanceIdentifier) {
        DescribeDBInstancesRequest describeDBInstancesRequest = new DescribeDBInstancesRequest().withDBInstanceIdentifier(dbInstanceIdentifier);
        DescribeDBInstancesResult describeDBInstancesResult = rdsClient.describeDBInstances(describeDBInstancesRequest);
        Set<String> currentDbEngineVersions = describeDBInstancesResult.getDBInstances().stream()
                .map(DBInstance::getEngineVersion)
                .collect(Collectors.toSet());
        LOGGER.debug("Current RDS DB engine versions {}", currentDbEngineVersions);

        validateClusterHasASingleVersion(currentDbEngineVersions);
        String currentDbEngineVersion = currentDbEngineVersions.stream().findFirst().get();
        LOGGER.debug("Current DB engine version: {}", currentDbEngineVersion);

        return currentDbEngineVersion;
    }

    public Set<String> getUpgradeTargetVersions(AmazonRdsClient rdsClient, String dbVersion) {
        DescribeDBEngineVersionsRequest describeDBEngineVersionsRequest = new DescribeDBEngineVersionsRequest();
        describeDBEngineVersionsRequest.setEngine("postgres");
        describeDBEngineVersionsRequest.setEngineVersion(dbVersion);
        try {
            DescribeDBEngineVersionsResult result = rdsClient.describeDBEngineVersions(describeDBEngineVersionsRequest);
            LOGGER.debug("Filtering DB upgrade targets for current version {}, results are: {}", dbVersion, result);
            Set<String> validUpgradeTargets = result.getDBEngineVersions().stream()
                    .filter(ver -> dbVersion.equals(ver.getEngineVersion()))
                    .flatMap(ver -> ver.getValidUpgradeTarget().stream())
                    .map(UpgradeTarget::getEngineVersion)
                    .collect(Collectors.toSet());
            LOGGER.debug("The following valid AWS RDS upgrade targets were found: {}", validUpgradeTargets);
            return validUpgradeTargets;
        } catch (Exception e) {
            String message = String.format("Exception occurred when querying valid upgrade targets: %s", e);
            LOGGER.warn(message);
            throw new CloudConnectorException(message, e);
        }
    }

    public void upgradeRds(AmazonRdsClient rdsClient, String targetVersion, String dbInstanceIdentifier) {
        ModifyDBInstanceRequest modifyDBInstanceRequest = new ModifyDBInstanceRequest();
        modifyDBInstanceRequest.setDBInstanceIdentifier(dbInstanceIdentifier);
        modifyDBInstanceRequest.setEngineVersion(targetVersion);
        modifyDBInstanceRequest.setAllowMajorVersionUpgrade(true);
        modifyDBInstanceRequest.setApplyImmediately(true);

        LOGGER.debug("RDS modify request to upgrade engine version to {}: {}", targetVersion, dbInstanceIdentifier);
        try {
            rdsClient.modifyDBInstance(modifyDBInstanceRequest);
        } catch (RuntimeException ex) {
            String message = String.format("Error when starting the upgrade of RDS: %s", ex);
            LOGGER.warn(message);
            throw new CloudConnectorException(message, ex);
        }
    }

    public void waitForRdsUpgrade(AuthenticatedContext ac, AmazonRdsClient rdsClient, String dbInstanceIdentifier) {
        LOGGER.debug("Waiting until RDS enters upgrading state");
        DescribeDBInstancesRequest describeDBInstancesRequest = new DescribeDBInstancesRequest().withDBInstanceIdentifier(dbInstanceIdentifier);
        waitUntilUpgradeStarts(rdsClient, describeDBInstancesRequest);
        waitUntilUpgradeFinishes(ac, rdsClient, describeDBInstancesRequest);
    }

    private void waitUntilUpgradeStarts(AmazonRdsClient rdsClient, DescribeDBInstancesRequest describeDBInstancesRequest) {
        UpgradeStartWaitTask upgradeStartWaitTask = new UpgradeStartWaitTask(describeDBInstancesRequest, rdsClient);
        try {
            upgradeStartPoller.waitForUpgradeToStart(upgradeStartWaitTask);
            LOGGER.debug("RDS entered the upgrading state");
        } catch (PollerException e) {
            String message = String.format("Error when waiting for upgrade to start: %s", e);
            LOGGER.warn(message);
            throw new CloudConnectorException(message, e);
        }
    }

    private void waitUntilUpgradeFinishes(AuthenticatedContext ac, AmazonRdsClient rdsClient, DescribeDBInstancesRequest describeDBInstancesRequest) {
        Waiter<DescribeDBInstancesRequest> rdsWaiter = customAmazonWaiterProvider.getDbInstanceModifyWaiter(rdsClient);
        StackCancellationCheck stackCancellationCheck = new StackCancellationCheck(ac.getCloudContext().getId());
        LOGGER.debug("Starting waiting on RDS upgrade");
        run(rdsWaiter, describeDBInstancesRequest, stackCancellationCheck);
        LOGGER.debug("Finished waiting on RDS upgrade");
    }

    private void validateClusterHasASingleVersion(Set<String> dbVersions) {
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

}
