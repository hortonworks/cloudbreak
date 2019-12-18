package com.sequenceiq.it.cloudbreak.action.v4.stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class StackCreateAction implements Action<StackTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackCreateAction.class);

    @Override
    public StackTestDto action(TestContext testContext, StackTestDto testDto, CloudbreakClient client) throws Exception {
        Log.whenJson(LOGGER, " Stack post request:\n", testDto.getRequest());
        testDto.setResponse(
                client.getCloudbreakClient()
                        .stackV4Endpoint()
                        .post(client.getWorkspaceId(), testDto.getRequest()));
        Log.whenJson(LOGGER, " Stack created was successfully:\n", testDto.getResponse());

        return testDto;
    }
}
