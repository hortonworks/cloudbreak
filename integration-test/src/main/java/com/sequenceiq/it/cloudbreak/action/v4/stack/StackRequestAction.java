package com.sequenceiq.it.cloudbreak.action.v4.stack;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class StackRequestAction implements Action<StackTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackRequestAction.class);

    @Override
    public StackTestDto action(TestContext testContext, StackTestDto testDto, CloudbreakClient client) throws Exception {
        Log.log(LOGGER, format(" Name: %s", testDto.getRequest().getName()));
        Log.logJSON(LOGGER, " Stack get cli skeleton:\n", testDto.getRequest());
        StackV4Request request = client.getCloudbreakClient().stackV4Endpoint().getRequestfromName(
                client.getWorkspaceId(),
                testDto.getName());
        testDto.setRequest(request);
        Log.logJSON(LOGGER, " get cli skeleton was successfully:\n", testDto.getRequest());
        Log.log(LOGGER, format(" ID: %s", testDto.getResponse().getId()));
        return testDto;
    }
}
