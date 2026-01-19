package com.sequenceiq.it.cloudbreak.action.v4.stack;

import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class StackGetAction implements Action<StackTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackGetAction.class);

    @Override
    public StackTestDto action(TestContext testContext, StackTestDto testDto, CloudbreakClient client) throws Exception {
        Log.when(LOGGER, "Stack get request: " + testDto.getRequest().getName());
        testDto.setResponse(
                client.getDefaultClient(testContext)
                        .stackV4Endpoint()
                        .get(client.getWorkspaceId(), testDto.getName(), new HashSet<>(), testContext.getActingUserCrn().getAccountId()));
        Log.whenJson(LOGGER, " Stack get was successful:\n", testDto.getResponse());

        return testDto;
    }
}
