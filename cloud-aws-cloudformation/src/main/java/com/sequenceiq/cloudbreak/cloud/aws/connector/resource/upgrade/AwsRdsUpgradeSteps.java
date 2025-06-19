package com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonRdsClient;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.AwsRdsParameterGroupService;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.AwsRdsUpgradeOperations;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.AwsRdsUpgradeValidatorProvider;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.RdsEngineVersion;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.RdsInfo;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.RdsInstanceStatusesToRdsStateConverter;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.RdsState;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.common.database.Version;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.services.rds.model.DBInstance;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesResponse;

@Component
public class AwsRdsUpgradeSteps {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsRdsUpgradeSteps.class);

    @Inject
    private AwsRdsUpgradeOperations awsRdsUpgradeOperations;

    @Inject
    private AwsRdsUpgradeValidatorProvider awsRdsUpgradeValidatorProvider;

    @Inject
    private RdsInstanceStatusesToRdsStateConverter rdsInstanceStatusesToRdsStateConverter;

    @Inject
    private AwsRdsParameterGroupService awsRdsParameterGroupService;

    public RdsInfo getRdsInfo(AmazonRdsClient rdsClient, String dbInstanceIdentifier) {
        LOGGER.debug("Started to retrieve RDS info for upgrade, dbInstanceIdentifier: {}", dbInstanceIdentifier);
        DescribeDbInstancesResponse describeDBInstanceResponse = awsRdsUpgradeOperations.describeRds(rdsClient, dbInstanceIdentifier);

        Set<String> currentDbEngineVersions = describeDBInstanceResponse.dbInstances().stream()
                .map(DBInstance::engineVersion)
                .collect(Collectors.toSet());
        awsRdsUpgradeValidatorProvider.validateClusterHasASingleVersion(currentDbEngineVersions);

        Map<String, String> dbArnToInstanceStatuses = describeDBInstanceResponse.dbInstances().stream()
                .collect(Collectors.toMap(DBInstance::dbInstanceArn, DBInstance::dbInstanceStatus));
        RdsState rdsState = rdsInstanceStatusesToRdsStateConverter.convert(dbArnToInstanceStatuses);
        RdsInfo rdsInfo = new RdsInfo(rdsState, dbArnToInstanceStatuses, new RdsEngineVersion(currentDbEngineVersions.iterator().next()));

        LOGGER.debug("Collected the following info on RDS before starting to upgrade: {}", rdsInfo);
        return rdsInfo;
    }

    public List<CloudResource> upgradeRds(AuthenticatedContext ac, AmazonRdsClient rdsClient, DatabaseServer databaseServer, RdsInfo rdsInfo,
            Version targetMajorVersion) {
        RdsEngineVersion currentRdsVersion = rdsInfo.getRdsEngineVersion();
        RdsEngineVersion upgradeTargetVersion = awsRdsUpgradeValidatorProvider.getHighestUpgradeTargetVersion(rdsClient, targetMajorVersion, currentRdsVersion);
        String dbParameterGroupName;
        List<CloudResource> cloudResources = new ArrayList<>();
        if (isCustomParameterGroupNeeded(databaseServer)) {
            dbParameterGroupName = awsRdsParameterGroupService.createParameterGroupWithCustomSettings(ac, rdsClient, databaseServer, upgradeTargetVersion);
            cloudResources.add(createParamGroupResource(ac, dbParameterGroupName));
        } else {
            dbParameterGroupName = null;
        }
        awsRdsUpgradeOperations.upgradeRds(rdsClient, upgradeTargetVersion, databaseServer.getServerId(), dbParameterGroupName);
        return cloudResources;
    }

    public void waitForUpgrade(AmazonRdsClient rdsClient, DatabaseServer databaseServer) {
        awsRdsUpgradeOperations.waitForRdsUpgrade(rdsClient, databaseServer.getServerId());
    }

    private boolean isCustomParameterGroupNeeded(DatabaseServer databaseServer) {
        return databaseServer.isUseSslEnforcement();
    }

    private CloudResource createParamGroupResource(AuthenticatedContext ac, String dbParameterGroupName) {
        return CloudResource.builder()
                .withType(ResourceType.RDS_DB_PARAMETER_GROUP)
                .withName(dbParameterGroupName)
                .withAvailabilityZone(ac.getCloudContext().getLocation().getAvailabilityZone().value())
                .build();
    }
}
