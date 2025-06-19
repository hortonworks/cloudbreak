package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static com.sequenceiq.cloudbreak.cloud.aws.scheduler.CancellableWaiterConfiguration.cancellableWaiterConfiguration;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonRdsClient;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.StackCancellationCheck;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;

import software.amazon.awssdk.services.rds.model.DescribeDbInstancesRequest;
import software.amazon.awssdk.services.rds.model.StartDbInstanceRequest;
import software.amazon.awssdk.services.rds.waiters.RdsWaiter;

@Service
public class AwsRdsStartService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsRdsStartService.class);

    @Inject
    private AwsCloudFormationClient awsClient;

    public void start(AuthenticatedContext ac, DatabaseStack dbStack) throws ExecutionException, TimeoutException, InterruptedException {
        AmazonRdsClient rdsClient = awsClient.createRdsClient(ac);

        String dbInstanceIdentifier = dbStack.getDatabaseServer().getServerId();

        StartDbInstanceRequest startDBInstanceRequest = StartDbInstanceRequest.builder().dbInstanceIdentifier(dbInstanceIdentifier).build();

        try {
            LOGGER.debug("Starting database {}", dbInstanceIdentifier);
            rdsClient.startDBInstance(startDBInstanceRequest);
        } catch (RuntimeException ex) {
            throw new CloudConnectorException(ex.getMessage(), ex);
        }

        StackCancellationCheck cancellationCheck = new StackCancellationCheck(ac.getCloudContext().getId());
        DescribeDbInstancesRequest request = DescribeDbInstancesRequest.builder().dbInstanceIdentifier(dbInstanceIdentifier).build();
        try (RdsWaiter waiter = rdsClient.waiters()) {
            LOGGER.debug("Waiting for database {} to be available", dbInstanceIdentifier);
            waiter.waitUntilDBInstanceAvailable(request, cancellableWaiterConfiguration(cancellationCheck));
        } catch (Exception e) {
            LOGGER.error("Starting database {} failed", dbInstanceIdentifier, e);
            throw new CloudConnectorException(e.getMessage(), e);
        }
    }
}
