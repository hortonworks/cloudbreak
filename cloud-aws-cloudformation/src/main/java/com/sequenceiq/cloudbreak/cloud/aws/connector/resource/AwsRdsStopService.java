package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static com.sequenceiq.cloudbreak.cloud.aws.scheduler.WaiterRunner.run;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.StopDBInstanceRequest;
import com.amazonaws.waiters.Waiter;
import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonRdsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.CustomAmazonWaiterProvider;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.StackCancellationCheck;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;

@Service
public class AwsRdsStopService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsRdsStopService.class);

    private static final String SUCCESS_STATUS = "stopped";

    @Inject
    private AwsCloudFormationClient awsClient;

    @Inject
    private CustomAmazonWaiterProvider customAmazonWaiterProvider;

    public void stop(AuthenticatedContext ac, DatabaseStack dbStack) throws ExecutionException, TimeoutException, InterruptedException {
        AwsCredentialView credentialView = new AwsCredentialView(ac.getCloudCredential());
        String regionName = ac.getCloudContext().getLocation().getRegion().value();
        AmazonRdsClient rdsClient = awsClient.createRdsClient(credentialView, regionName);

        String dbInstanceIdentifier = dbStack.getDatabaseServer().getServerId();

        StopDBInstanceRequest stopDBInstanceRequest = new StopDBInstanceRequest();
        stopDBInstanceRequest.setDBInstanceIdentifier(dbInstanceIdentifier);

        LOGGER.debug("RDS stop request");
        try {
            rdsClient.stopDBInstance(stopDBInstanceRequest);
        } catch (RuntimeException ex) {
            throw new CloudConnectorException(ex.getMessage(), ex);
        }

        Waiter<DescribeDBInstancesRequest> rdsWaiter = customAmazonWaiterProvider
                .getDbInstanceStopWaiter(rdsClient);
        DescribeDBInstancesRequest describeDBInstancesRequest = new DescribeDBInstancesRequest().withDBInstanceIdentifier(dbInstanceIdentifier);
        StackCancellationCheck stackCancellationCheck = new StackCancellationCheck(ac.getCloudContext().getId());
        run(rdsWaiter, describeDBInstancesRequest, stackCancellationCheck, "RDS stop failed");
        LOGGER.debug("RDS stop process finished. DB Instance ID: {}", dbInstanceIdentifier);
    }
}
