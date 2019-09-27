package com.sequenceiq.it.cloudbreak.action.v4.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.RedbeamsClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.database.RedbeamsDatabaseTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class RedbeamsDatabaseCreateIfNotExistsAction implements Action<RedbeamsDatabaseTestDto, RedbeamsClient> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsDatabaseCreateIfNotExistsAction.class);

    @Override
    public RedbeamsDatabaseTestDto action(TestContext testContext, RedbeamsDatabaseTestDto testDto, RedbeamsClient client) throws Exception {
        LOGGER.info("Register Database with name: {}", testDto.getRequest().getName());
        try {
            testDto.setResponse(
                    client.getEndpoints().databaseV4Endpoint().register(testDto.getRequest())
            );
            Log.logJSON(LOGGER, "Database registered successfully: ", testDto.getRequest());
        } catch (Exception e) {
            LOGGER.info("Cannot register Database, fetch existing one: {}", testDto.getRequest().getName());
            testDto.setResponse(
                    client.getEndpoints().databaseV4Endpoint()
                            .getByName(client.getEnvironmentCrn(), testDto.getRequest().getName()));
        }
        if (testDto.getResponse() == null) {
            throw new IllegalStateException("Database could not be registered.");
        }
        return testDto;
    }
}
