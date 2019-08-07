package com.sequenceiq.it.cloudbreak.action.v4.stack;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class StackDeleteAction implements Action<StackTestDto, CloudbreakClient> {

    public static final String INSTANCE_ID = "SDA-instanceId";

    private static final Logger LOGGER = LoggerFactory.getLogger(StackDeleteAction.class);

    @Override
    public StackTestDto action(TestContext testContext, StackTestDto testDto, CloudbreakClient client) throws Exception {
        Log.log(LOGGER, format(" Name: %s", testDto.getRequest().getName()));
        Log.logJSON(LOGGER, " Stack delete request:\n", testDto.getRequest());
        client.getCloudbreakClient()
                .stackV4Endpoint()
                .delete(client.getWorkspaceId(), testDto.getName(), false);
        Log.logJSON(LOGGER, " Stack deletion was successful:\n", testDto.getResponse());
        Log.log(LOGGER, format(" crn: %s", testDto.getResponse().getCrn()));
        return testDto;
    }
}
