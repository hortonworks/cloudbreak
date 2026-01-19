package com.sequenceiq.it.cloudbreak.action.v4.stack;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class StackDeleteAction implements Action<StackTestDto, CloudbreakClient> {

    public static final String INSTANCE_ID = "SDA-instanceId";

    private static final Logger LOGGER = LoggerFactory.getLogger(StackDeleteAction.class);

    @Override
    public StackTestDto action(TestContext testContext, StackTestDto testDto, CloudbreakClient client) throws Exception {
        Log.when(LOGGER, format("Stack delete request: %s", testDto.getRequest().getName()));
        client.getDefaultClient(testContext)
                .stackV4Endpoint()
                .delete(client.getWorkspaceId(), testDto.getName(), false, testContext.getActingUserCrn().getAccountId());
        Log.whenJson(LOGGER, " Stack deletion was successful:\n", testDto.getResponse());
        return testDto;
    }
}
