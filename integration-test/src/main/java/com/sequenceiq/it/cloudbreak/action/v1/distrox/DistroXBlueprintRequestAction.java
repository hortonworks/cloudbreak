package com.sequenceiq.it.cloudbreak.action.v1.distrox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.GeneratedBlueprintV4Response;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class DistroXBlueprintRequestAction implements Action<DistroXTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXBlueprintRequestAction.class);

    @Override
    public DistroXTestDto action(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) throws Exception {
        Log.when(LOGGER, " Stack get generated blueprint.");
        GeneratedBlueprintV4Response bp = client.getDefaultClient(testContext).distroXV1Endpoint().postStackForBlueprint(testDto.getRequest());
        testDto.withGeneratedBlueprint(bp);
        Log.whenJson(LOGGER, " get generated blueprint was successful:\n", testDto.getGeneratedBlueprint());
        return testDto;
    }
}
