package com.sequenceiq.it.cloudbreak.newway.action.v4.stack;

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.stack.StackTestDto;

public class StackRefreshAction implements Action<StackTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackRefreshAction.class);

    @Override
    public StackTestDto action(TestContext testContext, StackTestDto entity, CloudbreakClient client) throws Exception {
        entity.setResponse(
                client.getCloudbreakClient().stackV4Endpoint().get(client.getWorkspaceId(), entity.getName(), Collections.emptySet())
        );
        return entity;
    }
}
