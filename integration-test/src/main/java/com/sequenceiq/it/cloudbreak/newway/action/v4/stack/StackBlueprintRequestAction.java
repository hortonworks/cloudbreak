package com.sequenceiq.it.cloudbreak.newway.action.v4.stack;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;
import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.GeneratedBlueprintV4Response;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.stack.StackTestDto;

public class StackBlueprintRequestAction implements Action<StackTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackBlueprintRequestAction.class);

    @Override
    public StackTestDto action(TestContext testContext, StackTestDto testDto, CloudbreakClient client) throws Exception {
        log(LOGGER, format(" Name: %s", testDto.getRequest().getName()));
        logJSON(LOGGER, " Stack get blueprint:\n", testDto.getRequest());
        GeneratedBlueprintV4Response bp = client.getCloudbreakClient().stackV4Endpoint().postStackForBlueprint(
                client.getWorkspaceId(),
                testDto.getName(),
                testDto.getRequest());
        testDto.withGeneratedBlueprint(bp);
        logJSON(LOGGER, " get blueprint was successfully:\n", testDto.getGeneratedBlueprint());
        return testDto;
    }
}
