package com.sequenceiq.it.cloudbreak.action.v4.environment;

import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.it.cloudbreak.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class EnvironmentDeleteByNameAction extends AbstractEnvironmentAction {

    private final boolean cascading;

    public EnvironmentDeleteByNameAction(boolean cascading) {
        this.cascading = cascading;
    }

    public EnvironmentDeleteByNameAction() {
        this(true);
    }

    @Override
    protected EnvironmentTestDto environmentAction(TestContext testContext, EnvironmentTestDto testDto, EnvironmentClient client) throws Exception {
        SimpleEnvironmentResponse delete = client.getEnvironmentClient()
                .environmentV1Endpoint()
                .deleteByName(testDto.getName(), cascading, false);
        Log.whenJson("Environment delete response: ", delete);
        return testDto;
    }
}
