package com.sequenceiq.it.cloudbreak.action.v4.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.database.RedbeamsDatabaseServerTestDto;
import com.sequenceiq.it.cloudbreak.microservice.RedbeamsClient;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.UpgradeDatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.UpgradeTargetMajorVersion;

public class RedbeamsDatabaseServerDescribeByClusterCrnAction implements Action<RedbeamsDatabaseServerTestDto, RedbeamsClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsDatabaseServerDescribeByClusterCrnAction.class);

    private final String environmentCrn;

    private final String clusterCrn;

    public RedbeamsDatabaseServerDescribeByClusterCrnAction(String environmentCrn, String clusterCrn) {
        this.environmentCrn = environmentCrn;
        this.clusterCrn = clusterCrn;
    }

    @Override
    public RedbeamsDatabaseServerTestDto action(TestContext testContext, RedbeamsDatabaseServerTestDto testDto, RedbeamsClient client) throws Exception {
        UpgradeDatabaseServerV4Request upgradeDatabaseServerV4Request = new UpgradeDatabaseServerV4Request();
        upgradeDatabaseServerV4Request.setUpgradeTargetMajorVersion(UpgradeTargetMajorVersion.VERSION_11);
        testDto.setResponse(client.getDefaultClient(testContext)
                .databaseServerV4Endpoint()
                .getByClusterCrn(environmentCrn, clusterCrn));
        return testDto;
    }

}
