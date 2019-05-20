package com.sequenceiq.it.cloudbreak.action.v4.stack;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UserNamePasswordV4Request;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class StackModifyAmbariPasswordAction implements Action<StackTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackModifyAmbariPasswordAction.class);

    @Override
    public StackTestDto action(TestContext testContext, StackTestDto testDto, CloudbreakClient client) throws Exception {
        Log.log(LOGGER, format(" Name: %s", testDto.getRequest().getName()));
        Log.logJSON(LOGGER, " Stack put ambari password request:\n", testDto.getRequest());
        UserNamePasswordV4Request userNamePasswordV4Request = new UserNamePasswordV4Request();
        userNamePasswordV4Request.setOldPassword(testDto.getRequest().getCluster().getPassword());
        userNamePasswordV4Request.setUserName(testDto.getRequest().getCluster().getUserName());
        userNamePasswordV4Request.setPassword("testnewambaripassword");
        client.getCloudbreakClient().stackV4Endpoint().putPassword(client.getWorkspaceId(),
                testDto.getName(), userNamePasswordV4Request);
        Log.logJSON(LOGGER, " Stack was modified ambari password successfully:\n", testDto.getResponse());
        Log.log(LOGGER, format(" ID: %s", testDto.getResponse().getId()));
        return testDto;
    }
}
