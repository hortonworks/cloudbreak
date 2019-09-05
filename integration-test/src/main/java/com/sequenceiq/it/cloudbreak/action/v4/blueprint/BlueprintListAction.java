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

public class BlueprintListAction implements Action<BlueprintTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintListAction.class);

    @Override
    public BlueprintTestDto action(TestContext testContext, BlueprintTestDto testDto, CloudbreakClient client) throws Exception {
        log(LOGGER, format(" Name: %s", testDto.getRequest().getName()));
        logJSON(LOGGER, format(" Blueprint list by workspace request:%n"), testDto.getRequest());
        testDto.setViewResponses(
                client.getCloudbreakClient()
                        .blueprintV4Endpoint()
                        .list(client.getWorkspaceId(), true).getResponses());
        logJSON(LOGGER, format(" Blueprint list has executed successfully:%n"), testDto.getViewResponses());

        return testDto;
    }

}