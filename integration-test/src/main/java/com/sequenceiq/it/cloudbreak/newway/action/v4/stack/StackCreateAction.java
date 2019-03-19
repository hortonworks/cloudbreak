package com.sequenceiq.it.cloudbreak.newway.action.v4.stack;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.stack.StackTestDto;

public class StackCreateAction implements Action<StackTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackCreateAction.class);

    @Override
    public StackTestDto action(TestContext testContext, StackTestDto testDto, CloudbreakClient client) throws Exception {
        log(LOGGER, " Name: " + testDto.getRequest().getName());
        logJSON(LOGGER, " Stack post request:\n", testDto.getRequest());
        testDto.setResponse(
                client.getCloudbreakClient()
                        .stackV4Endpoint()
                        .post(client.getWorkspaceId(), testDto.getRequest()));
        logJSON(LOGGER, " Stack created was successfully:\n", testDto.getResponse());
        log(LOGGER, " ID: " + testDto.getResponse().getId());

        return testDto;
    }
}
