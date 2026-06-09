package com.sequenceiq.it.cloudbreak.action.sdx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;

public class SdxEncryptionProfileEnableAction implements Action<SdxTestDto, SdxClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxEncryptionProfileEnableAction.class);

    @Override
    public SdxTestDto action(TestContext testContext, SdxTestDto testDto, SdxClient sdxClient) throws Exception {
        String encryptionProfileNameOrCrn = testDto.getRequest().getEncryptionProfileNameOrCrn();
        Log.when(LOGGER, String.format("Enabling encryption profile, dl name: %s encryption profile CRN: %s", testDto.getName(), encryptionProfileNameOrCrn));
        FlowIdentifier flowIdentifier = sdxClient
                .getDefaultClient(testContext)
                .sdxEncryptionProfileEndpoint()
                .enableEncryptionProfileByName(testDto.getName(), encryptionProfileNameOrCrn);
        testDto.setFlow("Enable Encryption Profile", flowIdentifier);
        Log.when(LOGGER, "Encryption Profile was enabled successfully");
        return testDto;
    }

}
