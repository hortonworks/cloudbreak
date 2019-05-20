package com.sequenceiq.it.cloudbreak.action.v4.blueprint;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.blueprint.BlueprintTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class BlueprintRequestAction implements Action<BlueprintTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintRequestAction.class);

    @Override
    public BlueprintTestDto action(TestContext testContext, BlueprintTestDto testDto, CloudbreakClient client) throws Exception {
        Log.log(LOGGER, format(" Name: %s", testDto.getName()));
        Log.logJSON(LOGGER, format(" Blueprint request request:%n"), testDto.getName());
        testDto.setRequest(
                client.getCloudbreakClient()
                        .blueprintV4Endpoint()
                        .getRequest(client.getWorkspaceId(), testDto.getName()));
        Log.logJSON(LOGGER, format(" Blueprint requested successfully:%n"), testDto.getRequest());

        return testDto;
    }

}