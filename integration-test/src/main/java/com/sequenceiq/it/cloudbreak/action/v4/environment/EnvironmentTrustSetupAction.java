package com.sequenceiq.it.cloudbreak.action.v4.environment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.environment.api.v1.environment.model.response.SetupCrossRealmTrustResponse;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTrustSetupDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;

public class EnvironmentTrustSetupAction implements Action<EnvironmentTrustSetupDto, EnvironmentClient> {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentTrustSetupAction.class);

    @Override
    public EnvironmentTrustSetupDto action(TestContext testContext, EnvironmentTrustSetupDto testDto, EnvironmentClient client) throws Exception {
        SetupCrossRealmTrustResponse response = client.getDefaultClient(testContext).hybridEndpoint().setupByCrn(
                testContext.get(EnvironmentTestDto.class).getResourceCrn(), testDto.getRequest());
        testDto.setResponse(testDto.getRequest());
        testDto.setLastKnownFlowId(response.getFlowIdentifier().getPollableId());
        Log.when(LOGGER, "Environment trust setup  action posted");
        return testDto;
    }
}
