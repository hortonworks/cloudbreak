package com.sequenceiq.it.cloudbreak.action.v4.stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class StackStartAction implements Action<StackTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackStartAction.class);

    @Override
    public StackTestDto action(TestContext testContext, StackTestDto testDto, CloudbreakClient client) throws Exception {
        Log.log(LOGGER, String.format(" Name: %s", testDto.getRequest().getName()));
        Log.logJSON(LOGGER, " Stack post request:\n", testDto.getRequest());
        client.getCloudbreakClient().stackV4Endpoint().putStart(client.getWorkspaceId(), testDto.getName());
        Log.logJSON(LOGGER, " Stack was started successfully:\n", testDto.getResponse());
        Log.log(LOGGER, String.format(" CRN: %s", testDto.getResponse().getCrn()));
        return testDto;
    }
}
