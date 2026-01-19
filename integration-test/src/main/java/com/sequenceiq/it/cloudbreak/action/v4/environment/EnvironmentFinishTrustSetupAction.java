package com.sequenceiq.it.cloudbreak.action.v4.environment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.environment.api.v1.environment.model.request.FinishSetupCrossRealmTrustRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.FinishSetupCrossRealmTrustResponse;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;

public class EnvironmentFinishTrustSetupAction extends AbstractEnvironmentAction {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentFinishTrustSetupAction.class);

    @Override
    protected EnvironmentTestDto environmentAction(TestContext testContext, EnvironmentTestDto testDto, EnvironmentClient client) throws Exception {
        FinishSetupCrossRealmTrustResponse response = client.getDefaultClient(testContext).hybridEndpoint()
                .finishSetupByCrn(testDto.getResourceCrn(), new FinishSetupCrossRealmTrustRequest());
        testDto.setLastKnownFlow(response.getFlowIdentifier());
        Log.when(LOGGER, "Environment finish trust setup action posted");
        return testDto;
    }
}
