package com.sequenceiq.it.cloudbreak.action.sdx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;

public class SdxEncryptionProfileDisableAction implements Action<SdxTestDto, SdxClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxEncryptionProfileDisableAction.class);

    @Override
    public SdxTestDto action(TestContext testContext, SdxTestDto testDto, SdxClient sdxClient) throws Exception {
        Log.when(LOGGER, String.format("Disabling encryption profile, dl name: %s", testDto.getName()));
        FlowIdentifier flowIdentifier = sdxClient
                .getDefaultClient(testContext)
                .sdxEncryptionProfileEndpoint()
                .disableEncryptionProfileByName(testDto.getName());
        testDto.setFlow("Disable Encryption Profile", flowIdentifier);
        Log.when(LOGGER, "Disable Encryption Profile was triggered successfully");
        return testDto;
    }

}
