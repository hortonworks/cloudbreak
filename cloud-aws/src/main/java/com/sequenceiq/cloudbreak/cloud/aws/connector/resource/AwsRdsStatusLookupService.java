package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.sequenceiq.cloudbreak.cloud.aws.AwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

@Service
public class AwsRdsStatusLookupService {

    @Inject
    private AwsClient awsClient;

    public ExternalDatabaseStatus getStatus(AuthenticatedContext ac, String dbInstanceIdentifier) {
        AwsCredentialView credentialView = new AwsCredentialView(ac.getCloudCredential());
        String regionName = ac.getCloudContext().getLocation().getRegion().value();
        AmazonRDS rdsClient = awsClient.createRdsClient(credentialView, regionName);

        DescribeDBInstancesRequest describeDBInstancesRequest = new DescribeDBInstancesRequest().withDBInstanceIdentifier(dbInstanceIdentifier);
        DescribeDBInstancesResult describeDBInstancesResult;
        try {
            describeDBInstancesResult = rdsClient.describeDBInstances(describeDBInstancesRequest);
        } catch (RuntimeException ex) {
            throw new CloudConnectorException(ex.getMessage(), ex);
        }

        return describeDBInstancesResult.getDBInstances()
                .stream()
                .map(i -> getExternalDatabaseStatus(i.getDBInstanceStatus()))
                .findFirst()
                .get();
    }

    private ExternalDatabaseStatus getExternalDatabaseStatus(String dbInstanceStatus) {
        switch (dbInstanceStatus.toLowerCase()) {
            case "starting": return ExternalDatabaseStatus.START_IN_PROGRESS;
            case "available": return ExternalDatabaseStatus.STARTED;
            case "stopping": return ExternalDatabaseStatus.STOP_IN_PROGRESS;
            case "stopped": return ExternalDatabaseStatus.STOPPED;
            default: return ExternalDatabaseStatus.UPDATE_IN_PROGRESS;
        }
    }
}
