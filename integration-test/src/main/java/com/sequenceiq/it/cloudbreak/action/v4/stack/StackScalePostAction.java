package com.sequenceiq.it.cloudbreak.action.v4.stack;

import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class StackScalePostAction implements Action<StackTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackScalePostAction.class);

    private StackScaleV4Request request = new StackScaleV4Request();

    public StackScalePostAction withGroup(String group) {
        request.setGroup(group);
        return this;
    }

    public StackScalePostAction withDesiredCount(Integer count) {
        request.setDesiredCount(count);
        return this;
    }

    public StackScalePostAction withForced(Boolean forced) {
        request.setForced(forced);
        return this;
    }

    public static StackScalePostAction valid() {
        return new StackScalePostAction().withGroup("worker").withDesiredCount(10);
    }

    @Override
    public StackTestDto action(TestContext testContext, StackTestDto testDto, CloudbreakClient client) throws Exception {
        Log.when(LOGGER, String.format("Stack scale request on: %s. Hostgroup: %s, desiredCount: %d", testDto.getName(), request.getGroup(),
                request.getDesiredCount()));
        Log.whenJson(LOGGER, " Stack scale request: ", testDto.getRequest());
        FlowIdentifier flowIdentifier = client.getDefaultClient()
                .stackV4Endpoint()
                .putScaling(client.getWorkspaceId(), testDto.getName(), request,
                        testContext.getActingUserCrn().getAccountId());
        testDto.setFlow("Stack scale", flowIdentifier);
        StackV4Response stackV4Response = client.getDefaultClient()
                .stackV4Endpoint()
                .get(client.getWorkspaceId(), testDto.getName(), new HashSet<>(), testContext.getActingUserCrn().getAccountId());
        testDto.setResponse(stackV4Response);
        Log.whenJson(LOGGER, " Stack scale response: ", stackV4Response);
        LOGGER.info("Hardware info for stack after upscale: {}", stackV4Response.getHardwareInfoGroups());
        return testDto;
    }
}
