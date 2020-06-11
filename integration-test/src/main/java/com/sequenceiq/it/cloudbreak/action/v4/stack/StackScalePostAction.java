package com.sequenceiq.it.cloudbreak.action.v4.stack;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackScaleV4Request;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class StackScalePostAction implements Action<StackTestDto, CloudbreakClient> {

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
        Log.whenJson(" StackScale post request:\n", request);
        FlowIdentifier flowIdentifier = client.getCloudbreakClient()
                .stackV4Endpoint()
                .putScaling(client.getWorkspaceId(), testDto.getName(), request);
        testDto.setFlow("Stack scale", flowIdentifier);
        return testDto;
    }
}
