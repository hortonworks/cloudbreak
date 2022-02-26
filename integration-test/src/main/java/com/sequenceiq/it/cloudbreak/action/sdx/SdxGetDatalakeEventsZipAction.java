package com.sequenceiq.it.cloudbreak.action.sdx;

import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.util.SdxEventTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class SdxGetDatalakeEventsZipAction implements Action<SdxEventTestDto, SdxClient> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SdxGetDatalakeEventsZipAction.class);

    @Override
    public SdxEventTestDto action(TestContext testContext, SdxEventTestDto testDto, SdxClient client) throws Exception {
        Log.when(LOGGER, "Getting zipped datalake events via " + client.getDefaultClient().sdxEventEndpoint() +
                ", for input " + testDto.argsToString());
        Response zippedDatalakeEvents = client.getDefaultClient().sdxEventEndpoint().getDatalakeEventsZip(
                testDto.getEnvironmentCrn(), testDto.getTypes()
        );
        Log.when(LOGGER, "Zipped datalake events response: " + zippedDatalakeEvents);
        return testDto;
    }
}
