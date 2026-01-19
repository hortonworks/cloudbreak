package com.sequenceiq.it.cloudbreak.action.v4.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.database.RedbeamsDatabaseServerTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.RedbeamsClient;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.UpgradeDatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.UpgradeTargetMajorVersion;

public class RedbeamsDatabaseServerUpgradeAction implements Action<RedbeamsDatabaseServerTestDto, RedbeamsClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsDatabaseServerUpgradeAction.class);

    @Override
    public RedbeamsDatabaseServerTestDto action(TestContext testContext, RedbeamsDatabaseServerTestDto testDto, RedbeamsClient client) throws Exception {
        Log.as(LOGGER, " Database server stop request:\n");
        UpgradeDatabaseServerV4Request upgradeDatabaseServerV4Request = new UpgradeDatabaseServerV4Request();
        upgradeDatabaseServerV4Request.setUpgradeTargetMajorVersion(UpgradeTargetMajorVersion.VERSION_11);
        client.getDefaultClient(testContext)
                .databaseServerV4Endpoint()
                .upgrade(testDto.getCrn(), upgradeDatabaseServerV4Request);
        return testDto;
    }

}
