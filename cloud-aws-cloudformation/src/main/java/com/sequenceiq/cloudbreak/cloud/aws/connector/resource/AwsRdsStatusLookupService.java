package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import java.util.Locale;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonRdsClient;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus;
import com.sequenceiq.cloudbreak.cloud.model.database.CloudDatabaseServerSslCertificate;
import com.sequenceiq.cloudbreak.cloud.model.database.CloudDatabaseServerSslCertificateType;

import software.amazon.awssdk.services.rds.model.DbInstanceNotFoundException;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesRequest;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesResponse;

@Service
public class AwsRdsStatusLookupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsRdsStatusLookupService.class);

    @Inject
    private AwsCloudFormationClient awsClient;

    public ExternalDatabaseStatus getStatus(AuthenticatedContext ac, DatabaseStack dbStack) {
        DescribeDbInstancesResponse describeDBInstancesResponse = getDescribeDBInstancesResponseInternal(ac, dbStack, "RDS Querying ExternalDatabaseStatus",
                "DB Instance does not exist: {}");

        return describeDBInstancesResponse == null ? ExternalDatabaseStatus.DELETED : describeDBInstancesResponse.dbInstances()
                .stream()
                .map(i -> getExternalDatabaseStatus(i.dbInstanceStatus()))
                .findFirst()
                .get();
    }

    private DescribeDbInstancesResponse getDescribeDBInstancesResponseInternal(AuthenticatedContext ac, DatabaseStack dbStack, String preDescribeMessage,
            String notFoundMessage) {
        AmazonRdsClient rdsClient = awsClient.createRdsClient(ac);

        String dbInstanceIdentifier = dbStack.getDatabaseServer().getServerId();

        DescribeDbInstancesRequest describeDBInstancesRequest = DescribeDbInstancesRequest.builder().dbInstanceIdentifier(dbInstanceIdentifier).build();
        DescribeDbInstancesResponse describeDBInstancesResponse;
        try {
            if (preDescribeMessage != null) {
                LOGGER.debug(preDescribeMessage);
            }
            describeDBInstancesResponse = rdsClient.describeDBInstances(describeDBInstancesRequest);
        } catch (DbInstanceNotFoundException ex) {
            LOGGER.debug(notFoundMessage, ex.getMessage());
            describeDBInstancesResponse = null;
        } catch (RuntimeException ex) {
            throw new CloudConnectorException(ex.getMessage(), ex);
        }
        return describeDBInstancesResponse;
    }

    public CloudDatabaseServerSslCertificate getActiveSslRootCertificate(AuthenticatedContext ac, DatabaseStack dbStack) {
        DescribeDbInstancesResponse describeDBInstancesResponse = getDescribeDBInstancesResponseInternal(ac, dbStack, "RDS Fetching active SSL root certificate",
                "DB Instance does not exist: {}");

        return describeDBInstancesResponse == null ? null : describeDBInstancesResponse.dbInstances()
                .stream()
                .map(i -> new CloudDatabaseServerSslCertificate(CloudDatabaseServerSslCertificateType.ROOT,
                        i.caCertificateIdentifier()))
                .findFirst()
                .get();
    }

    public boolean isDeleteProtectionEnabled(DescribeDbInstancesResponse describeDBInstancesResponse) {
        if (isDbStackExistOnProviderSide(describeDBInstancesResponse)) {
            return describeDBInstancesResponse.dbInstances()
                    .stream()
                    .findFirst()
                    .get()
                    .deletionProtection();
        } else {
            return false;
        }
    }

    public boolean isDbStackExistOnProviderSide(DescribeDbInstancesResponse describeDBInstancesResponse) {
        return describeDBInstancesResponse != null;
    }

    public DescribeDbInstancesResponse getDescribeDBInstancesResponseForDeleteProtection(AuthenticatedContext ac, DatabaseStack dbStack) {
        return getDescribeDBInstancesResponseInternal(ac, dbStack, "RDS Checking if delete protection is enabled",
                "DB Instance does not exist! Therefore termination protection check is not relevant anymore: {}");
    }

    public DescribeDbInstancesResponse getDescribeDBInstancesResult(AuthenticatedContext ac, DatabaseStack dbStack) {
        return getDescribeDBInstancesResponseInternal(ac, dbStack, String.format("Get DB instance %s", dbStack.getDatabaseServer().getServerId()),
                "DB Instance does not exist: {}");
    }

    private ExternalDatabaseStatus getExternalDatabaseStatus(String dbInstanceStatus) {
        switch (dbInstanceStatus.toLowerCase(Locale.ROOT)) {
            case "starting":
                return ExternalDatabaseStatus.START_IN_PROGRESS;
            case "available":
                return ExternalDatabaseStatus.STARTED;
            case "stopping":
                return ExternalDatabaseStatus.STOP_IN_PROGRESS;
            case "stopped":
                return ExternalDatabaseStatus.STOPPED;
            case "deleting":
                return ExternalDatabaseStatus.DELETE_IN_PROGRESS;
            default:
                return ExternalDatabaseStatus.UPDATE_IN_PROGRESS;
        }
    }

}
