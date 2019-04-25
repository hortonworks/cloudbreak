package com.sequenceiq.it.cloudbreak.newway.action.v4.blueprint;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;
import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.blueprint.BlueprintTestDto;

public class BlueprintCreateAction implements Action<BlueprintTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintCreateAction.class);

    @Override
    public BlueprintTestDto action(TestContext testContext, BlueprintTestDto testDto, CloudbreakClient client) throws Exception {
        log(LOGGER, format(" Name: %s", testDto.getRequest().getName()));
        logJSON(LOGGER, format(" Blueprint post request:%n"), testDto.getRequest());
        testDto.setResponse(
                client.getCloudbreakClient()
                        .blueprintV4Endpoint()
                        .post(client.getWorkspaceId(), testDto.getRequest()));
        logJSON(LOGGER, format(" Blueprint created  successfully:%n"), testDto.getResponse());
        log(LOGGER, format(" ID: %s", testDto.getResponse().getId()));

        return testDto;
    }

}