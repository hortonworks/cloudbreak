package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static com.sequenceiq.cloudbreak.cloud.aws.scheduler.WaiterRunner.run;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonRdsClient;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.CustomAmazonWaiterProvider;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;

import software.amazon.awssdk.core.waiters.Waiter;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesRequest;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesResponse;
import software.amazon.awssdk.services.rds.model.StopDbInstanceRequest;

@Service
public class AwsRdsStopService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsRdsStopService.class);

    @Inject
    private AwsCloudFormationClient awsClient;

    @Inject
    private CustomAmazonWaiterProvider customAmazonWaiterProvider;

    public void stop(AuthenticatedContext ac, DatabaseStack dbStack) throws ExecutionException, TimeoutException, InterruptedException {
        AmazonRdsClient rdsClient = awsClient.createRdsClient(ac);

        String dbInstanceIdentifier = dbStack.getDatabaseServer().getServerId();

        StopDbInstanceRequest stopDBInstanceRequest = StopDbInstanceRequest.builder().dbInstanceIdentifier(dbInstanceIdentifier).build();

        LOGGER.debug("RDS stop request");
        try {
            rdsClient.stopDBInstance(stopDBInstanceRequest);
        } catch (RuntimeException ex) {
            throw new CloudConnectorException(ex.getMessage(), ex);
        }
        Waiter<DescribeDbInstancesResponse> rdsWaiter = customAmazonWaiterProvider.getDbInstanceStopWaiter();
        DescribeDbInstancesRequest describeDBInstancesRequest = DescribeDbInstancesRequest.builder().dbInstanceIdentifier(dbInstanceIdentifier).build();
        run(() -> rdsClient.describeDBInstances(describeDBInstancesRequest), rdsWaiter, "RDS stop failed");
        LOGGER.debug("RDS stop process finished. DB Instance ID: {}", dbInstanceIdentifier);
    }
}
