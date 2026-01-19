package com.sequenceiq.it.cloudbreak.action.v4.stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class StackStartAction implements Action<StackTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackStartAction.class);

    @Override
    public StackTestDto action(TestContext testContext, StackTestDto testDto, CloudbreakClient client) throws Exception {
        Log.when(LOGGER, String.format(" Stack start request: %s", testDto.getRequest().getName()));
        client.getDefaultClient(testContext).stackV4Endpoint().putStart(client.getWorkspaceId(), testDto.getName(),
                testContext.getActingUserCrn().getAccountId());
        Log.when(LOGGER, " Stack was start requested successfully");
        return testDto;
    }
}
