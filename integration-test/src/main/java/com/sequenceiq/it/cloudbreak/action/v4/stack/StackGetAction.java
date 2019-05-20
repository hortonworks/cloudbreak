package com.sequenceiq.it.cloudbreak.action.v4.stack;

import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class StackGetAction implements Action<StackTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackGetAction.class);

    @Override
    public StackTestDto action(TestContext testContext, StackTestDto testDto, CloudbreakClient client) throws Exception {
        Log.log(LOGGER, " Name: " + testDto.getRequest().getName());
        Log.logJSON(LOGGER, " Stack get request:\n", testDto.getRequest());
        testDto.setResponse(
                client.getCloudbreakClient()
                        .stackV4Endpoint()
                        .get(client.getWorkspaceId(), testDto.getName(), new HashSet<>()));
        Log.logJSON(LOGGER, " Stack get was successfully:\n", testDto.getResponse());
        Log.log(LOGGER, " ID: " + testDto.getResponse().getId());

        return testDto;
    }
}
