package com.sequenceiq.it.cloudbreak.newway.action.v4.stack;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.stack.StackTestDto;

public class StackGetAction implements Action<StackTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackGetAction.class);

    @Override
    public StackTestDto action(TestContext testContext, StackTestDto entity, CloudbreakClient client) throws Exception {
        log(LOGGER, " Name: " + entity.getRequest().getName());
        logJSON(LOGGER, " Stack get request:\n", entity.getRequest());
        entity.setResponse(
                client.getCloudbreakClient()
                        .stackV4Endpoint()
                        .get(client.getWorkspaceId(), entity.getName(), new HashSet<>()));
        logJSON(LOGGER, " Stack get was successfully:\n", entity.getResponse());
        log(LOGGER, " ID: " + entity.getResponse().getId());

        return entity;
    }
}
