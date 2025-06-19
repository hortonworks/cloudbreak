package com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonRdsClient;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;

import software.amazon.awssdk.services.rds.model.DescribeDbInstancesRequest;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesResponse;
import software.amazon.awssdk.services.rds.model.ModifyDbInstanceRequest;

@Service
public class AwsRdsUpgradeOperations {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsRdsUpgradeOperations.class);

    @Inject
    private AwsRdsUpgradeValidatorProvider awsRdsUpgradeValidatorProvider;

    @Inject
    private AwsRdsUpgradeWaitOperations awsRdsUpgradeWaitOperations;

    public DescribeDbInstancesResponse describeRds(AmazonRdsClient rdsClient, String dbInstanceIdentifier) {
        DescribeDbInstancesRequest describeDBInstancesRequest = DescribeDbInstancesRequest.builder().dbInstanceIdentifier(dbInstanceIdentifier).build();
        DescribeDbInstancesResponse result = rdsClient.describeDBInstances(describeDBInstancesRequest);
        LOGGER.debug("Describing RDS with dbInstanceIdentifier {}, result: {}", dbInstanceIdentifier, result);
        return result;
    }

    public void upgradeRds(AmazonRdsClient rdsClient, RdsEngineVersion targetVersion, String dbInstanceIdentifier, String dbParameterGroupName) {
        ModifyDbInstanceRequest.Builder modifyDBInstanceRequestBuilder = ModifyDbInstanceRequest.builder()
                .dbInstanceIdentifier(dbInstanceIdentifier)
                .engineVersion(targetVersion.getVersion())
                .allowMajorVersionUpgrade(true)
                .applyImmediately(true);
        if (StringUtils.isNotEmpty(dbParameterGroupName)) {
            modifyDBInstanceRequestBuilder.dbParameterGroupName(dbParameterGroupName);
        }

        LOGGER.debug("RDS modify request to upgrade engine version to {} for DB {}, request: {}", targetVersion, dbInstanceIdentifier,
                modifyDBInstanceRequestBuilder);
        try {
            rdsClient.modifyDBInstance(modifyDBInstanceRequestBuilder.build());
        } catch (Exception ex) {
            if (ex.getMessage().contains("Cannot modify engine version because another engine version upgrade is already in progress")) {
                LOGGER.info("The upgrade has already been started");
            } else {
                String message = String.format("Error when starting the upgrade of RDS: %s", ex);
                LOGGER.warn(message);
                throw new CloudConnectorException(message, ex);
            }
        }
    }

    public void waitForRdsUpgrade(AmazonRdsClient rdsClient, String dbInstanceIdentifier) {
        LOGGER.debug("Waiting until RDS enters upgrading state, dbInstanceIdentifier: {}", dbInstanceIdentifier);
        DescribeDbInstancesRequest describeDBInstancesRequest = DescribeDbInstancesRequest.builder().dbInstanceIdentifier(dbInstanceIdentifier).build();
        awsRdsUpgradeWaitOperations.waitUntilUpgradeStarts(rdsClient, describeDBInstancesRequest);
        awsRdsUpgradeWaitOperations.waitUntilUpgradeFinishes(rdsClient, describeDBInstancesRequest);
    }
}

