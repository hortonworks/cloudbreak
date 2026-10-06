package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.database.DatabaseServerStorageMetrics;

import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.rds.model.DBInstance;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesResponse;

/**
 * Reads the free storage of an RDS instance from CloudWatch (metric {@code FreeStorageSpace} in namespace {@code AWS/RDS}) and expresses it as a
 * percentage of the instance's allocated storage. Used by the redbeams provider sync poller to warn before the database runs out of space.
 */
@Service
public class AwsRdsStorageMetricService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsRdsStorageMetricService.class);

    private static final String NAMESPACE = "AWS/RDS";

    private static final String METRIC_NAME = "FreeStorageSpace";

    private static final String DB_INSTANCE_IDENTIFIER_DIMENSION = "DBInstanceIdentifier";

    private static final int PERIOD_SECONDS = 300;

    private static final int LOOKBACK_MINUTES = 60;

    private static final long BYTES_IN_GIB = 1024L * 1024L * 1024L;

    private static final double HUNDRED_PERCENT = 100.0d;

    @Inject
    private AwsRdsStatusLookupService awsRdsStatusLookupService;

    @Inject
    private AwsCloudWatchService awsCloudWatchService;

    public Optional<DatabaseServerStorageMetrics> getStorageMetrics(AuthenticatedContext ac, DatabaseStack dbStack) {
        String dbInstanceIdentifier = dbStack.getDatabaseServer().getServerId();
        try {
            Optional<Long> allocatedBytes = getAllocatedBytes(ac, dbStack);
            if (allocatedBytes.isEmpty() || allocatedBytes.get() <= 0) {
                LOGGER.debug("Cannot determine allocated storage for RDS instance {}, skipping storage metric collection.", dbInstanceIdentifier);
                return Optional.empty();
            }
            Optional<Long> freeBytes = getFreeStorageBytes(ac, dbInstanceIdentifier);
            if (freeBytes.isEmpty()) {
                LOGGER.debug("CloudWatch returned no FreeStorageSpace datapoint for RDS instance {}, skipping storage metric collection.", dbInstanceIdentifier);
                return Optional.empty();
            }
            double freePercentage = (double) freeBytes.get() / allocatedBytes.get() * HUNDRED_PERCENT;
            LOGGER.debug("RDS instance {} free storage: {} bytes of {} allocated ({}% free).",
                    dbInstanceIdentifier, freeBytes.get(), allocatedBytes.get(), freePercentage);
            return Optional.of(new DatabaseServerStorageMetrics(freePercentage, freeBytes.get(), allocatedBytes.get()));
        } catch (Exception e) {
            LOGGER.warn("Failed to read storage metrics for RDS instance {}: {}", dbInstanceIdentifier, e.getMessage(), e);
            return Optional.empty();
        }
    }

    private Optional<Long> getAllocatedBytes(AuthenticatedContext ac, DatabaseStack dbStack) {
        DescribeDbInstancesResponse response = awsRdsStatusLookupService.getDescribeDBInstancesResult(ac, dbStack);
        if (response == null) {
            return Optional.empty();
        }
        return response.dbInstances().stream()
                .findFirst()
                .map(DBInstance::allocatedStorage)
                .filter(allocatedGib -> allocatedGib != null && allocatedGib > 0)
                .map(allocatedGib -> allocatedGib.longValue() * BYTES_IN_GIB);
    }

    private Optional<Long> getFreeStorageBytes(AuthenticatedContext ac, String dbInstanceIdentifier) {
        AwsCredentialView credentialView = new AwsCredentialView(ac.getCloudCredential());
        String regionName = ac.getCloudContext().getLocation().getRegion().value();
        Dimension dimension = Dimension.builder().name(DB_INSTANCE_IDENTIFIER_DIMENSION).value(dbInstanceIdentifier).build();
        return awsCloudWatchService.getLatestAverageMetricValue(credentialView, regionName, NAMESPACE, METRIC_NAME, dimension, LOOKBACK_MINUTES, PERIOD_SECONDS)
                .map(Double::longValue);
    }
}
