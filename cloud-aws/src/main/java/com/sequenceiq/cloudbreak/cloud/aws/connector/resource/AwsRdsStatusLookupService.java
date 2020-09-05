package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.DBInstanceNotFoundException;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.sequenceiq.cloudbreak.cloud.aws.AwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus;

@Service
public class AwsRdsStatusLookupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsRdsStatusLookupService.class);

    @Inject
    private AwsClient awsClient;

    public ExternalDatabaseStatus getStatus(AuthenticatedContext ac, DatabaseStack dbStack) {
        AwsCredentialView credentialView = new AwsCredentialView(ac.getCloudCredential());
        String regionName = ac.getCloudContext().getLocation().getRegion().value();
        AmazonRDS rdsClient = awsClient.createRdsClient(credentialView, regionName);

        String dbInstanceIdentifier = dbStack.getDatabaseServer().getServerId();

        DescribeDBInstancesRequest describeDBInstancesRequest = new DescribeDBInstancesRequest().withDBInstanceIdentifier(dbInstanceIdentifier);
        DescribeDBInstancesResult describeDBInstancesResult;
        try {
            describeDBInstancesResult = rdsClient.describeDBInstances(describeDBInstancesRequest);
        } catch (DBInstanceNotFoundException ex) {
            return ExternalDatabaseStatus.DELETED;
        } catch (RuntimeException ex) {
            throw new CloudConnectorException(ex.getMessage(), ex);
        }

        return describeDBInstancesResult.getDBInstances()
                .stream()
                .map(i -> getExternalDatabaseStatus(i.getDBInstanceStatus()))
                .findFirst()
                .get();
    }

    public boolean isDeleteProtectonEnabled(AuthenticatedContext ac, DatabaseStack dbStack) {
        AwsCredentialView credentialView = new AwsCredentialView(ac.getCloudCredential());
        String regionName = ac.getCloudContext().getLocation().getRegion().value();
        AmazonRDS rdsClient = awsClient.createRdsClient(credentialView, regionName);

        String dbInstanceIdentifier = dbStack.getDatabaseServer().getServerId();

        DescribeDBInstancesRequest describeDBInstancesRequest = new DescribeDBInstancesRequest().withDBInstanceIdentifier(dbInstanceIdentifier);
        DescribeDBInstancesResult describeDBInstancesResult;
        try {
            LOGGER.debug("RDS Checking if delete protection is enabled");
            describeDBInstancesResult = rdsClient.describeDBInstances(describeDBInstancesRequest);
        } catch (RuntimeException ex) {
            throw new CloudConnectorException(ex.getMessage(), ex);
        }

        return describeDBInstancesResult.getDBInstances()
                .stream()
                .findFirst()
                .get().getDeletionProtection();
    }

    private ExternalDatabaseStatus getExternalDatabaseStatus(String dbInstanceStatus) {
        switch (dbInstanceStatus.toLowerCase()) {
            case "starting": return ExternalDatabaseStatus.START_IN_PROGRESS;
            case "available": return ExternalDatabaseStatus.STARTED;
            case "stopping": return ExternalDatabaseStatus.STOP_IN_PROGRESS;
            case "stopped": return ExternalDatabaseStatus.STOPPED;
            case "deleting": return ExternalDatabaseStatus.DELETE_IN_PROGRESS;
            default: return ExternalDatabaseStatus.UPDATE_IN_PROGRESS;
        }
    }
}
