package com.sequenceiq.it.cloudbreak.action.v4.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.database.RedbeamsDatabaseServerTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.RedbeamsClient;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;

public class RedbeamsDatabaseServerDeleteAction implements Action<RedbeamsDatabaseServerTestDto, RedbeamsClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsDatabaseServerDeleteAction.class);

    @Override
    public RedbeamsDatabaseServerTestDto action(TestContext testContext, RedbeamsDatabaseServerTestDto testDto, RedbeamsClient client) throws Exception {
        Log.as(LOGGER, " Database server delete request:\n");
        //can't set response because it's not the same type
        DatabaseServerV4Response response = client.getDefaultClient(testContext)
                .databaseServerV4Endpoint()
                .deleteByCrn(testDto.getResponse().getCrn(), false);
        Log.whenJson(LOGGER, " Database deleted successfully:\n", response);
        Log.when(LOGGER, String.format(" CRN: %s", response.getCrn()));

        return testDto;
    }

}
