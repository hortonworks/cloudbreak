package com.sequenceiq.it.cloudbreak.action.v1.distrox;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class DistroXStopAction implements Action<DistroXTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXStopAction.class);

    @Override
    public DistroXTestDto action(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) throws Exception {
        Log.log(LOGGER, format(" Name: %s", testDto.getRequest().getName()));
        Log.log(LOGGER, " Stack stop request on: %s", testDto.getName());
        client.getCloudbreakClient()
                .distroXV1Endpoint()
                .putStopByName(testDto.getName());
        Log.log(LOGGER, " Stack stop request was successful on: %s.", testDto.getName());
        return testDto;
    }
}
