package com.sequenceiq.it.cloudbreak.action.v4.environment;

import java.util.Set;

import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponses;
import com.sequenceiq.it.cloudbreak.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class EnvironmentDeleteMultipleByNamesAction extends AbstractEnvironmentAction {

    private final Set<String> envNames;

    public EnvironmentDeleteMultipleByNamesAction(Set<String> envNames) {
        this.envNames = envNames;
    }

    @Override
    protected EnvironmentTestDto environmentAction(TestContext testContext, EnvironmentTestDto testDto, EnvironmentClient client) throws Exception {
        SimpleEnvironmentResponses delete = client.getEnvironmentClient()
                .environmentV1Endpoint()
                .deleteMultipleByNames(envNames, true, false);
        Log.whenJson("Environments delete response: ", delete);
        return testDto;
    }
}
