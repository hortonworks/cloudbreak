package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static com.sequenceiq.cloudbreak.cloud.aws.scheduler.WaiterRunner.run;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonRdsClient;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.CustomAmazonWaiterProvider;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
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

    @Inject
    private AwsRdsParameterGroupService awsRdsParameterGroupService;

    public void disableDeleteProtection(AuthenticatedContext ac, DatabaseStack dbStack) {
        AmazonRdsClient rdsClient = awsClient.createRdsClient(ac);
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

        waitUntilModifyFinishes(dbInstanceIdentifier, rdsClient, "Failed to disable deletion protection");
        LOGGER.debug("RDS delete protection is disabled for DB Instance ID: {}", dbInstanceIdentifier);
    }

    public void updateMasterUserPassword(AuthenticatedContext ac, DatabaseStack databaseStack, String newPassword) {
        AmazonRdsClient rdsClient = awsClient.createRdsClient(ac);
        String dbInstanceIdentifier = databaseStack.getDatabaseServer().getServerId();
        ModifyDbInstanceRequest modifyDBInstanceRequest = ModifyDbInstanceRequest.builder()
                .dbInstanceIdentifier(dbInstanceIdentifier)
                .masterUserPassword(newPassword)
                .build();
        LOGGER.info("Modify master user password for database: {}", dbInstanceIdentifier);
        try {
            rdsClient.modifyDBInstance(modifyDBInstanceRequest);
            // master password change starts asynchronously, so we have to wait for the 'resetting-master-credentials' status first
            waitUntilMasterPasswordResetStarts(dbInstanceIdentifier, rdsClient, "Failed to start master user password change");
            waitUntilModifyFinishes(dbInstanceIdentifier, rdsClient, "Failed to change master user password");
            LOGGER.info("Master user password modified for database: {}", dbInstanceIdentifier);
        } catch (RuntimeException e) {
            LOGGER.warn("Master user password modification failed for database: {}, reason: {}", dbInstanceIdentifier, e.getMessage());
            throw new CloudConnectorException(e.getMessage(), e);
        }
    }

    public void migrateNonSslToSsl(AuthenticatedContext ac, DatabaseServer databaseServer) {
        AmazonRdsClient rdsClient = awsClient.createRdsClient(ac);
        String dbInstanceIdentifier = databaseServer.getServerId();
        try {
            awsRdsParameterGroupService.applySslEnforcement(ac, rdsClient, databaseServer);
            waitUntilModifyFinishes(dbInstanceIdentifier, rdsClient, "Failed to do group assignement");
            LOGGER.info("Ssl enforcement modified for database: {}", dbInstanceIdentifier);
        } catch (RuntimeException e) {
            LOGGER.warn("Ssl enforcement modification failed for database: {}, reason: {}", dbInstanceIdentifier, e.getMessage());
            throw new CloudConnectorException(e.getMessage(), e);
        }
    }

    private void waitUntilMasterPasswordResetStarts(String dbInstanceIdentifier, AmazonRdsClient rdsClient, String exceptionMessage) {
        Waiter<DescribeDbInstancesResponse> rdsWaiter = customAmazonWaiterProvider.getDbMasterPasswordStartWaiter();
        DescribeDbInstancesRequest describeDBInstancesRequest = DescribeDbInstancesRequest.builder().dbInstanceIdentifier(dbInstanceIdentifier).build();
        run(() -> rdsClient.describeDBInstances(describeDBInstancesRequest), rdsWaiter, exceptionMessage);
    }

    private void waitUntilModifyFinishes(String dbInstanceIdentifier, AmazonRdsClient rdsClient, String exceptionMessage) {
        Waiter<DescribeDbInstancesResponse> rdsWaiter = customAmazonWaiterProvider.getDbInstanceModifyWaiter();
        DescribeDbInstancesRequest describeDBInstancesRequest = DescribeDbInstancesRequest.builder().dbInstanceIdentifier(dbInstanceIdentifier).build();
        run(() -> rdsClient.describeDBInstances(describeDBInstancesRequest), rdsWaiter, exceptionMessage);
    }
}

