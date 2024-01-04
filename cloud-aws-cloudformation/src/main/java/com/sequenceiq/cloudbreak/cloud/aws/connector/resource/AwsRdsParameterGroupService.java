package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonRdsClient;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.AwsRdsCustomParameterSupplier;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.AwsRdsVersionOperations;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.RdsEngineVersion;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsRdsDbParameterGroupView;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.services.rds.model.Parameter;

@Component
public class AwsRdsParameterGroupService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsRdsParameterGroupService.class);

    @Inject
    private AwsRdsVersionOperations awsRdsVersionOperations;

    @Inject
    private AwsRdsCustomParameterSupplier awsRdsCustomParameterSupplier;

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

    public List<CloudResource> removeFormerParamGroups(AmazonRdsClient rdsClient, DatabaseServer databaseServer, List<CloudResource> cloudResources) {
        List<CloudResource> removedResources = List.of();
        if (databaseServer.isUseSslEnforcement()) {
            removedResources = cloudResources.stream()
                    .filter(cloudResource -> ResourceType.RDS_DB_PARAMETER_GROUP == cloudResource.getType())
                    .collect(Collectors.toList());
            removedResources.forEach(cloudResource -> rdsClient.deleteParameterGroup(cloudResource.getName()));
            LOGGER.debug("The following parameter groups have been deleted: {}", removedResources);
        } else {
            LOGGER.debug("Parameter group deletion is skipped as CB hadn't created one during RDS creation");
        }
        return removedResources;
    }
}
