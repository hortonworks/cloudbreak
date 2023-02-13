package com.sequenceiq.it.cloudbreak.action.v4.blueprint;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.blueprint.BlueprintTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class BlueprintListAction implements Action<BlueprintTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintListAction.class);

    @Override
    public BlueprintTestDto action(TestContext testContext, BlueprintTestDto testDto, CloudbreakClient client) throws Exception {
        Log.whenJson(LOGGER, format(" Blueprint list by workspace request, workspace:%n"), client.getWorkspaceId());
        testDto.setViewResponses(
                client.getDefaultClient()
                        .blueprintV4Endpoint()
                        .list(client.getWorkspaceId(), true).getResponses());
        Log.whenJson(LOGGER, format(" Blueprint list has executed successfully:%n"), testDto.getViewResponses());

        return testDto;
    }

}