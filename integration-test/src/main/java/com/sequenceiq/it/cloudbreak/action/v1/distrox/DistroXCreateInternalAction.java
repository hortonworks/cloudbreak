package com.sequenceiq.it.cloudbreak.action.v1.distrox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class DistroXCreateInternalAction implements Action<DistroXTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXCreateInternalAction.class);

    @Override
    public DistroXTestDto action(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) throws Exception {
        Log.whenJson(LOGGER, " Distrox create internal request: ", testDto.getRequest());
        StackV4Response stackV4Response = client.getInternalClient(testContext)
                        .distroXV1Endpoint()
                        .postInternal(testDto.getInitiatorUserCrn(), null, testDto.getRequest());
        testDto.setFlow("Distrox create internal", stackV4Response.getFlowIdentifier());
        testDto.setResponse(stackV4Response);
        Log.whenJson(LOGGER, " Distrox create response: ", stackV4Response);
        return testDto;
    }
}
