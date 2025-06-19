package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.ACCESS_DENIED;
import static com.sequenceiq.cloudbreak.cloud.aws.scheduler.CancellableWaiterConfiguration.cancellableWaiterConfiguration;
import static com.sequenceiq.cloudbreak.cloud.aws.scheduler.WaiterRunner.run;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonRdsClient;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.CustomAmazonWaiterProvider;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.StackCancellationCheck;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;

import software.amazon.awssdk.core.waiters.Waiter;
import software.amazon.awssdk.services.rds.model.DBInstance;
import software.amazon.awssdk.services.rds.model.DescribeDbEngineVersionsRequest;
import software.amazon.awssdk.services.rds.model.DescribeDbEngineVersionsResponse;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesRequest;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesResponse;
import software.amazon.awssdk.services.rds.model.ModifyDbInstanceRequest;
import software.amazon.awssdk.services.rds.model.RdsException;
import software.amazon.awssdk.services.rds.model.RebootDbInstanceRequest;
import software.amazon.awssdk.services.rds.waiters.RdsWaiter;

@Service
public class AwsDatabaseSslCertRotationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsDatabaseSslCertRotationService.class);

    @Inject
    private AwsCloudFormationClient awsClient;

    @Inject
    private CustomAmazonWaiterProvider customAmazonWaiterProvider;

    public void applyCertificateChange(AuthenticatedContext ac, DatabaseStack dbStack, String desiredCertificate) {
            AmazonRdsClient rdsClient = awsClient.createRdsClient(ac);
            String dbInstanceIdentifier = dbStack.getDatabaseServer().getServerId();

            try {
                DescribeDbInstancesResponse describeDbInstances = getDescribeDbInstancesResponse(rdsClient, dbInstanceIdentifier);
                if (describeDbInstances.hasDbInstances()) {
                    if (doWeNeedToModifyTheCertificate(desiredCertificate, describeDbInstances)) {
                        modifyRdsCertificate(desiredCertificate, rdsClient, dbInstanceIdentifier);
                        if (doWeNeedToRestart(rdsClient, describeDbInstances)) {
                            rebootRdsInstance(rdsClient, dbInstanceIdentifier);
                        }
                    }
                }
            } catch (RdsException e) {
                LOGGER.warn("Failed to retrieve DB engine versions.", e);
                if (ACCESS_DENIED.equals((e.awsErrorDetails().errorCode()))) {
                    String message = "Could not query valid upgrade targets because user is not authorized to perform rds:DescribeDBEngineVersions action.";
                    LOGGER.info(message, e);
                } else {
                    String message = getErrorMessage(e.getMessage());
                    throw new CloudConnectorException(message, e);
                }
            } catch (RuntimeException ex) {
                throw new CloudConnectorException(ex.getMessage(), ex);
            }
            waitForRdsInstanceToBeAvailable(ac, rdsClient, dbInstanceIdentifier);
    }

    private void waitForRdsInstanceToBeAvailable(AuthenticatedContext ac, AmazonRdsClient rdsClient, String dbInstanceIdentifier) {
        StackCancellationCheck cancellationCheck = new StackCancellationCheck(ac.getCloudContext().getId());
        DescribeDbInstancesRequest request = DescribeDbInstancesRequest.builder()
                .dbInstanceIdentifier(dbInstanceIdentifier)
                .build();
        try (RdsWaiter waiter = rdsClient.waiters()) {
            LOGGER.debug("Waiting for database {} to be available", dbInstanceIdentifier);
            waiter.waitUntilDBInstanceAvailable(request, cancellableWaiterConfiguration(cancellationCheck));
        } catch (Exception e) {
            LOGGER.error("Rotating cert of database {} failed", dbInstanceIdentifier, e);
            throw new CloudConnectorException(e.getMessage(), e);
        }
    }

    private void waitUntilModifyFinishes(String dbInstanceIdentifier, AmazonRdsClient rdsClient, String exceptionMessage) {
        Waiter<DescribeDbInstancesResponse> rdsWaiter = customAmazonWaiterProvider.getDbInstanceModifyWaiter();
        DescribeDbInstancesRequest describeDBInstancesRequest = DescribeDbInstancesRequest.builder()
                .dbInstanceIdentifier(dbInstanceIdentifier)
                .build();
        run(() -> rdsClient.describeDBInstances(describeDBInstancesRequest), rdsWaiter, exceptionMessage);
    }

    private String getErrorMessage(String exceptionMessage) {
        String message = String.format("Exception occurred when querying valid upgrade targets: %s", exceptionMessage);
        LOGGER.warn(message);
        return message;
    }

    private DescribeDbInstancesResponse getDescribeDbInstancesResponse(AmazonRdsClient rdsClient, String dbInstanceIdentifier) {
        DescribeDbInstancesRequest describeDbInstancesRequest = DescribeDbInstancesRequest.builder()
                .dbInstanceIdentifier(dbInstanceIdentifier)
                .build();
        return rdsClient.describeDBInstances(describeDbInstancesRequest);
    }

    private void rebootRdsInstance(AmazonRdsClient rdsClient, String dbInstanceIdentifier) {
        RebootDbInstanceRequest rebootDbInstanceRequest = RebootDbInstanceRequest.builder()
                .dbInstanceIdentifier(dbInstanceIdentifier)
                .build();
        rdsClient.rebootDBInstance(rebootDbInstanceRequest);
    }

    private boolean doWeNeedToRestart(AmazonRdsClient rdsClient, DescribeDbInstancesResponse describeDbInstancesResponse) {
        DBInstance dbInstance = describeDbInstancesResponse.dbInstances().get(0);
        DescribeDbEngineVersionsRequest describeDbEngineVersionsRequest = DescribeDbEngineVersionsRequest.builder()
                .engineVersion(dbInstance.engineVersion())
                .engine(dbInstance.engine())
                .build();
        DescribeDbEngineVersionsResponse describeDbEngineVersionsResponse = rdsClient.describeDBEngineVersions(describeDbEngineVersionsRequest);
        if (describeDbEngineVersionsResponse.hasDbEngineVersions()) {
            return !describeDbEngineVersionsResponse.dbEngineVersions().get(0).supportsCertificateRotationWithoutRestart();
        }
        return true;
    }

    private void waitUntilCertRotationStarts(String dbInstanceIdentifier, AmazonRdsClient rdsClient, String exceptionMessage) {
        Waiter<DescribeDbInstancesResponse> rdsWaiter = customAmazonWaiterProvider.getCertRotationStartWaiter();
        DescribeDbInstancesRequest describeDBInstancesRequest = DescribeDbInstancesRequest.builder()
                .dbInstanceIdentifier(dbInstanceIdentifier)
                .build();
        run(() -> rdsClient.describeDBInstances(describeDBInstancesRequest), rdsWaiter, exceptionMessage);
    }

    private void modifyRdsCertificate(String desiredCertificate, AmazonRdsClient rdsClient, String dbInstanceIdentifier) {
        ModifyDbInstanceRequest modifyDbInstanceRequest = ModifyDbInstanceRequest.builder()
                .dbInstanceIdentifier(dbInstanceIdentifier)
                .caCertificateIdentifier(desiredCertificate)
                .applyImmediately(true)
                .build();
        rdsClient.modifyDBInstance(modifyDbInstanceRequest);
        waitUntilCertRotationStarts(dbInstanceIdentifier, rdsClient, "Failed to start rotate certificate");
        waitUntilModifyFinishes(dbInstanceIdentifier, rdsClient, "Failed to rotate certificate");
    }

    private boolean doWeNeedToModifyTheCertificate(String desiredCertificate, DescribeDbInstancesResponse describeDbInstancesResponse) {
        return !describeDbInstancesResponse.dbInstances()
                .get(0)
                .caCertificateIdentifier()
                .equalsIgnoreCase(desiredCertificate);
    }

}
