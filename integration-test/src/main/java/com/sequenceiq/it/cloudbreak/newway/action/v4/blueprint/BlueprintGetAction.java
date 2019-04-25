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

public class BlueprintGetAction implements Action<BlueprintTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintGetAction.class);

    @Override
    public BlueprintTestDto action(TestContext testContext, BlueprintTestDto testDto, CloudbreakClient client) throws Exception {
        log(LOGGER, format(" Name: %s", testDto.getName()));
        logJSON(LOGGER, format(" Blueprint get response:%n"), testDto.getName());
        testDto.setResponse(
                client.getCloudbreakClient()
                        .blueprintV4Endpoint()
                        .get(client.getWorkspaceId(), testDto.getName()));
        logJSON(LOGGER, format(" Blueprint get successfully:%n"), testDto.getResponse());

        return testDto;
    }

}