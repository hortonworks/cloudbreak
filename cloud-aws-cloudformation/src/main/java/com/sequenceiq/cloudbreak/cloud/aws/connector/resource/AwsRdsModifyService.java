package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static com.sequenceiq.cloudbreak.cloud.aws.scheduler.WaiterRunner.run;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonRdsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.CustomAmazonWaiterProvider;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;

import software.amazon.awssdk.core.waiters.Waiter;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesRequest;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesResponse;
import software.amazon.awssdk.services.rds.model.ModifyDbInstanceRequest;

@Service
public class AwsRdsModifyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsRdsModifyService.class);

    @Inject
    private AwsCloudFormationClient awsClient;

    @Inject
    private CustomAmazonWaiterProvider customAmazonWaiterProvider;

    public void disableDeleteProtection(AuthenticatedContext ac, DatabaseStack dbStack) {
        AwsCredentialView credentialView = new AwsCredentialView(ac.getCloudCredential());
        String regionName = ac.getCloudContext().getLocation().getRegion().value();
        AmazonRdsClient rdsClient = awsClient.createRdsClient(credentialView, regionName);

        String dbInstanceIdentifier = dbStack.getDatabaseServer().getServerId();

        ModifyDbInstanceRequest modifyDBInstanceRequest = ModifyDbInstanceRequest.builder()
                .dbInstanceIdentifier(dbInstanceIdentifier)
                .deletionProtection(false)
                .build();

        LOGGER.debug("RDS modify request to disable delete protection for DB: {}", dbInstanceIdentifier);
        try {
            rdsClient.modifyDBInstance(modifyDBInstanceRequest);
        } catch (RuntimeException ex) {
            throw new CloudConnectorException(ex.getMessage(), ex);
        }

        Waiter<DescribeDbInstancesResponse> rdsWaiter = customAmazonWaiterProvider.getDbInstanceModifyWaiter();
        DescribeDbInstancesRequest describeDBInstancesRequest = DescribeDbInstancesRequest.builder().dbInstanceIdentifier(dbInstanceIdentifier).build();
        run(() -> rdsClient.describeDBInstances(describeDBInstancesRequest), rdsWaiter, "Failed to disable deletion protection");
        LOGGER.debug("RDS delete protection is disabled for DB Instance ID: {}", dbInstanceIdentifier);
    }
}

