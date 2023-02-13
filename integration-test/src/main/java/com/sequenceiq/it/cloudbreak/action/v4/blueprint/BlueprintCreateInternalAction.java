package com.sequenceiq.it.cloudbreak.action.v4.blueprint;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.blueprint.BlueprintTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class BlueprintCreateInternalAction implements Action<BlueprintTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintCreateInternalAction.class);

    @Override
    public BlueprintTestDto action(TestContext testContext, BlueprintTestDto testDto, CloudbreakClient client) throws Exception {
        Log.whenJson(LOGGER, format(" Blueprint post request:%n"), testDto.getRequest());
        testDto.setResponse(
                client.getInternalClient(testContext)
                        .blueprintV4Endpoint()
                        .postInternal(testDto.getAccountId(), client.getWorkspaceId(), testDto.getRequest()));
        Log.whenJson(LOGGER, format(" Blueprint created  successfully:%n"), testDto.getResponse());
        return testDto;
    }

}