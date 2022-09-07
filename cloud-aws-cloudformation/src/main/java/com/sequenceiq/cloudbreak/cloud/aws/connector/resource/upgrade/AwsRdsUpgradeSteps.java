package com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonRdsClient;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.AwsRdsUpgradeOperations;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.AwsRdsUpgradeValidatorService;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.RdsEngineVersion;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.RdsInfo;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.RdsInstanceStatusesToRdsStateConverter;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.RdsState;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.common.database.Version;

@Component
public class AwsRdsUpgradeSteps {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsRdsUpgradeSteps.class);

    @Inject
    private AwsRdsUpgradeOperations awsRdsUpgradeOperations;

    @Inject
    private AwsRdsUpgradeValidatorService awsRdsUpgradeValidatorService;

    @Inject
    private RdsInstanceStatusesToRdsStateConverter rdsInstanceStatusesToRdsStateConverter;

    public RdsInfo getRdsInfo(AmazonRdsClient rdsClient, String dbInstanceIdentifier) {
        LOGGER.debug("Started to retrieve RDS info for upgrade, dbInstanceIdentifier: {}", dbInstanceIdentifier);
        DescribeDBInstancesResult describeDBInstanceResult = awsRdsUpgradeOperations.describeRds(rdsClient, dbInstanceIdentifier);

        Set<String> currentDbEngineVersions = describeDBInstanceResult.getDBInstances().stream()
                .map(DBInstance::getEngineVersion)
                .collect(Collectors.toSet());
        awsRdsUpgradeValidatorService.validateClusterHasASingleVersion(currentDbEngineVersions);

        Map<String, String> dbArnToInstanceStatuses = describeDBInstanceResult.getDBInstances().stream()
                .collect(Collectors.toMap(DBInstance::getDBInstanceArn, DBInstance::getDBInstanceStatus));
        RdsState rdsState = rdsInstanceStatusesToRdsStateConverter.convert(dbArnToInstanceStatuses);
        RdsInfo rdsInfo = new RdsInfo(rdsState, dbArnToInstanceStatuses, new RdsEngineVersion(currentDbEngineVersions.iterator().next()));

        LOGGER.debug("Collected the following info on RDS before starting to upgrade: {}", rdsInfo);
        return rdsInfo;
    }

    public void upgradeRds(AmazonRdsClient rdsClient, DatabaseServer databaseServer, RdsInfo rdsInfo, Version targetMajorVersion) {
        RdsEngineVersion currentRdsVersion = rdsInfo.getRdsEngineVersion();
        RdsEngineVersion upgradeTargetVersion = awsRdsUpgradeOperations.getHighestUpgradeTargetVersion(rdsClient, targetMajorVersion, currentRdsVersion);
        String dbParameterGroupName;
        if (isCustomParameterGroupNeeded(databaseServer)) {
            dbParameterGroupName = awsRdsUpgradeOperations.createParameterGroupWithCustomSettings(rdsClient, databaseServer, upgradeTargetVersion);
        } else {
            dbParameterGroupName = null;
        }
        awsRdsUpgradeOperations.upgradeRds(rdsClient, upgradeTargetVersion, databaseServer.getServerId(), dbParameterGroupName);
    }

    public void waitForUpgrade(AuthenticatedContext ac, AmazonRdsClient rdsClient, DatabaseServer databaseServer) {
        awsRdsUpgradeOperations.waitForRdsUpgrade(ac, rdsClient, databaseServer.getServerId());
    }

    private boolean isCustomParameterGroupNeeded(DatabaseServer databaseServer) {
        return databaseServer.isUseSslEnforcement();
    }
}