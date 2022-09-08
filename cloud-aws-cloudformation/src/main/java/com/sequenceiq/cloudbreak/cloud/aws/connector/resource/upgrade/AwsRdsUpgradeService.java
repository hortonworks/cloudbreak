package com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade;

import static com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.RdsState.AVAILABLE;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonRdsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.AwsRdsUpgradeValidatorService;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.RdsInfo;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.common.database.Version;

@Service
public class AwsRdsUpgradeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsRdsUpgradeService.class);

    @Inject
    private AwsCloudFormationClient awsClient;

    @Inject
    private AwsRdsUpgradeSteps awsRdsUpgradeSteps;

    @Inject
    private AwsRdsUpgradeValidatorService awsRdsUpgradeValidatorService;

    public void upgrade(AuthenticatedContext ac, DatabaseStack dbStack, Version targetMajorVersion) {
        DatabaseServer databaseServer = dbStack.getDatabaseServer();
        String dbInstanceIdentifier = databaseServer.getServerId();
        LOGGER.debug("Starting the upgrade of RDS {} to target major version of {}", dbInstanceIdentifier, targetMajorVersion);

        AmazonRdsClient rdsClient = getAmazonRdsClient(ac);
        RdsInfo rdsInfo = getRdsInfo(dbInstanceIdentifier, rdsClient);
        if (awsRdsUpgradeValidatorService.isRdsMajorVersionSmallerThanTarget(rdsInfo, targetMajorVersion)) {
            awsRdsUpgradeValidatorService.validateRdsIsAvailableOrUpgrading(rdsInfo);
            upgradeRdsIfNotUpgradingAlready(targetMajorVersion, databaseServer, rdsClient, rdsInfo);
            waitForRdsUpgrade(ac, databaseServer, rdsClient);
        }
        LOGGER.debug("RDS upgrade done for DB: {}", dbInstanceIdentifier);
    }

    private RdsInfo getRdsInfo(String dbInstanceIdentifier, AmazonRdsClient rdsClient) {
        return awsRdsUpgradeSteps.getRdsInfo(rdsClient, dbInstanceIdentifier);
    }

    private void upgradeRdsIfNotUpgradingAlready(Version targetMajorVersion, DatabaseServer databaseServer, AmazonRdsClient rdsClient, RdsInfo rdsInfo) {
        if (AVAILABLE == rdsInfo.getRdsState()) {
            LOGGER.debug("RDS {} is in available state, calling upgrade.", databaseServer.getServerId());
            awsRdsUpgradeSteps.upgradeRds(rdsClient, databaseServer, rdsInfo, targetMajorVersion);
        } else {
            LOGGER.debug("RDS {} is already upgrading, proceeding to wait for upgrade", databaseServer.getServerId());
        }
    }

    private void waitForRdsUpgrade(AuthenticatedContext ac, DatabaseServer databaseServer, AmazonRdsClient rdsClient) {
        awsRdsUpgradeSteps.waitForUpgrade(ac, rdsClient, databaseServer);
    }

    private AmazonRdsClient getAmazonRdsClient(AuthenticatedContext ac) {
        AwsCredentialView credentialView = new AwsCredentialView(ac.getCloudCredential());
        String regionName = ac.getCloudContext().getLocation().getRegion().value();
        return awsClient.createRdsClient(credentialView, regionName);
    }

}
