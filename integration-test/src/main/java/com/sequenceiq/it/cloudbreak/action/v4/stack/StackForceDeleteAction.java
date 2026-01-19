package com.sequenceiq.it.cloudbreak.action.v4.stack;

import static java.lang.String.format;

import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class StackForceDeleteAction implements Action<StackTestDto, CloudbreakClient> {

    public static final String INSTANCE_ID = "SDA-instanceId";

    private static final Logger LOGGER = LoggerFactory.getLogger(StackForceDeleteAction.class);

    @Override
    public StackTestDto action(TestContext testContext, StackTestDto testDto, CloudbreakClient client) throws Exception {
        Log.when(LOGGER, format("Stack delete request: %s", testDto.getRequest().getName()));
        client.getDefaultClient(testContext)
                .stackV4Endpoint()
                .delete(client.getWorkspaceId(), testDto.getName(), true, testContext.getActingUserCrn().getAccountId());
        testDto.setResponse(
                client.getDefaultClient(testContext)
                        .stackV4Endpoint()
                        .get(client.getWorkspaceId(), testDto.getName(), new HashSet<>(), testContext.getActingUserCrn().getAccountId()));
        Log.whenJson(LOGGER, " Stack deletion was successful: ", testDto.getResponse());
        return testDto;
    }
}
