package com.sequenceiq.it.cloudbreak.newway.action.v4.stack;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;
import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UserNamePasswordV4Request;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.stack.StackTestDto;

public class StackModifyAmbariPasswordAction implements Action<StackTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackModifyAmbariPasswordAction.class);

    @Override
    public StackTestDto action(TestContext testContext, StackTestDto entity, CloudbreakClient client) throws Exception {
        log(LOGGER, format(" Name: %s", entity.getRequest().getName()));
        logJSON(LOGGER, " Stack put ambari password request:\n", entity.getRequest());
        UserNamePasswordV4Request userNamePasswordV4Request = new UserNamePasswordV4Request();
        userNamePasswordV4Request.setOldPassword(entity.getRequest().getCluster().getPassword());
        userNamePasswordV4Request.setUserName(entity.getRequest().getCluster().getUserName());
        userNamePasswordV4Request.setPassword("testnewambaripassword");
        client.getCloudbreakClient().stackV4Endpoint().putPassword(client.getWorkspaceId(),
                entity.getName(), userNamePasswordV4Request);
        logJSON(LOGGER, " Stack was modified ambari password successfully:\n", entity.getResponse());
        log(LOGGER, format(" ID: %s", entity.getResponse().getId()));
        return entity;
    }
}
