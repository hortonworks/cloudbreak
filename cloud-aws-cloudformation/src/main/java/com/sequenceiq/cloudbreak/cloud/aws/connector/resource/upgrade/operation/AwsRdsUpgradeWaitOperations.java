package com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation;

import static com.sequenceiq.cloudbreak.cloud.aws.scheduler.WaiterRunner.run;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.waiters.Waiter;
import com.dyngr.exception.PollerException;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonRdsClient;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.CustomAmazonWaiterProvider;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.StackCancellationCheck;
import com.sequenceiq.cloudbreak.cloud.aws.util.poller.upgrade.UpgradeStartPoller;
import com.sequenceiq.cloudbreak.cloud.aws.util.poller.upgrade.UpgradeStartWaitTask;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;

@Component
public class AwsRdsUpgradeWaitOperations {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsRdsUpgradeWaitOperations.class);

    @Inject
    private UpgradeStartPoller upgradeStartPoller;

    @Inject
    private CustomAmazonWaiterProvider customAmazonWaiterProvider;

    public void waitUntilUpgradeStarts(AmazonRdsClient rdsClient, DescribeDBInstancesRequest describeDBInstancesRequest) {
        LOGGER.debug("Starting RDS state polling until RDS start the upgrade, dbInstanceIdentifier: {}", describeDBInstancesRequest.getDBInstanceIdentifier());
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

    public void waitUntilUpgradeFinishes(AuthenticatedContext ac, AmazonRdsClient rdsClient, DescribeDBInstancesRequest describeDBInstancesRequest) {
        Waiter<DescribeDBInstancesRequest> rdsWaiter = customAmazonWaiterProvider.getDbInstanceModifyWaiter(rdsClient);
        StackCancellationCheck stackCancellationCheck = new StackCancellationCheck(ac.getCloudContext().getId());
        LOGGER.debug("Starting waiting on RDS upgrade");
        run(rdsWaiter, describeDBInstancesRequest, stackCancellationCheck);
        LOGGER.debug("Finished waiting on RDS upgrade");
    }

}