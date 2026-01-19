package com.sequenceiq.it.cloudbreak.action.v4.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.database.RedbeamsDatabaseTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.RedbeamsClient;

public class RedbeamsDatabaseDeleteAction implements Action<RedbeamsDatabaseTestDto, RedbeamsClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsDatabaseDeleteAction.class);

    @Override
    public RedbeamsDatabaseTestDto action(TestContext testContext, RedbeamsDatabaseTestDto testDto, RedbeamsClient client) throws Exception {
        Log.when(LOGGER, String.format("Database delete request Name: %s", testDto.getRequest().getName()));
        testDto.setResponse(
                client.getDefaultClient(testContext)
                        .databaseV4Endpoint()
                        .deleteByName(testDto.getRequest().getEnvironmentCrn(), testDto.getName()));
        Log.whenJson(LOGGER, " Database deleted successfully:\n", testDto.getResponse());
        return testDto;
    }

}
