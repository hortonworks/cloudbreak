package com.sequenceiq.it.cloudbreak.action.v4.database;

import java.util.Collection;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.database.RedbeamsDatabaseTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.RedbeamsClient;
import com.sequenceiq.redbeams.api.endpoint.v4.database.responses.DatabaseV4Response;

public class RedbeamsDatabaseListAction implements Action<RedbeamsDatabaseTestDto, RedbeamsClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsDatabaseListAction.class);

    @Override
    public RedbeamsDatabaseTestDto action(TestContext testContext, RedbeamsDatabaseTestDto testDto, RedbeamsClient client) throws Exception {
        Collection<DatabaseV4Response> responses = client.getDefaultClient(testContext)
                .databaseV4Endpoint()
                .list(testDto.getRequest().getEnvironmentCrn())
                .getResponses();
        testDto.setResponses(responses.stream().collect(Collectors.toSet()));
        Log.whenJson(LOGGER, " Databases listed successfully:\n", testDto.getResponses());
        return testDto;
    }
}
