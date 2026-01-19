package com.sequenceiq.it.cloudbreak.action.v4.stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class StackCreateAction implements Action<StackTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackCreateAction.class);

    @Override
    public StackTestDto action(TestContext testContext, StackTestDto testDto, CloudbreakClient client) throws Exception {
        Log.whenJson(LOGGER, " Stack create request: ", testDto.getRequest());
        StackV4Response response = client.getDefaultClient(testContext)
                        .stackV4Endpoint()
                        .post(client.getWorkspaceId(), testDto.getRequest(), testContext.getActingUserCrn().getAccountId());
        testDto.setResponse(response);
        testDto.setFlow("Stack create", response.getFlowIdentifier());
        Log.whenJson(LOGGER, " Stack create response ",  response);
        return testDto;
    }
}
