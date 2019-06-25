package com.sequenceiq.it.cloudbreak.action.v4.blueprint;

import static com.sequenceiq.it.cloudbreak.log.Log.log;
import static com.sequenceiq.it.cloudbreak.log.Log.logJSON;
import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.blueprint.BlueprintTestDto;

public class BlueprintGetAction implements Action<BlueprintTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintGetAction.class);

    @Override
    public BlueprintTestDto action(TestContext testContext, BlueprintTestDto testDto, CloudbreakClient client) throws Exception {
        log(LOGGER, format(" Name: %s", testDto.getName()));
        logJSON(LOGGER, format(" Blueprint getByName response:%n"), testDto.getName());
        testDto.setResponse(
                client.getCloudbreakClient()
                        .blueprintV4Endpoint()
                        .getByName(client.getWorkspaceId(), testDto.getName()));
        logJSON(LOGGER, format(" Blueprint getByName successfully:%n"), testDto.getResponse());

        return testDto;
    }

}