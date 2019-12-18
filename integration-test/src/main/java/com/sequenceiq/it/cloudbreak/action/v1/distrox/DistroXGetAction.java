package com.sequenceiq.it.cloudbreak.action.v1.distrox;

import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class DistroXGetAction implements Action<DistroXTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXGetAction.class);

    @Override
    public DistroXTestDto action(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) throws Exception {
        testDto.setResponse(
                client.getCloudbreakClient()
                        .distroXV1Endpoint()
                        .getByName(testDto.getName(), new HashSet<>()));
        Log.whenJson(LOGGER, " Stack get was successful:\n", testDto.getResponse());
        Log.when(LOGGER, " crn: " + testDto.getResponse().getCrn());

        return testDto;
    }
}
