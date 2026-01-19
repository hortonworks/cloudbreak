package com.sequenceiq.it.cloudbreak.action.v4.stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class StackRequestAction implements Action<StackTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackRequestAction.class);

    @Override
    public StackTestDto action(TestContext testContext, StackTestDto testDto, CloudbreakClient client) throws Exception {
        Log.when(LOGGER, " Stack get cli skeleton:" + testDto.getName());
        StackV4Request request = client.getDefaultClient(testContext).stackV4Endpoint().getRequestfromName(
                client.getWorkspaceId(),
                testDto.getName(),
                testContext.getActingUserCrn().getAccountId());
        testDto.setRequest(request);
        Log.whenJson(LOGGER, " get cli skeleton was successfully:\n", testDto.getRequest());
        return testDto;
    }
}
