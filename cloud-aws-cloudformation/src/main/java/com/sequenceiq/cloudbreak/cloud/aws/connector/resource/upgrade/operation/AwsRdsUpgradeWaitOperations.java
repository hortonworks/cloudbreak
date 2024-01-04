package com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation;

import static com.sequenceiq.cloudbreak.cloud.aws.scheduler.WaiterRunner.run;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.dyngr.exception.PollerException;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonRdsClient;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.CustomAmazonWaiterProvider;
import com.sequenceiq.cloudbreak.cloud.aws.util.poller.upgrade.UpgradeStartPoller;
import com.sequenceiq.cloudbreak.cloud.aws.util.poller.upgrade.UpgradeStartWaitTask;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;

import software.amazon.awssdk.core.waiters.Waiter;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesRequest;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesResponse;

@Component
public class AwsRdsUpgradeWaitOperations {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsRdsUpgradeWaitOperations.class);

    @Inject
    private UpgradeStartPoller upgradeStartPoller;

    @Inject
    private CustomAmazonWaiterProvider customAmazonWaiterProvider;

    public void waitUntilUpgradeStarts(AmazonRdsClient rdsClient, DescribeDbInstancesRequest describeDBInstancesRequest) {
        LOGGER.debug("Starting RDS state polling until RDS start the upgrade, dbInstanceIdentifier: {}", describeDBInstancesRequest.dbInstanceIdentifier());
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

    public void waitUntilUpgradeFinishes(AmazonRdsClient rdsClient, DescribeDbInstancesRequest describeDBInstancesRequest) {
        Waiter<DescribeDbInstancesResponse> rdsWaiter = customAmazonWaiterProvider.getDbInstanceModifyWaiter();
        LOGGER.debug("Starting waiting on RDS upgrade");
        run(() -> rdsClient.describeDBInstances(describeDBInstancesRequest), rdsWaiter);
        LOGGER.debug("Finished waiting on RDS upgrade");
    }
}
