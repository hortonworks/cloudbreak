package com.sequenceiq.it.cloudbreak.action.v4.stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class StackDeleteInstanceAction implements Action<StackTestDto> {

    public static final String INSTANCE_ID = "SDA-instanceId";

    private static final Logger LOGGER = LoggerFactory.getLogger(StackDeleteInstanceAction.class);

    @Override
    public StackTestDto action(TestContext testContext, StackTestDto testDto, CloudbreakClient client) throws Exception {
        Log.logJSON(LOGGER, " Stack delete instance request:\n", testDto.getRequest());
        String instanceId = testContext.getRequiredSelected(INSTANCE_ID);
        Boolean forced = testContext.getSelected("forced");
        client.getCloudbreakClient()
                .stackV4Endpoint()
                .deleteInstance(client.getWorkspaceId(), testDto.getName(), forced != null && forced, instanceId);
        Log.logJSON(LOGGER, " Stack delete instance was successful:\n", testDto.getResponse());
        return testDto;
    }
}
