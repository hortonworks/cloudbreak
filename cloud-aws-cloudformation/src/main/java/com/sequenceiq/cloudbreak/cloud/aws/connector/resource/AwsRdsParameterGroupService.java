package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonRdsClient;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.AwsRdsCustomParameterSupplier;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.AwsRdsVersionOperations;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.RdsEngineVersion;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsRdsDbParameterGroupView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.services.rds.model.ModifyDbInstanceRequest;
import software.amazon.awssdk.services.rds.model.Parameter;

@Component
public class AwsRdsParameterGroupService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsRdsParameterGroupService.class);

    @Inject
    private AwsRdsVersionOperations awsRdsVersionOperations;

    @Inject
    private AwsRdsCustomParameterSupplier awsRdsCustomParameterSupplier;

    @Inject
    private PersistenceNotifier persistenceNotifier;

    public String createParameterGroupWithCustomSettings(AuthenticatedContext ac, AmazonRdsClient rdsClient, DatabaseServer databaseServer,
        RdsEngineVersion upgradeTargetVersion) {
        AwsRdsDbParameterGroupView awsRdsDbParameterGroupView = new AwsRdsDbParameterGroupView(databaseServer, awsRdsVersionOperations);
        String dbParameterGroupName = String.format("%s-v%s", awsRdsDbParameterGroupView.getDBParameterGroupName(), upgradeTargetVersion.getMajorVersion());
        String dbParameterGroupFamily = awsRdsVersionOperations.getDBParameterGroupFamily(databaseServer.getEngine(), upgradeTargetVersion.getVersion());
        String serverId = databaseServer.getServerId();
        String dbParameterGroupDescription = String.format("DB parameter group for %s", serverId);

        createParameterGroupIfNeeded(ac, rdsClient, dbParameterGroupName, dbParameterGroupFamily, dbParameterGroupDescription);
        changeParameterInGroup(rdsClient, dbParameterGroupName);
        return dbParameterGroupName;
    }

    public String applySslEnforcement(AuthenticatedContext ac, AmazonRdsClient rdsClient, DatabaseServer databaseServer) {
        String serverId = databaseServer.getServerId();

        AwsRdsDbParameterGroupView awsRdsDbParameterGroupView = new AwsRdsDbParameterGroupView(databaseServer, awsRdsVersionOperations);
        String dbParameterGroupFamily = awsRdsDbParameterGroupView.getDBParameterGroupFamily();
        String dbParameterGroupName = awsRdsDbParameterGroupView.getDBParameterGroupName();
        String dbParameterGroupDescription = String.format("DB parameter group for %s specifically for ssl enforcement", serverId);

        Optional<CloudResource> dbGroup = createParameterGroupIfNeeded(ac, rdsClient, dbParameterGroupName, dbParameterGroupFamily, dbParameterGroupDescription);
        if (dbGroup.isPresent()) {
            persistenceNotifier.notifyAllocation(dbGroup.get(), ac.getCloudContext());
        }
        changeParameterInGroup(rdsClient, dbParameterGroupName);
        assignGroupToRds(rdsClient, serverId, dbParameterGroupName);
        return dbParameterGroupName;
    }

    private CloudResource createParamGroupResource(AuthenticatedContext ac, String dbParameterGroupName) {
        return CloudResource.builder()
                .withType(ResourceType.RDS_DB_PARAMETER_GROUP)
                .withName(dbParameterGroupName)
                .withAvailabilityZone(ac.getCloudContext().getLocation().getAvailabilityZone().value())
                .build();
    }

    private void assignGroupToRds(AmazonRdsClient rdsClient, String dbInstanceIdentifier, String dbParameterGroupName) {
        ModifyDbInstanceRequest.Builder modifyDBInstanceRequestBuilder = ModifyDbInstanceRequest.builder()
                .dbInstanceIdentifier(dbInstanceIdentifier);
        if (StringUtils.isNotEmpty(dbParameterGroupName)) {
            modifyDBInstanceRequestBuilder.dbParameterGroupName(dbParameterGroupName);
            try {
                rdsClient.modifyDBInstance(modifyDBInstanceRequestBuilder.build());
            } catch (Exception ex) {
                String message = String.format("Error when starting the ssl enforcement of RDS: %s", ex);
                LOGGER.warn(message);
                throw new CloudConnectorException(message, ex);
            }
        }
    }

    private void changeParameterInGroup(AmazonRdsClient rdsClient, String dbParameterGroupName) {
        List<Parameter> parametersToChange = awsRdsCustomParameterSupplier.getParametersToChange();
        rdsClient.changeParameterInGroup(dbParameterGroupName, parametersToChange);
        LOGGER.debug("Changed RDS parameters in parameters group. Parameter group name: {}. parameters: {}", dbParameterGroupName, parametersToChange);
    }

    private Optional<CloudResource> createParameterGroupIfNeeded(AuthenticatedContext ac, AmazonRdsClient rdsClient,
        String dbParameterGroupName, String dbParameterGroupFamily, String dbParameterGroupDescription) {
        if (!rdsClient.isDbParameterGroupPresent(dbParameterGroupName)) {
            LOGGER.debug("Creating a custom parameter group for RDS. DbParameterGroupName: {}, family: {}", dbParameterGroupName, dbParameterGroupFamily);
            rdsClient.createParameterGroup(dbParameterGroupFamily, dbParameterGroupName, dbParameterGroupDescription);
            return Optional.ofNullable(createParamGroupResource(ac, dbParameterGroupName));
        } else {
            LOGGER.debug("Custom parameter group with name {} already exists", dbParameterGroupName);
        }
        return Optional.empty();
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
