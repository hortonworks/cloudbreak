package com.sequenceiq.it.cloudbreak.action.v1.distrox;

import java.util.Collections;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;

public class DistroXRefreshAction implements Action<DistroXTestDto, CloudbreakClient> {

    @Override
    public DistroXTestDto action(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) throws Exception {
        testDto.setResponse(
                client.getCloudbreakClient().distroXV1Endpoint().get(testDto.getName(), Collections.emptySet())
        );
        return testDto;
    }
}
