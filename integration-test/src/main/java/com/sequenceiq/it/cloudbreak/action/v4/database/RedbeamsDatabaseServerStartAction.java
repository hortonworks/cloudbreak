package com.sequenceiq.it.cloudbreak.action.v4.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.database.RedbeamsDatabaseServerTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.RedbeamsClient;

public class RedbeamsDatabaseServerStartAction implements Action<RedbeamsDatabaseServerTestDto, RedbeamsClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsDatabaseServerStartAction.class);

    @Override
    public RedbeamsDatabaseServerTestDto action(TestContext testContext, RedbeamsDatabaseServerTestDto testDto, RedbeamsClient client) throws Exception {
        Log.as(LOGGER, " Database server start request:\n");
        client.getDefaultClient(testContext)
                .databaseServerV4Endpoint()
                .start(testDto.getResponse().getCrn());

        return testDto;
    }

}
