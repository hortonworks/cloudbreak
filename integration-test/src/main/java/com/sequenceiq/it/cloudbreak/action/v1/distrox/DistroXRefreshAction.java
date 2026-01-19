package com.sequenceiq.it.cloudbreak.action.v1.distrox;

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class DistroXRefreshAction implements Action<DistroXTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXRefreshAction.class);

    @Override
    public DistroXTestDto action(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) throws Exception {
        testDto.setResponse(
                client.getDefaultClient(testContext).distroXV1Endpoint().getByName(testDto.getName(), Collections.emptySet())
        );
        Log.whenJson(LOGGER, " Distrox get response: ", testDto.getResponse());
        return testDto;
    }
}
