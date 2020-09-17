package com.sequenceiq.it.cloudbreak.action.v4.environment;

import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.it.cloudbreak.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class EnvironmentDeleteAction implements Action<EnvironmentTestDto, EnvironmentClient> {

    private final boolean cascade;

    private final boolean force;

    public EnvironmentDeleteAction() {
        this(true, false);
    }

    public EnvironmentDeleteAction(boolean cascade, boolean force) {
        this.cascade = cascade;
        this.force = force;
    }

    @Override
    public EnvironmentTestDto action(TestContext testContext, EnvironmentTestDto testDto, EnvironmentClient environmentClient) throws Exception {
        Log.whenJson("Deleting environment with settings: ", this);
        SimpleEnvironmentResponse delete = environmentClient.getEnvironmentClient()
                .environmentV1Endpoint()
                .deleteByCrn(testDto.getResponse().getCrn(), cascade, force);
        Log.whenJson("Environment delete response: ", delete);
        return testDto;
    }
}
