package com.sequenceiq.it.cloudbreak.newway.action.v4.stack;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;
import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.stack.StackTestDto;

public class StackSyncAction implements Action<StackTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackSyncAction.class);

    @Override
    public StackTestDto action(TestContext testContext, StackTestDto testDto, CloudbreakClient client) throws Exception {
        log(LOGGER, format(" Name: %s", testDto.getRequest().getName()));
        logJSON(LOGGER, " Stack post request:\n", testDto.getRequest());
        client.getCloudbreakClient().stackV4Endpoint().putSync(client.getWorkspaceId(), testDto.getName());
        logJSON(LOGGER, " Stack sync was successful:\n", testDto.getResponse());
        log(LOGGER, format(" ID: %s", testDto.getResponse().getId()));
        return testDto;
    }
}
