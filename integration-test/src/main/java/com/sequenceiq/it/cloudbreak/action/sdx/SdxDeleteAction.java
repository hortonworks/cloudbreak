package com.sequenceiq.it.cloudbreak.action.sdx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class SdxDeleteAction implements Action<SdxTestDto, SdxClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxDeleteAction.class);

    @Override
    public SdxTestDto action(TestContext testContext, SdxTestDto testDto, SdxClient client) throws Exception {
        Log.when(LOGGER, " SDX endpoint: %s" + client.getSdxClient().sdxEndpoint() + ", SDX's environment: " + testDto.getRequest().getEnvironment());
        FlowIdentifier flowIdentifier = client.getSdxClient()
                .sdxEndpoint()
                .delete(testDto.getName(), false);
        testDto.setFlow("SDX delete", flowIdentifier);
        Log.whenJson(LOGGER, " SDX delete response: ",
                client.getSdxClient().sdxEndpoint().list(testContext.get(EnvironmentTestDto.class).getName()));
        return testDto;
    }
}
