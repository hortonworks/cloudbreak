package com.sequenceiq.it.cloudbreak.action.v4.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.RedbeamsClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.database.RedbeamsDatabaseServerTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class RedbeamsDatabaseServerCreateAction implements Action<RedbeamsDatabaseServerTestDto, RedbeamsClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsDatabaseServerCreateAction.class);

    @Override
    public RedbeamsDatabaseServerTestDto action(TestContext testContext, RedbeamsDatabaseServerTestDto testDto, RedbeamsClient client) throws Exception {
        Log.whenJson(LOGGER, " Database register request:\n", testDto.getRequest());
        testDto.setResponse(
                client.getEndpoints()
                        .databaseServerV4Endpoint()
                        .create(testDto.getRequest()));
        Log.whenJson(LOGGER, " Database registered successfully:\n", testDto.getResponse());
        Log.when(LOGGER, String.format(" CRN: %s", testDto.getResponse().getResourceCrn()));

        return testDto;
    }

}
