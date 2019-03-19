package com.sequenceiq.it.cloudbreak.newway.action.v4.stack;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.stack.StackTestDto;

public class StackDeleteInstanceAction implements Action<StackTestDto> {

    public static final String INSTANCE_ID = "SDA-instanceId";

    private static final Logger LOGGER = LoggerFactory.getLogger(StackDeleteInstanceAction.class);

    @Override
    public StackTestDto action(TestContext testContext, StackTestDto testDto, CloudbreakClient client) throws Exception {
        logJSON(LOGGER, " Stack delete instance request:\n", testDto.getRequest());
        String instanceId = testContext.getRequiredSelected(INSTANCE_ID);
        Boolean forced = testContext.getSelected("forced");
        client.getCloudbreakClient()
                .stackV4Endpoint()
                .deleteInstance(client.getWorkspaceId(), testDto.getName(), forced != null && forced, instanceId);
        logJSON(LOGGER, " Stack delete instance was successful:\n", testDto.getResponse());
        return testDto;
    }
}
