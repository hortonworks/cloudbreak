package com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonRdsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.AwsRdsUpgradeOperations;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.AwsRdsVersionOperations;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.common.database.MajorVersion;

/*
TODO:
    - if the DB server was already upgraded, then the upgrade command should not be fired again.
    - but then should check state as well. If it is upgrading, then it should wait. But if it is not upgrading, then no waiting should be done.


 */
@Service
public class AwsRdsUpgradeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsRdsUpgradeOperations.class);

    @Inject
    private AwsCloudFormationClient awsClient;

    @Inject
    private AwsRdsUpgradeOperations awsRdsUpgradeOperations;

    @Inject
    private AwsRdsVersionOperations awsRdsVersionOperations;

    public List<CloudResourceStatus> upgrade(AuthenticatedContext ac, DatabaseStack dbStack, MajorVersion targetMajorVersion) {
        AwsCredentialView credentialView = new AwsCredentialView(ac.getCloudCredential());
        String regionName = ac.getCloudContext().getLocation().getRegion().value();
        AmazonRdsClient rdsClient = awsClient.createRdsClient(credentialView, regionName);

        return upgradeRds(ac, targetMajorVersion, rdsClient, dbStack.getDatabaseServer());
    }

    private List<CloudResourceStatus> upgradeRds(AuthenticatedContext ac, MajorVersion targetMajorVersion, AmazonRdsClient rdsClient,
            DatabaseServer databaseServer) {
        String dbInstanceIdentifier = databaseServer.getServerId();
        String currentDbEngineVersion = awsRdsUpgradeOperations.getCurrentDbEngineVersion(rdsClient, dbInstanceIdentifier);
        Set<String> upgradeTargets = awsRdsUpgradeOperations.getUpgradeTargetVersions(rdsClient, currentDbEngineVersion);
        String highestAvailableTargetVersion = awsRdsVersionOperations.getHighestUpgradeVersion(upgradeTargets, targetMajorVersion);

        String dbParameterGroupName = awsRdsUpgradeOperations.createPatameterGroupWithCustomSettings(rdsClient, databaseServer, highestAvailableTargetVersion);
        awsRdsUpgradeOperations.upgradeRds(rdsClient, highestAvailableTargetVersion, dbInstanceIdentifier, dbParameterGroupName);
        awsRdsUpgradeOperations.waitForRdsUpgrade(ac, rdsClient, dbInstanceIdentifier);

        LOGGER.debug("RDS upgrade done for DB: {}", dbInstanceIdentifier);
        return List.of();
    }

}
