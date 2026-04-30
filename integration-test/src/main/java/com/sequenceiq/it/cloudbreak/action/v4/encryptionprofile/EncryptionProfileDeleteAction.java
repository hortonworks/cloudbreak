package com.sequenceiq.it.cloudbreak.action.v4.encryptionprofile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EncryptionProfileTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;

public class EncryptionProfileDeleteAction implements Action<EncryptionProfileTestDto, EnvironmentClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EncryptionProfileDeleteAction.class);

    @Override
    public EncryptionProfileTestDto action(TestContext testContext, EncryptionProfileTestDto testDto, EnvironmentClient environmentClient) throws Exception {
        Log.when(LOGGER, "Encryption Profile delete request, name:" + testDto.getName());
        testDto.setResponse(
                environmentClient.getDefaultClient(testContext)
                        .encryptionProfileEndpoint()
                        .deleteByName(testDto.getName()));
        Log.whenJson(LOGGER, " Encryption Profile was deleted successfully:\n", testDto.getResponse());
        return testDto;
    }
}
