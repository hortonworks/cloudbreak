package com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade;

import static com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.RdsState.AVAILABLE;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonRdsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.AwsRdsParameterGroupService;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.AwsRdsUpgradeValidatorProvider;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.RdsInfo;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.common.database.Version;

@Service
public class AwsRdsUpgradeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsRdsUpgradeService.class);

    @Inject
    private AwsCloudFormationClient awsClient;

    @Inject
    private AwsRdsUpgradeSteps awsRdsUpgradeSteps;

    @Inject
    private AwsRdsUpgradeValidatorProvider awsRdsUpgradeValidatorProvider;

    @Inject
    private AwsRdsParameterGroupService awsRdsParameterGroupService;

    public void upgrade(AuthenticatedContext ac, DatabaseStack dbStack, Version targetMajorVersion, PersistenceNotifier persistenceNotifier,
            List<CloudResource> cloudResources) {
        DatabaseServer databaseServer = dbStack.getDatabaseServer();
        String dbInstanceIdentifier = databaseServer.getServerId();
        LOGGER.debug("Starting the upgrade of RDS {} to target major version of {}", dbInstanceIdentifier, targetMajorVersion);

        AmazonRdsClient rdsClient = getAmazonRdsClient(ac);
        RdsInfo rdsInfo = getRdsInfo(dbInstanceIdentifier, rdsClient);
        if (awsRdsUpgradeValidatorProvider.isRdsMajorVersionSmallerThanTarget(rdsInfo, targetMajorVersion)) {
            awsRdsUpgradeValidatorProvider.validateRdsIsAvailableOrUpgrading(rdsInfo);
            upgradeRdsIfNotUpgradingAlready(ac, targetMajorVersion, databaseServer, rdsClient, rdsInfo, persistenceNotifier);
            waitForRdsUpgrade(databaseServer, rdsClient);
            List<CloudResource> removedResources = awsRdsParameterGroupService.removeFormerParamGroups(rdsClient, dbStack.getDatabaseServer(), cloudResources);
            persistenceNotifier.notifyDeletions(removedResources, ac.getCloudContext());
        }
        LOGGER.debug("RDS upgrade done for DB: {}", dbInstanceIdentifier);
    }

    private RdsInfo getRdsInfo(String dbInstanceIdentifier, AmazonRdsClient rdsClient) {
        return awsRdsUpgradeSteps.getRdsInfo(rdsClient, dbInstanceIdentifier);
    }

    private void upgradeRdsIfNotUpgradingAlready(AuthenticatedContext ac, Version targetMajorVersion, DatabaseServer databaseServer, AmazonRdsClient rdsClient,
            RdsInfo rdsInfo, PersistenceNotifier persistenceNotifier) {
        if (AVAILABLE == rdsInfo.getRdsState()) {
            LOGGER.debug("RDS {} is in available state, calling upgrade.", databaseServer.getServerId());
            List<CloudResource> newResources = awsRdsUpgradeSteps.upgradeRds(ac, rdsClient, databaseServer, rdsInfo, targetMajorVersion);
            persistenceNotifier.notifyAllocations(newResources, ac.getCloudContext());
        } else {
            LOGGER.debug("RDS {} is already upgrading, proceeding to wait for upgrade", databaseServer.getServerId());
        }
    }

    private void waitForRdsUpgrade(DatabaseServer databaseServer, AmazonRdsClient rdsClient) {
        awsRdsUpgradeSteps.waitForUpgrade(rdsClient, databaseServer);
    }

    private AmazonRdsClient getAmazonRdsClient(AuthenticatedContext ac) {
        AwsCredentialView credentialView = new AwsCredentialView(ac.getCloudCredential());
        String regionName = ac.getCloudContext().getLocation().getRegion().value();
        return awsClient.createRdsClient(credentialView, regionName);
    }
}
