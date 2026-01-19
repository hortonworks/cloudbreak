package com.sequenceiq.it.cloudbreak.action.v4.stack;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class StackGetWithResourcesAction implements Action<DistroXTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackGetWithResourcesAction.class);

    @Override
    public DistroXTestDto action(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) throws Exception {
        Log.whenJson(LOGGER, "Stack get request: ", testDto.getRequest());
        StackV4Response response = client.getDefaultClient(testContext)
                .stackV4Endpoint()
                .getWithResources(0L, testDto.getName(), Set.of(), testContext.getActingUserCrn().getAccountId());
        testDto.setResponse(response);
        Log.whenJson(LOGGER, "Stack get response: ",  response);
        return testDto;
    }
}
