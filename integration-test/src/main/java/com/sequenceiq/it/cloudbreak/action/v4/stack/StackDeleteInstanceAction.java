package com.sequenceiq.it.cloudbreak.action.v4.stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class StackDeleteInstanceAction implements Action<StackTestDto, CloudbreakClient> {

    public static final String INSTANCE_ID = "SDA-instanceId";

    private static final Logger LOGGER = LoggerFactory.getLogger(StackDeleteInstanceAction.class);

    @Override
    public StackTestDto action(TestContext testContext, StackTestDto testDto, CloudbreakClient client) throws Exception {
        String instanceId = testContext.getRequiredSelected(INSTANCE_ID);
        Log.when(LOGGER, " Stack delete instance request: " + testDto.getName() + " instance id: " + instanceId);
        Boolean forced = testContext.getSelected("forced");
        client.getDefaultClient(testContext)
                .stackV4Endpoint()
                .deleteInstance(client.getWorkspaceId(), testDto.getName(), forced != null && forced, instanceId,
                        testContext.getActingUserCrn().getAccountId());
        Log.whenJson(LOGGER, " Stack delete instance was successful:\n", testDto.getResponse());
        return testDto;
    }
}
