package com.sequenceiq.it.cloudbreak.action.sdx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.sdx.api.model.SdxRecoveryRequest;
import com.sequenceiq.sdx.api.model.SdxRecoveryResponse;

public class SdxInternalResizeRecoveryAction implements Action<SdxInternalTestDto, SdxClient>  {
    private static final Logger LOGGER = LoggerFactory.getLogger(SdxInternalResizeRecoveryAction.class);

    @Override
    public SdxInternalTestDto action(TestContext testContext, SdxInternalTestDto testDto, SdxClient client) throws Exception {
        SdxRecoveryRequest recoveryRequest = testDto.getSdxRecoveryRequest();
        Log.when(LOGGER, " Data Lake endpoint: %s" + client.getDefaultClient(testContext).sdxEndpoint() + ", Data Lake's environment: "
                + testDto.getRequest().getEnvironment());
        SdxRecoveryResponse recoveryResponse = client.getDefaultClient(testContext)
                .sdxRecoveryEndpoint()
                .recoverClusterByName(testDto.getName(), recoveryRequest);
        testDto.setFlow("Data Lake resize recovery", recoveryResponse.getFlowIdentifier());
        return testDto;
    }
}
