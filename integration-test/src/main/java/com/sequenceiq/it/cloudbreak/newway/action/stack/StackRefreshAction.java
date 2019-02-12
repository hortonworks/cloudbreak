package com.sequenceiq.it.cloudbreak.newway.action.stack;

import java.util.Collections;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.StackEntity;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

public class StackRefreshAction implements Action<StackEntity> {

    @Override
    public StackEntity action(TestContext testContext, StackEntity entity, CloudbreakClient client) throws Exception {
        entity.setResponse(
                client.getCloudbreakClient().stackV4Endpoint().get(client.getWorkspaceId(), entity.getName(), Collections.emptySet())
        );
        return entity;
    }
}
