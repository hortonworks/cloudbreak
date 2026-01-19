package com.sequenceiq.it.cloudbreak.action.v4.stack;

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class StackRefreshAction implements Action<StackTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackRefreshAction.class);

    @Override
    public StackTestDto action(TestContext testContext, StackTestDto testDto, CloudbreakClient client) throws Exception {
        testDto.setResponse(
                client.getDefaultClient(testContext).stackV4Endpoint().get(client.getWorkspaceId(), testDto.getName(), Collections.emptySet(),
                        testContext.getActingUserCrn().getAccountId())
        );
        return testDto;
    }
}
