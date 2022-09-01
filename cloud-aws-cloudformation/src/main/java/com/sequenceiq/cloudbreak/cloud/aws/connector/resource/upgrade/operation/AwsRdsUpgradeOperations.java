package com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.amazonaws.services.rds.model.ModifyDBInstanceRequest;
import com.amazonaws.services.rds.model.Parameter;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonRdsClient;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsRdsDbParameterGroupView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.common.database.Version;

@Service
public class AwsRdsUpgradeOperations {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsRdsUpgradeOperations.class);

    @Inject
    private AwsRdsVersionOperations awsRdsVersionOperations;

    @Inject
    private AwsRdsUpgradeValidatorService awsRdsUpgradeValidatorService;

    @Inject
    private AwsRdsUpgradeWaitOperations awsRdsUpgradeWaitOperations;

    @Inject
    private AwsRdsCustomParameterSupplier awsRdsCustomParameterSupplier;

    public DescribeDBInstancesResult describeRds(AmazonRdsClient rdsClient, String dbInstanceIdentifier) {
        DescribeDBInstancesRequest describeDBInstancesRequest = new DescribeDBInstancesRequest().withDBInstanceIdentifier(dbInstanceIdentifier);
        DescribeDBInstancesResult result = rdsClient.describeDBInstances(describeDBInstancesRequest);
        LOGGER.debug("Describing RDS with dbInstanceIdentifier {}, result: {}", dbInstanceIdentifier, result);
        return result;
    }

    public RdsEngineVersion getHighestUpgradeTargetVersion(AmazonRdsClient rdsClient, Version targetMajorVersion, RdsEngineVersion currentDbVersion) {
        Set<String> validUpgradeTargets = awsRdsVersionOperations.getAllUpgradeTargetVersions(rdsClient, currentDbVersion);
        Optional<RdsEngineVersion> upgradeTargetForMajorVersion = awsRdsVersionOperations.getHighestUpgradeVersionForTargetMajorVersion(validUpgradeTargets,
                targetMajorVersion);
        awsRdsUpgradeValidatorService.validateUpgradePresentForTargetMajorVersion(upgradeTargetForMajorVersion);
        LOGGER.debug("The highest available RDS upgrade target version for major version {} is: {}", targetMajorVersion, upgradeTargetForMajorVersion);
        return upgradeTargetForMajorVersion.get();
    }

    public void upgradeRds(AmazonRdsClient rdsClient, RdsEngineVersion targetVersion, String dbInstanceIdentifier, String dbParameterGroupName) {
        ModifyDBInstanceRequest modifyDBInstanceRequest = new ModifyDBInstanceRequest()
                .withDBInstanceIdentifier(dbInstanceIdentifier)
                .withEngineVersion(targetVersion.getVersion())
                .withAllowMajorVersionUpgrade(true)
                .withApplyImmediately(true)
                .withDBParameterGroupName(dbParameterGroupName);

        LOGGER.debug("RDS modify request to upgrade engine version to {} for DB {}, request: {}", targetVersion, dbInstanceIdentifier, modifyDBInstanceRequest);
        try {
            rdsClient.modifyDBInstance(modifyDBInstanceRequest);
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

    public void waitForRdsUpgrade(AuthenticatedContext ac, AmazonRdsClient rdsClient, String dbInstanceIdentifier) {
        LOGGER.debug("Waiting until RDS enters upgrading state, dbInstanceIdentifier: {}", dbInstanceIdentifier);
        DescribeDBInstancesRequest describeDBInstancesRequest = new DescribeDBInstancesRequest().withDBInstanceIdentifier(dbInstanceIdentifier);
        awsRdsUpgradeWaitOperations.waitUntilUpgradeStarts(rdsClient, describeDBInstancesRequest);
        awsRdsUpgradeWaitOperations.waitUntilUpgradeFinishes(ac, rdsClient, describeDBInstancesRequest);
    }

    public String createParameterGroupWithCustomSettings(AmazonRdsClient rdsClient, DatabaseServer databaseServer, RdsEngineVersion upgradeTargetVersion) {
        AwsRdsDbParameterGroupView awsRdsDbParameterGroupView = new AwsRdsDbParameterGroupView(databaseServer, awsRdsVersionOperations);
        String dbParameterGroupName = String.format("%s-v%s", awsRdsDbParameterGroupView.getDBParameterGroupName(), upgradeTargetVersion.getMajorVersion());
        String dbParameterGroupFamily = awsRdsVersionOperations.getDBParameterGroupFamily(databaseServer.getEngine(), upgradeTargetVersion.getVersion());
        String serverId = databaseServer.getServerId();
        String dbParameterGroupDescription = String.format("DB parameter group for %s", serverId);

        createParameterGroupIfNeeded(rdsClient, dbParameterGroupName, dbParameterGroupFamily, dbParameterGroupDescription);
        changeParameterInGroup(rdsClient, dbParameterGroupName);
        return dbParameterGroupName;
    }

    private void changeParameterInGroup(AmazonRdsClient rdsClient, String dbParameterGroupName) {
        List<Parameter> parametersToChange = awsRdsCustomParameterSupplier.getParametersToChange();
        rdsClient.changeParameterInGroup(dbParameterGroupName, parametersToChange);
        LOGGER.debug("Changed RDS parameters in parameters group. Parameter group name: {}. parameters: {}", dbParameterGroupName, parametersToChange);
    }

    private void createParameterGroupIfNeeded(AmazonRdsClient rdsClient, String dbParameterGroupName, String dbParameterGroupFamily, String
            dbParameterGroupDescription) {
        if (!rdsClient.isDbParameterGroupPresent(dbParameterGroupName)) {
            LOGGER.debug("Creating a custom parameter group for RDS. DbParameterGroupName: {}, family: {}", dbParameterGroupName, dbParameterGroupFamily);
            rdsClient.createParameterGroup(dbParameterGroupFamily, dbParameterGroupName, dbParameterGroupDescription);
        } else {
            LOGGER.debug("Custom parameter group with name {} already exists", dbParameterGroupName);
        }
    }

}