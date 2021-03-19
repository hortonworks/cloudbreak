package com.sequenceiq.it.cloudbreak.action.v4.environment;

import java.util.Set;

import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponses;
import com.sequenceiq.it.cloudbreak.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class EnvironmentDeleteMultipleByCrnsAction extends AbstractEnvironmentAction {

    private final Set<String> crns;

    public EnvironmentDeleteMultipleByCrnsAction(Set<String> crns) {
        this.crns = crns;
    }

    @Override
    protected EnvironmentTestDto environmentAction(TestContext testContext, EnvironmentTestDto testDto, EnvironmentClient client) throws Exception {
        SimpleEnvironmentResponses delete = client.getDefaultClient()
                .environmentV1Endpoint()
                .deleteMultipleByCrns(crns, true, false);
        testDto.setResponseSimpleEnvSet(delete.getResponses());
        Log.whenJson("Environments delete response: ", delete);
        return testDto;
    }
}
