package com.sequenceiq.it.cloudbreak.action.v4.environment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.environment.api.v1.environment.model.response.AddCrossRealmTrustResponse;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentDirectionalTrustSetupDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;

public class EnvironmentDirectionalTrustSetupAction implements Action<EnvironmentDirectionalTrustSetupDto, EnvironmentClient> {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentDirectionalTrustSetupAction.class);

    @Override
    public EnvironmentDirectionalTrustSetupDto action(TestContext testContext, EnvironmentDirectionalTrustSetupDto testDto, EnvironmentClient client)
            throws Exception {
        AddCrossRealmTrustResponse response = client.getDefaultClient(testContext).crossRealmTrustEndpoint().addTrustByCrn(
                testContext.get(EnvironmentTestDto.class).getResourceCrn(), testDto.getRequest());
        testDto.setResponse(testDto.getRequest());
        testDto.setLastKnownFlowId(response.getFlowIdentifier().getPollableId());
        Log.when(LOGGER, "Environment trust setup  action posted");
        return testDto;
    }
}
