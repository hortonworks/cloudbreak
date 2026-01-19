package com.sequenceiq.it.cloudbreak.action.v4.stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.GeneratedBlueprintV4Response;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class StackBlueprintRequestAction implements Action<StackTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackBlueprintRequestAction.class);

    @Override
    public StackTestDto action(TestContext testContext, StackTestDto testDto, CloudbreakClient client) throws Exception {
        Log.whenJson(LOGGER, " Stack get generated blueprint:\n", testDto.getRequest());
        GeneratedBlueprintV4Response bp = client.getDefaultClient(testContext).stackV4Endpoint().postStackForBlueprint(
                client.getWorkspaceId(),
                testDto.getName(),
                testDto.getRequest(),
                testContext.getActingUserCrn().getAccountId());
        testDto.withGeneratedBlueprint(bp);
        Log.whenJson(LOGGER, " get generated blueprint was successfully:\n", testDto.getGeneratedBlueprint());
        return testDto;
    }
}
