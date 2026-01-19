package com.sequenceiq.it.cloudbreak.action.v1.distrox;

import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class DistroXForceDeleteAction implements Action<DistroXTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXForceDeleteAction.class);

    @Override
    public DistroXTestDto action(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) throws Exception {
        client.getDefaultClient(testContext)
                .distroXV1Endpoint()
                .deleteByName(testDto.getName(), true);
        testDto.setResponse(
                client.getDefaultClient(testContext)
                        .distroXV1Endpoint()
                        .getByName(testDto.getName(), new HashSet<>()));
        Log.when(LOGGER, " Stack deletion was successful.");
        return testDto;
    }
}
