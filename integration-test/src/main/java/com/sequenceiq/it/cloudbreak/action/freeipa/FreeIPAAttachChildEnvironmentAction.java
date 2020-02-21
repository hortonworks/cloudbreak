package com.sequenceiq.it.cloudbreak.action.freeipa;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.FreeIPAClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIPAChildEnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class FreeIPAAttachChildEnvironmentAction implements Action<FreeIPAChildEnvironmentTestDto, FreeIPAClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIPAAttachChildEnvironmentAction.class);

    @Override
    public FreeIPAChildEnvironmentTestDto action(TestContext testContext, FreeIPAChildEnvironmentTestDto testDto, FreeIPAClient client) throws Exception {
        Log.whenJson(LOGGER, format(" FreeIPA attach child environment:%n"), testDto.getRequest());
        client.getFreeIpaClient()
                .getFreeIpaV1Endpoint()
                .attachChildEnvironment(testDto.getRequest());
        Log.when(LOGGER, " FreeIPA attached child environment successfully.");
        return testDto;
    }
}
