package com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonRdsClient;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.AwsRdsUpgradeValidatorProvider;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.RdsInfo;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;

@Service
public class AwsRdsUpgradeValidatorService {

    @Inject
    private AwsRdsUpgradeValidatorProvider awsRdsUpgradeValidatorProvider;

    @Inject
    private AwsRdsUpgradeSteps awsRdsUpgradeSteps;

    @Inject
    private AwsCloudFormationClient awsClient;

    public void validateUpgradeDatabaseServer(AuthenticatedContext authenticatedContext, DatabaseStack databaseStack, TargetMajorVersion targetMajorVersion) {
        awsRdsUpgradeValidatorProvider.validateCustomPropertiesAdded(authenticatedContext, databaseStack);
        validateVersionUpgradeSupported(authenticatedContext, databaseStack, targetMajorVersion);
    }

    private void validateVersionUpgradeSupported(AuthenticatedContext authenticatedContext, DatabaseStack databaseStack, TargetMajorVersion targetMajorVersion) {
        AmazonRdsClient amazonRdsClient = awsClient.createRdsClient(authenticatedContext);
        String dbInstanceIdentifier = databaseStack.getDatabaseServer().getServerId();
        RdsInfo rdsInfo = awsRdsUpgradeSteps.getRdsInfo(amazonRdsClient, dbInstanceIdentifier);
        awsRdsUpgradeValidatorProvider.getHighestUpgradeTargetVersion(amazonRdsClient, targetMajorVersion, rdsInfo.getRdsEngineVersion());
    }
}
