package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.services.rds.model.DBInstanceNotFoundException;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.sequenceiq.cloudbreak.cloud.aws.AwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonRdsClient;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus;
import com.sequenceiq.cloudbreak.cloud.model.database.CloudDatabaseServerSslCertificate;
import com.sequenceiq.cloudbreak.cloud.model.database.CloudDatabaseServerSslCertificateType;

@Service
public class AwsRdsStatusLookupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsRdsStatusLookupService.class);

    @Inject
    private AwsClient awsClient;

    public ExternalDatabaseStatus getStatus(AuthenticatedContext ac, DatabaseStack dbStack) {
        DescribeDBInstancesResult describeDBInstancesResult = getDescribeDBInstancesResultInternal(ac, dbStack, "RDS Querying ExternalDatabaseStatus",
                "DB Instance does not exist: {}");

        return describeDBInstancesResult == null ? ExternalDatabaseStatus.DELETED : describeDBInstancesResult.getDBInstances()
                .stream()
                .map(i -> getExternalDatabaseStatus(i.getDBInstanceStatus()))
                .findFirst()
                .get();
    }

    private DescribeDBInstancesResult getDescribeDBInstancesResultInternal(AuthenticatedContext ac, DatabaseStack dbStack, String preDescribeMessage,
            String notFoundMessage) {
        AwsCredentialView credentialView = new AwsCredentialView(ac.getCloudCredential());
        String regionName = ac.getCloudContext().getLocation().getRegion().value();
        AmazonRdsClient rdsClient = awsClient.createRdsClient(credentialView, regionName);

        String dbInstanceIdentifier = dbStack.getDatabaseServer().getServerId();

        DescribeDBInstancesRequest describeDBInstancesRequest = new DescribeDBInstancesRequest().withDBInstanceIdentifier(dbInstanceIdentifier);
        DescribeDBInstancesResult describeDBInstancesResult;
        try {
            if (preDescribeMessage != null) {
                LOGGER.debug(preDescribeMessage);
            }
            describeDBInstancesResult = rdsClient.describeDBInstances(describeDBInstancesRequest);
        } catch (DBInstanceNotFoundException ex) {
            LOGGER.debug(notFoundMessage, ex.getMessage());
            describeDBInstancesResult = null;
        } catch (RuntimeException ex) {
            throw new CloudConnectorException(ex.getMessage(), ex);
        }
        return describeDBInstancesResult;
    }

    public CloudDatabaseServerSslCertificate getActiveSslRootCertificate(AuthenticatedContext ac, DatabaseStack dbStack) {
        DescribeDBInstancesResult describeDBInstancesResult = getDescribeDBInstancesResultInternal(ac, dbStack, "RDS Fetching active SSL root certificate",
                "DB Instance does not exist: {}");

        return describeDBInstancesResult == null ? null : describeDBInstancesResult.getDBInstances()
                .stream()
                .map(i -> new CloudDatabaseServerSslCertificate(CloudDatabaseServerSslCertificateType.ROOT, i.getCACertificateIdentifier()))
                .findFirst()
                .get();
    }

    public boolean isDeleteProtectionEnabled(DescribeDBInstancesResult describeDBInstancesResult) {
        if (isDbStackExistOnProviderSide(describeDBInstancesResult)) {
            return describeDBInstancesResult.getDBInstances()
                    .stream()
                    .findFirst()
                    .get()
                    .getDeletionProtection();
        } else {
            return false;
        }
    }

    public boolean isDbStackExistOnProviderSide(DescribeDBInstancesResult describeDBInstancesResult) {
        return describeDBInstancesResult != null;
    }

    public DescribeDBInstancesResult getDescribeDBInstancesResultForDeleteProtection(AuthenticatedContext ac, DatabaseStack dbStack) {
        return getDescribeDBInstancesResultInternal(ac, dbStack, "RDS Checking if delete protection is enabled",
                "DB Instance does not exist! Therefore termination protection check is not relevant anymore: {}");
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
