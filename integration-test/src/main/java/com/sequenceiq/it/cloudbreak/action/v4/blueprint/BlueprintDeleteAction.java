package com.sequenceiq.it.cloudbreak.action.v4.blueprint;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.blueprint.BlueprintTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class BlueprintDeleteAction implements Action<BlueprintTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintDeleteAction.class);

    @Override
    public BlueprintTestDto action(TestContext testContext, BlueprintTestDto testDto, CloudbreakClient client) throws Exception {
        Log.when(LOGGER, format(" Blueprint deleteByName request: %n", testDto.getRequest().getName()));
        testDto.setResponse(
                client.getDefaultClient(testContext)
                        .blueprintV4Endpoint()
                        .deleteByName(client.getWorkspaceId(), testDto.getName()));
        Log.whenJson(LOGGER, format(" Blueprint deleted successfully:%n"), testDto.getResponse());
        Log.when(LOGGER, format(" crn: %s", testDto.getResponse().getCrn()));

        return testDto;
    }

}