package com.sequenceiq.it.cloudbreak.action.v1.distrox;

import static java.lang.String.format;

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class DistroXStartAction implements Action<DistroXTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXStartAction.class);

    @Override
    public DistroXTestDto action(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) throws Exception {
        Log.when(LOGGER, format(" Starting Distrox: %s ", testDto.getName()));
        Log.whenJson(LOGGER, " Distrox start request: ", testDto.getRequest());
        FlowIdentifier flowIdentifier = client.getDefaultClient(testContext)
                .distroXV1Endpoint()
                .putStartByName(testDto.getName());
        testDto.setFlow("DistroX start", flowIdentifier);
        StackV4Response stackV4Response = client.getDefaultClient(testContext)
                .distroXV1Endpoint().getByName(testDto.getName(), Collections.emptySet());
        testDto.setResponse(stackV4Response);
        Log.whenJson(LOGGER, " Distrox start response: ", stackV4Response);
        return testDto;
    }
}
