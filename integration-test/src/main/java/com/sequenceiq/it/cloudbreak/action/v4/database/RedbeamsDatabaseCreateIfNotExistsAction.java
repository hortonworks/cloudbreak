package com.sequenceiq.it.cloudbreak.action.v4.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.database.RedbeamsDatabaseTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.RedbeamsClient;

public class RedbeamsDatabaseCreateIfNotExistsAction implements Action<RedbeamsDatabaseTestDto, RedbeamsClient> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsDatabaseCreateIfNotExistsAction.class);

    @Override
    public RedbeamsDatabaseTestDto action(TestContext testContext, RedbeamsDatabaseTestDto testDto, RedbeamsClient client) throws Exception {
        Log.when(LOGGER, "Register Database with name: " + testDto.getName());
        try {
            testDto.setResponse(
                    client.getDefaultClient(testContext).databaseV4Endpoint().register(testDto.getRequest())
            );
            Log.whenJson(LOGGER, "Database registered successfully: ", testDto.getResponse());
        } catch (Exception e) {
            Log.when(LOGGER, "Cannot register Database, fetch existing one: " + testDto.getName());

            testDto.setResponse(
                    client.getDefaultClient(testContext).databaseV4Endpoint()
                            .getByName(testDto.getRequest().getEnvironmentCrn(), testDto.getRequest().getName()));
            Log.whenJson(LOGGER, "Database fetched successfully: ", testDto.getResponse());
        }
        if (testDto.getResponse() == null) {
            throw new IllegalStateException("Database could not be registered.");
        }
        return testDto;
    }
}
